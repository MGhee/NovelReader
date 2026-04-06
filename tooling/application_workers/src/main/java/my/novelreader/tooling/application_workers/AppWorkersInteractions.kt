package my.novelreader.tooling.application_workers

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import my.novelreader.interactor.WorkersInteractions
import my.novelreader.core.domain.LibraryCategory
import timber.log.Timber
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
        Timber.d("AppWorkersInteractions: enqueueing download for $bookUrl")
        workManager.beginUniqueWork(
            "ChapterDownload_$bookUrl",
            ExistingWorkPolicy.KEEP,
            ChapterDownloadWorker.createRequest(bookUrl)
        ).enqueue()
        Timber.d("AppWorkersInteractions: download enqueued for $bookUrl")
    }

    override fun cancelDownload(bookUrl: String) {
        workManager.cancelUniqueWork("ChapterDownload_$bookUrl")
    }

    override fun observeDownloadProgress(bookUrl: String): Flow<Pair<Int, Int>?> {
        return workManager.getWorkInfosForUniqueWorkFlow("ChapterDownload_$bookUrl")
            .map { workInfos ->
                val info = workInfos.firstOrNull() ?: return@map null
                if (info.state.isFinished) {
                    null
                } else {
                    val progress = info.progress.getInt("progress", 0)
                    val total = info.progress.getInt("total", 0)
                    if (total > 0) Pair(progress, total) else null
                }
            }
    }

    override fun observeActiveDownloads(): Flow<Map<String, Pair<Int, Int>>> {
        return workManager.getWorkInfosByTagFlow(ChapterDownloadWorker.TAG)
            .map { workInfos ->
                workInfos
                    .filter { !it.state.isFinished }
                    .mapNotNull { info ->
                        val bookUrl = info.progress.getString("bookUrl") ?: return@mapNotNull null
                        val progress = info.progress.getInt("progress", 0)
                        val total = info.progress.getInt("total", 0)
                        if (total > 0) bookUrl to Pair(progress, total) else null
                    }
                    .toMap()
            }
    }
}
