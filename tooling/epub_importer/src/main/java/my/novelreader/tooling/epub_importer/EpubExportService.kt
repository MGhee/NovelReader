package my.novelreader.tooling.epub_importer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.provider.DocumentsContract
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import my.novelreader.core.Toasty
import my.novelreader.core.tryAsResponse
import my.novelreader.core.utils.Extra_String
import my.novelreader.core.utils.Extra_Uri
import my.novelreader.core.utils.isServiceRunning
import my.novelreader.coreui.states.NotificationsCenter
import my.novelreader.coreui.states.removeProgressBar
import my.novelreader.coreui.states.text
import my.novelreader.coreui.states.title
import my.novelreader.data.EpubExporterRepository
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class EpubExportService : Service() {

    @Inject
    lateinit var notificationsCenter: NotificationsCenter

    @Inject
    lateinit var epubExporterRepository: EpubExporterRepository

    @Inject
    lateinit var toasty: Toasty

    private class IntentData : Intent {
        var uri by Extra_Uri()
        var bookUrl by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, uri: Uri, bookUrl: String) : super(
            ctx,
            EpubExportService::class.java
        ) {
            this.uri = uri
            this.bookUrl = bookUrl
        }
    }

    companion object {
        fun start(ctx: Context, uri: Uri, bookUrl: String) {
            if (!isRunning(ctx))
                ContextCompat.startForegroundService(ctx, IntentData(ctx, uri, bookUrl))
        }

        private fun isRunning(context: Context): Boolean =
            context.isServiceRunning(EpubExportService::class.java)
    }

    private val channelName by lazy { getString(R.string.notification_channel_name_export_epub) }
    private val channelId = "Export EPUB"
    private val notificationId = channelId.hashCode()

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationBuilder = notificationsCenter.showNotification(
            notificationId = notificationId,
            channelId = channelId,
            channelName = channelName,
        )
        startForeground(notificationId, notificationBuilder.build())
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        val intentData = IntentData(intent)
        // Toasts still show when the user has blocked the app's notifications.
        toasty.show(R.string.exporting_epub)
        job = CoroutineScope(Dispatchers.IO).launch {
            notificationsCenter.modifyNotification(
                notificationBuilder,
                notificationId = notificationId
            ) {
                title = getString(R.string.export_epub)
                text = getString(R.string.exporting_epub)
                foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
                setProgress(100, 0, true)
            }

            var lastPercent = -1
            tryAsResponse {
                val outputStream = contentResolver.openOutputStream(intentData.uri, "wt")
                    ?: throw Exception(getString(R.string.failed_get_file))
                outputStream.use {
                    epubExporterRepository.exportEpub(
                        bookUrl = intentData.bookUrl,
                        outputStream = it
                    ) { exported, total ->
                        // Android drops notification updates sent too often, so only update on percent change.
                        val percent = exported * 100 / total
                        if (percent != lastPercent) {
                            lastPercent = percent
                            notificationsCenter.modifyNotification(
                                notificationBuilder,
                                notificationId = notificationId
                            ) {
                                text = getString(R.string.exporting_epub_progress, exported, total)
                                setProgress(total, exported, false)
                            }
                        }
                    }
                }
            }.onSuccess {
                toasty.show(R.string.epub_exported)
                notificationsCenter.showNotification(
                    channelName = channelName,
                    channelId = channelId,
                    notificationId = "Export EPUB success".hashCode()
                ) {
                    title = getString(R.string.export_epub)
                    text = getString(R.string.epub_exported)
                }
            }.onError {
                Timber.e(it.exception)
                toasty.show("${getString(R.string.failed_to_export_epub)}: ${it.message}", shortDuration = false)
                // Don't leave a partially written file behind.
                runCatching { DocumentsContract.deleteDocument(contentResolver, intentData.uri) }
                notificationsCenter.showNotification(
                    channelName = channelName,
                    channelId = channelId,
                    notificationId = "Export EPUB failure".hashCode()
                ) {
                    title = getString(R.string.export_epub)
                    text = getString(R.string.failed_to_export_epub)
                    setSubText(it.message)
                    removeProgressBar()
                }
            }

            stopSelf(startId)
        }
        return START_NOT_STICKY
    }
}
