package my.novelreader.tooling.application_workers.setup

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import my.novelreader.core.AppCoroutineScope
import my.novelreader.core.appPreferences.AppPreferences
import my.novelreader.core.domain.LibraryCategory
import my.novelreader.tooling.application_workers.LibraryUpdatesWorker
import my.novelreader.tooling.application_workers.SyncWorker
import my.novelreader.tooling.application_workers.UpdatesCheckerWorker
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeriodicWorkersInitializer @Inject constructor(
    private val appPreferences: AppPreferences,
    private val workManager: WorkManager,
    private val appCoroutineScope: AppCoroutineScope
) : DefaultLifecycleObserver {

    private fun startUpdatesChecker(enabled: Boolean) {
        Timber.d("startUpdatesChecker: called enabled=$enabled")
        if (!enabled) {
            if (!workManager.getWorkInfosByTag(UpdatesCheckerWorker.TAG).isCancelled) {
                workManager.cancelAllWorkByTag(UpdatesCheckerWorker.TAG)
            }
            return
        }

        workManager.enqueueUniquePeriodicWork(
            UpdatesCheckerWorker.TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            UpdatesCheckerWorker.createPeriodicRequest(),
        )
    }

    private fun startLibraryUpdates(enabled: Boolean, intervalHours: Int) {
        Timber.d("startLibraryUpdates: called enabled=$enabled intervalHours=$intervalHours")
        if (!enabled) {
            if (!workManager.getWorkInfosByTag(LibraryUpdatesWorker.TAG).isCancelled) {
                workManager.cancelAllWorkByTag(LibraryUpdatesWorker.TAG)
            }
            return
        }

        workManager.enqueueUniquePeriodicWork(
            LibraryUpdatesWorker.TAG,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            LibraryUpdatesWorker.createPeriodicRequest(
                updateCategory = LibraryCategory.DEFAULT,
                repeatIntervalHours = intervalHours
            ),
        )
    }

    // Lifecycle observer for foreground sync
    override fun onStart(owner: LifecycleOwner) {
        val serverUrl = appPreferences.SYNC_SERVER_URL.value
        Timber.d("PeriodicWorkersInitializer.onStart: app came to foreground, serverUrl='$serverUrl'")

        if (serverUrl.isBlank()) {
            Timber.d("PeriodicWorkersInitializer.onStart: no server URL configured, skipping sync")
            return
        }

        val apiKey = appPreferences.SYNC_API_KEY.value
        Timber.d("PeriodicWorkersInitializer.onStart: triggering sync on app foreground with serverUrl=$serverUrl, apiKey=${if (apiKey.isBlank()) "none" else "***"}")
        // Enqueue one-time sync work
        workManager.enqueueUniqueWork(
            SyncWorker.TAG_MANUAL,
            ExistingWorkPolicy.REPLACE,
            SyncWorker.createManualRequest(serverUrl, apiKey)
        )
    }

    fun init() {
        appCoroutineScope.launch {
            appPreferences.GLOBAL_APP_UPDATER_CHECKER_ENABLED
                .flow()
                .collectLatest { enabled ->
                    startUpdatesChecker(enabled)
                }
        }

        appCoroutineScope.launch {
            combine(
                appPreferences.GLOBAL_APP_AUTOMATIC_LIBRARY_UPDATES_ENABLED.flow(),
                appPreferences.GLOBAL_APP_AUTOMATIC_LIBRARY_UPDATES_INTERVAL_HOURS.flow()
            ) { enabled, intervalHours ->
                startLibraryUpdates(enabled, intervalHours)
            }.collect()
        }
    }

    fun startPeriodicSync(serverUrl: String) {
        Timber.d("startPeriodicSync: called with $serverUrl")
        val apiKey = appPreferences.SYNC_API_KEY.value
        workManager.enqueueUniquePeriodicWork(
            SyncWorker.TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            SyncWorker.createPeriodicRequest(serverUrl, apiKey),
        )
    }

    fun stopPeriodicSync() {
        Timber.d("stopPeriodicSync: called")
        workManager.cancelAllWorkByTag(SyncWorker.TAG)
    }
}