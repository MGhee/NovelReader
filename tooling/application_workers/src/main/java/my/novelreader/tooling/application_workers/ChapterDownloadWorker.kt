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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import my.novelreader.data.BookChaptersRepository
import my.novelreader.data.ChapterBodyRepository
import my.novelreader.core.Response
import my.novelreader.feature.local_database.tables.ChapterBody
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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
        private const val BATCH_SIZE = 50

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
                val totalChapters = bookChaptersRepository.chaptersCount(bookUrl)
                val chaptersToDownload = bookChaptersRepository.getChaptersWithoutBody(bookUrl)
                val alreadyDownloaded = totalChapters - chaptersToDownload.size

                Timber.d("ChapterDownloadWorker: $alreadyDownloaded already downloaded, ${chaptersToDownload.size} remaining out of $totalChapters total for $bookUrl")

                if (chaptersToDownload.isEmpty()) {
                    setProgress(
                        Data.Builder()
                            .putString("bookUrl", bookUrl)
                            .putInt("progress", totalChapters)
                            .putInt("total", totalChapters)
                            .build()
                    )
                    return@withContext Result.success()
                }

                // Report initial progress (showing already downloaded portion)
                setProgress(
                    Data.Builder()
                        .putString("bookUrl", bookUrl)
                        .putInt("progress", alreadyDownloaded)
                        .putInt("total", totalChapters)
                        .build()
                )

                val semaphore = Semaphore(60)
                val downloadedCount = AtomicInteger(0)
                val failedCount = AtomicInteger(0)

                // Batch insert queue
                val pendingInserts = ConcurrentLinkedQueue<Pair<ChapterBody, String?>>()
                val flushingDone = AtomicBoolean(false)

                coroutineScope {
                    // Background flusher: drains queue to DB in batches
                    val flusher = launch(Dispatchers.IO) {
                        while (!flushingDone.get() || pendingInserts.isNotEmpty()) {
                            val batch = mutableListOf<Pair<ChapterBody, String?>>()
                            while (batch.size < BATCH_SIZE) {
                                val item = pendingInserts.poll() ?: break
                                batch.add(item)
                            }
                            if (batch.isNotEmpty()) {
                                chapterBodyRepository.batchInsertWithTitles(batch)
                            } else {
                                delay(50) // Brief wait for more items
                            }
                        }
                    }

                    // Download all chapters concurrently
                    chaptersToDownload.map { chapter ->
                        async {
                            semaphore.withPermit {
                                downloadChapter(chapter.url, bookUrl)
                            }
                        }
                    }.forEach { task ->
                        val result = task.await()
                        if (result != null) {
                            pendingInserts.add(result)
                            downloadedCount.incrementAndGet()
                        } else {
                            failedCount.incrementAndGet()
                        }

                        // Report cumulative progress
                        setProgress(
                            Data.Builder()
                                .putString("bookUrl", bookUrl)
                                .putInt("progress", alreadyDownloaded + downloadedCount.get())
                                .putInt("total", totalChapters)
                                .build()
                        )
                    }

                    // Signal flusher to finish remaining items
                    flushingDone.set(true)
                    flusher.join()
                }

                Timber.d("ChapterDownloadWorker: completed. Downloaded ${downloadedCount.get()}/${chaptersToDownload.size} chapters for $bookUrl")
                Result.success()
            }
        } catch (e: Exception) {
            Timber.e(e, "ChapterDownloadWorker: failed with exception for book $bookUrl")
            Result.retry()
        }
    }

    private suspend fun downloadChapter(chapterUrl: String, bookUrl: String): Pair<ChapterBody, String?>? {
        return when (val result = chapterBodyRepository.downloadChapterContentDirect(chapterUrl, bookUrl)) {
            is Response.Success -> result.data
            else -> null
        }
    }
}
