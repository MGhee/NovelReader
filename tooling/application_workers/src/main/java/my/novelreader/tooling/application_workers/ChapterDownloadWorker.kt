package my.novelreader.tooling.application_workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.novelreader.data.BookChaptersRepository
import my.novelreader.data.ChapterBodyRepository
import my.novelreader.core.Response
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
internal class ChapterDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val chapterBodyRepository: ChapterBodyRepository,
    private val bookChaptersRepository: BookChaptersRepository,
) : CoroutineWorker(context, workerParameters) {

    companion object {
        const val TAG = "ChapterDownload"
        private const val DATA_BOOK_URL = "bookUrl"

        fun createRequest(bookUrl: String): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
                .addTag(TAG)
                .setConstraints(constraints)
                .setInputData(
                    Data.Builder()
                        .putString(DATA_BOOK_URL, bookUrl)
                        .build()
                )
                .build()
        }
    }

    override suspend fun doWork(): Result {
        val bookUrl = inputData.getString(DATA_BOOK_URL) ?: return Result.failure()

        Timber.d("ChapterDownloadWorker: starting download for book $bookUrl")

        return try {
            withContext(Dispatchers.IO) {
                val chaptersToDownload = bookChaptersRepository.getChaptersWithoutBody(bookUrl)

                Timber.d("ChapterDownloadWorker: found ${chaptersToDownload.size} chapters to download")

                var downloaded = 0
                chaptersToDownload.forEach { chapter ->
                    val result = chapterBodyRepository.downloadChapterBody(chapter.url)

                    if (result is Response.Success) {
                        downloaded++
                        Timber.d("ChapterDownloadWorker: downloaded ${chapter.title} ($downloaded/${chaptersToDownload.size})")

                        setProgress(
                            Data.Builder()
                                .putInt("chapterNumber", chapter.position)
                                .putString("chapterTitle", chapter.title)
                                .putInt("progress", downloaded)
                                .putInt("total", chaptersToDownload.size)
                                .build()
                        )
                    } else {
                        Timber.w("ChapterDownloadWorker: failed to download ${chapter.title}")
                    }

                    // Respect network by adding small delay between requests
                    if (downloaded < chaptersToDownload.size) {
                        Thread.sleep(500)
                    }
                }

                Timber.d("ChapterDownloadWorker: completed. Downloaded $downloaded/${chaptersToDownload.size} chapters")
                Result.success()
            }
        } catch (e: Exception) {
            Timber.e(e, "ChapterDownloadWorker: failed with exception")
            Result.retry()
        }
    }
}
