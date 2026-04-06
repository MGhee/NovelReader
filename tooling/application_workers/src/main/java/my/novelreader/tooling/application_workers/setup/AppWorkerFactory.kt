package my.novelreader.tooling.application_workers.setup

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import my.novelreader.coreui.states.NotificationsCenter
import my.novelreader.data.AppRemoteRepository
import my.novelreader.data.SyncRepository
import my.novelreader.data.BookChaptersRepository
import my.novelreader.data.ChapterBodyRepository
import my.novelreader.tooling.application_workers.ChapterDownloadWorker
import my.novelreader.tooling.application_workers.LibraryUpdatesWorker
import my.novelreader.tooling.application_workers.SyncWorker
import my.novelreader.tooling.application_workers.UpdatesCheckerWorker
import my.novelreader.tooling.application_workers.notifications.LibraryUpdateNotification
import javax.inject.Inject

class AppWorkerFactory @Inject internal constructor(
    private val appRemoteRepository: AppRemoteRepository,
    private val notificationsCenter: NotificationsCenter,
    private val syncRepository: SyncRepository,
    private val chapterBodyRepository: ChapterBodyRepository,
    private val bookChaptersRepository: BookChaptersRepository,
) : WorkerFactory() {
    @SuppressLint("LogNotTimber")
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        // Not using Timber as is yet no initialized at this stage
        Log.d("AppWorkerFactory", "AppWorkerFactory: workerClassName=$workerClassName")
        return when (workerClassName) {
            UpdatesCheckerWorker::class.java.name -> UpdatesCheckerWorker(
                context = appContext,
                workerParameters = workerParameters,
                appRemoteRepository = appRemoteRepository,
                notificationsCenter = notificationsCenter
            )
            LibraryUpdatesWorker::class.java.name -> null  // Let Hilt handle injection for HiltWorker
            ChapterDownloadWorker::class.java.name -> ChapterDownloadWorker(
                context = appContext,
                workerParameters = workerParameters,
                chapterBodyRepository = chapterBodyRepository,
                bookChaptersRepository = bookChaptersRepository,
            )
            SyncWorker::class.java.name -> SyncWorker(
                context = appContext,
                workerParameters = workerParameters,
                syncRepository = syncRepository,
            )
            else -> null
        }
    }
}