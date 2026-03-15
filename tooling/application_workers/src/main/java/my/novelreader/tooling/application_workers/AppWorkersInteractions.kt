package my.novelreader.tooling.application_workers

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import my.novelreader.interactor.WorkersInteractions
import my.novelreader.core.domain.LibraryCategory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppWorkersInteractions @Inject constructor(
    private val workManager: WorkManager
): WorkersInteractions {

    override fun checkForLibraryUpdates(libraryCategory: LibraryCategory) {
        workManager.beginUniqueWork(
            LibraryUpdatesWorker.TAG_MANUAL,
            ExistingWorkPolicy.REPLACE,
            LibraryUpdatesWorker.createManualRequest(updateCategory = libraryCategory)
        ).enqueue()
    }

    override fun syncWithServer(serverUrl: String, apiKey: String) {
        workManager.beginUniqueWork(
            SyncWorker.TAG_MANUAL,
            ExistingWorkPolicy.REPLACE,
            SyncWorker.createManualRequest(serverUrl, apiKey)
        ).enqueue()
    }
}
