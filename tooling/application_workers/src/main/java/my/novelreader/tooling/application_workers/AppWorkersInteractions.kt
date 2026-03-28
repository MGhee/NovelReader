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

    override fun syncWithServer(serverUrl: String, authToken: String) {
        workManager.enqueueUniqueWork(
            SyncWorker.TAG_MANUAL,
            ExistingWorkPolicy.REPLACE,
            SyncWorker.createManualRequest(serverUrl, authToken)
        )
    }

    override fun downloadAllBookChapters(bookUrl: String) {
        workManager.beginUniqueWork(
            "ChapterDownload_$bookUrl",
            ExistingWorkPolicy.REPLACE,
            ChapterDownloadWorker.createRequest(bookUrl)
        ).enqueue()
    }
}
