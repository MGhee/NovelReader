package my.novelreader.features.mangareader.loader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.novelreader.core.Response
import my.novelreader.features.mangareader.model.MangaReaderChapter
import my.novelreader.features.mangareader.model.MangaReaderPage
import my.novelreader.scraper.MangaSourceInterface

/**
 * Orchestrates loading chapters and their pages from a source.
 * Manages prefetching of adjacent chapters and page loading.
 */
data class ChapterInfo(
    val url: String,
    val title: String,
    val position: Int,
)

class ChapterLoader(
    private val source: MangaSourceInterface,
    private val chapters: List<ChapterInfo>,
    private val onChapterLoaded: (suspend (MangaReaderChapter) -> Unit)? = null,
    private val getCachedImageUrls: (suspend (String) -> List<String>?)? = null,
) {
    private var prefetchJob: Job? = null
    private val pageLoaders = mutableMapOf<String, PageLoader>()

    /**
     * Load a specific chapter by URL. Returns chapter with pages populated.
     * Tries local cache first (for offline reading), falls back to network.
     */
    suspend fun loadChapter(chapterUrl: String): Result<MangaReaderChapter> =
        withContext(Dispatchers.IO) {
            try {
                val chapterInfo = chapters.find { it.url == chapterUrl }
                val chapterNumber = chapterInfo?.let { (it.position + 1).toFloat() } ?: -1f
                val displayTitle = chapterInfo?.let { formatChapterTitle(it.title, chapterNumber) }
                    ?: "Chapter ${if (chapterNumber % 1 == 0f) chapterNumber.toInt() else chapterNumber}"

                // Try local cache first
                val cachedUrls = getCachedImageUrls?.invoke(chapterUrl)
                val imageUrls = if (cachedUrls != null) {
                    cachedUrls
                } else {
                    // Fall back to network
                    val response = source.getChapterImages(chapterUrl)
                    if (response !is Response.Success) {
                        val errorMsg = when (response) {
                            is Response.Error -> response.exception?.message ?: "Unknown error"
                            else -> "Failed to load chapter images"
                        }
                        return@withContext Result.failure(Exception(errorMsg))
                    }
                    response.data
                }

                val pages = imageUrls.mapIndexed { index, url ->
                    MangaReaderPage(index = index, imageUrl = url)
                }

                val chapter = MangaReaderChapter(
                    url = chapterUrl,
                    title = displayTitle,
                    pages = pages,
                    number = chapterNumber,
                )

                onChapterLoaded?.invoke(chapter)
                Result.success(chapter)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Format chapter title intelligently:
     * - If title already looks like chapter info (e.g., "Chapter 5", "Ch. 3: Title"), use verbatim
     * - Otherwise use "Chapter N" (where N is int if whole, float if fractional) with ": Title" appended
     */
    private fun formatChapterTitle(dbTitle: String, chapterNumber: Float): String {
        val trimmed = dbTitle.trim()
        if (trimmed.isEmpty()) {
            return "Chapter ${if (chapterNumber % 1 == 0f) chapterNumber.toInt() else chapterNumber}"
        }

        val chapterPattern = Regex("""^(?:Chapter|Ch\.)\s*\d+(?:\.\d+)?(?:\s*[:—\-].+)?$""", RegexOption.IGNORE_CASE)
        if (chapterPattern.matches(trimmed)) {
            return trimmed
        }

        val numberStr = if (chapterNumber % 1 == 0f) chapterNumber.toInt().toString() else chapterNumber.toString()
        return if (trimmed.matches(Regex("""^\d+(?:\.\d+)?$"""))) {
            "Chapter $trimmed"
        } else {
            "Chapter $numberStr: $trimmed"
        }
    }

    /**
     * Prefetch page URLs for a chapter (lazy-loads image URLs).
     */
    suspend fun prefetchPages(chapter: MangaReaderChapter, count: Int = 5) =
        coroutineScope {
            val loader = pageLoaders.getOrPut(chapter.url) {
                HttpPageLoader(chapter, source)
            }

            // Prefetch first N pages
            chapter.pages.take(count).forEach { page ->
                launch(Dispatchers.IO) {
                    loader.loadPage(page)
                }
            }
        }

    /**
     * Clean up resources for a chapter.
     */
    fun cleanup(chapterUrl: String) {
        pageLoaders.remove(chapterUrl)?.cleanup()
    }

    /**
     * Cancel any pending prefetch operations.
     */
    fun cancelPrefetch() {
        prefetchJob?.cancel()
    }

    /**
     * Get the page loader for a chapter, creating one if needed.
     */
    fun getPageLoader(chapter: MangaReaderChapter): PageLoader {
        return pageLoaders.getOrPut(chapter.url) {
            HttpPageLoader(chapter, source)
        }
    }
}
