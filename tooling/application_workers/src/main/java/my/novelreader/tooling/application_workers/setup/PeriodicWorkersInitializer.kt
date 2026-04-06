package my.novelreader.tooling.application_workers.setup

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import my.novelreader.core.AppCoroutineScope
import my.novelreader.core.Response
import my.novelreader.core.appPreferences.AppPreferences
import my.novelreader.core.domain.LibraryCategory
import my.novelreader.data.SyncRepository
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
    private val appCoroutineScope: AppCoroutineScope,
    private val syncRepository: SyncRepository,
) : DefaultLifecycleObserver {

    private var foregroundSyncJob: Job? = null

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

    private fun cancelPeriodicLibraryUpdates() {
        Timber.d("cancelPeriodicLibraryUpdates: cleaning up old periodic work")
        if (!workManager.getWorkInfosByTag(LibraryUpdatesWorker.TAG).isCancelled) {
            workManager.cancelAllWorkByTag(LibraryUpdatesWorker.TAG)
        }
    }

    // Lifecycle observer for foreground events
    override fun onStart(owner: LifecycleOwner) {
        Timber.d("PeriodicWorkersInitializer.onStart: app came to foreground")

        // Check for library updates on every app open
        checkLibraryUpdatesOnForeground()

        // Foreground sync
        val serverUrl = appPreferences.SYNC_SERVER_URL.value
        if (serverUrl.isBlank()) {
            Timber.d("PeriodicWorkersInitializer.onStart: no server URL configured, skipping sync")
            return
        }

        // Skip if a foreground sync is already running
        if (foregroundSyncJob?.isActive == true) {
            Timber.d("PeriodicWorkersInitializer.onStart: sync already in progress, skipping")
            return
        }

        // Use session token, fall back to API key for backward compat
        val authToken = appPreferences.SYNC_SESSION_TOKEN.value.ifBlank {
            appPreferences.SYNC_API_KEY.value
        }
        if (authToken.isBlank()) {
            Timber.d("PeriodicWorkersInitializer.onStart: no auth token, skipping sync")
            return
        }

        Timber.d("PeriodicWorkersInitializer.onStart: triggering direct foreground sync")

        foregroundSyncJob = appCoroutineScope.launch {
            try {
                val result = syncRepository.syncWithServer(serverUrl, authToken)
                when (result) {
                    is Response.Success -> Timber.d("Foreground sync successful: ${result.data}")
                    is Response.Error -> Timber.e(result.exception, "Foreground sync error: ${result.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Foreground sync unexpected error")
            }
        }
    }

    private fun checkLibraryUpdatesOnForeground() {
        Timber.d("PeriodicWorkersInitializer: checking library updates on foreground")
        workManager.beginUniqueWork(
            LibraryUpdatesWorker.TAG_MANUAL,
            ExistingWorkPolicy.KEEP,
            LibraryUpdatesWorker.createManualRequest(
                updateCategory = LibraryCategory.DEFAULT
            )
        ).enqueue()
    }

    fun init() {
        appCoroutineScope.launch {
            appPreferences.GLOBAL_APP_UPDATER_CHECKER_ENABLED
                .flow()
                .collectLatest { enabled ->
                    startUpdatesChecker(enabled)
                }
        }

        // Cancel any old periodic library update work — updates now happen on app foreground
        cancelPeriodicLibraryUpdates()
    }

    fun startPeriodicSync(serverUrl: String) {
        Timber.d("startPeriodicSync: called with $serverUrl")
        // Use session token, fall back to API key for backward compat
        val authToken = appPreferences.SYNC_SESSION_TOKEN.value.ifBlank {
            appPreferences.SYNC_API_KEY.value
        }
        workManager.enqueueUniquePeriodicWork(
            SyncWorker.TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            SyncWorker.createPeriodicRequest(serverUrl, authToken),
        )
    }

    fun stopPeriodicSync() {
        Timber.d("stopPeriodicSync: called")
        workManager.cancelAllWorkByTag(SyncWorker.TAG)
    }
}