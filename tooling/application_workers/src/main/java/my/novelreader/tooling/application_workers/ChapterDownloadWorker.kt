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
import my.novelreader.core.appPreferences.AppPreferences
import my.novelreader.data.BookChaptersRepository
import my.novelreader.data.ChapterBodyRepository
import my.novelreader.data.LibraryBooksRepository
import my.novelreader.data.MangaImagePrefetcher
import my.novelreader.core.Response
import my.novelreader.feature.local_database.tables.Chapter
import my.novelreader.feature.local_database.tables.ChapterBody
import my.novelreader.scraper.MangaSourceInterface
import my.novelreader.scraper.Scraper
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
    private val libraryBooksRepository: LibraryBooksRepository,
    private val appPreferences: AppPreferences,
    private val scraper: Scraper,
    private val mangaImagePrefetcher: MangaImagePrefetcher,
) : CoroutineWorker(context, workerParameters) {

    companion object {
        const val TAG = "ChapterDownload"
        private const val DATA_BOOK_URL = "bookUrl"
        private const val BATCH_SIZE = 50
        private const val CHAPTER_SOURCE_DELIMITER = " — "
        private const val DEFAULT_MANGA_CHAPTER_CONCURRENCY = 4
        private const val COMIX_MANGA_CHAPTER_CONCURRENCY = 1

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

        if (shouldStopDownload(bookUrl)) {
            Timber.d("ChapterDownloadWorker: skipping removed book $bookUrl")
            return Result.success()
        }

        Timber.d("ChapterDownloadWorker: starting download for book $bookUrl")

        return try {
            withContext(Dispatchers.IO) {
                val source = scraper.getCompatibleSource(bookUrl)
                if (source is MangaSourceInterface) {
                    val allChapters = resolveMangaChaptersForDownload(bookUrl)
                    val pendingChapterUrls = bookChaptersRepository.getChaptersWithoutBody(bookUrl)
                        .asSequence()
                        .map { it.url }
                        .toSet()
                    val chaptersToDownload = allChapters.filter { it.url in pendingChapterUrls }
                    val totalChapters = allChapters.size
                    val alreadyDownloaded = totalChapters - chaptersToDownload.size

                    Timber.d(
                        "ChapterDownloadWorker(manga): using ${chaptersToDownload.size}/${allChapters.size} chapters for selected source on $bookUrl"
                    )

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

                    setProgress(
                        Data.Builder()
                            .putString("bookUrl", bookUrl)
                            .putInt("progress", alreadyDownloaded)
                            .putInt("total", totalChapters)
                            .build()
                    )

                    return@withContext downloadMangaChapters(
                        bookUrl = bookUrl,
                        chapterUrls = chaptersToDownload.map { it.url },
                        source = source,
                        totalChapters = totalChapters,
                        alreadyDownloaded = alreadyDownloaded,
                    )
                }

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
                                if (shouldStopDownload(bookUrl)) return@withPermit null
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

                        if (shouldStopDownload(bookUrl)) {
                            flushingDone.set(true)
                            return@coroutineScope
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

    private suspend fun downloadMangaChapters(
        bookUrl: String,
        chapterUrls: List<String>,
        source: MangaSourceInterface,
        totalChapters: Int,
        alreadyDownloaded: Int,
    ): Result = coroutineScope {
        val semaphore = Semaphore(
            if (source.id == "comixto") COMIX_MANGA_CHAPTER_CONCURRENCY
            else DEFAULT_MANGA_CHAPTER_CONCURRENCY
        )
        val downloadedCount = AtomicInteger(0)
        val failedCount = AtomicInteger(0)

        chapterUrls.map { chapterUrl ->
            async {
                semaphore.withPermit {
                    if (shouldStopDownload(bookUrl)) return@withPermit false
                    runCatching {
                        when (val result = source.getChapterImages(chapterUrl)) {
                            is Response.Success -> {
                                if (shouldStopDownload(bookUrl)) return@runCatching false
                                val allOk = mangaImagePrefetcher.prefetchAndAwait(result.data)
                                if (allOk && !shouldStopDownload(bookUrl)) {
                                    chapterBodyRepository.saveMangaChapterPages(chapterUrl, result.data)
                                    true
                                } else {
                                    Timber.w("ChapterDownloadWorker(manga): incomplete prefetch for $chapterUrl, skipping save")
                                    false
                                }
                            }
                            else -> false
                        }
                    }.getOrDefault(false)
                }
            }
        }.forEach { task ->
            val ok = task.await()
            if (ok) downloadedCount.incrementAndGet() else failedCount.incrementAndGet()

            setProgress(
                Data.Builder()
                    .putString("bookUrl", bookUrl)
                    .putInt("progress", alreadyDownloaded + downloadedCount.get())
                    .putInt("total", totalChapters)
                    .build()
            )
        }

        Timber.d("ChapterDownloadWorker(manga): completed. Downloaded ${downloadedCount.get()}/${chapterUrls.size} chapters for $bookUrl")
        Result.success()
    }

    private suspend fun resolveMangaChaptersForDownload(bookUrl: String): List<Chapter> {
        val chapters = bookChaptersRepository.chapters(bookUrl).sortedBy { it.position }
        if (chapters.isEmpty()) return chapters

        val chaptersBySource = chapters
            .mapNotNull { chapter -> extractChapterSource(chapter.title)?.let { it to chapter } }
            .groupBy({ it.first }, { it.second })

        if (chaptersBySource.isEmpty()) return chapters

        val selectedSource = appPreferences.getPreferredMangaSource(bookUrl)
            ?.takeIf { chaptersBySource.containsKey(it) }
            ?: resolveLargestSource(chaptersBySource)

        return chaptersBySource[selectedSource] ?: chapters
    }

    private fun resolveLargestSource(chaptersBySource: Map<String, List<Chapter>>): String? =
        chaptersBySource.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, List<Chapter>>> { it.value.size }
                    .thenBy { it.key.lowercase() }
            )
            .firstOrNull()
            ?.key

    private fun extractChapterSource(title: String): String? =
        title.substringAfterLast(CHAPTER_SOURCE_DELIMITER, "")
            .trim()
            .takeIf { it.isNotBlank() }

    private suspend fun shouldStopDownload(bookUrl: String): Boolean {
        return isStopped || !libraryBooksRepository.existInLibrary(bookUrl)
    }
}
