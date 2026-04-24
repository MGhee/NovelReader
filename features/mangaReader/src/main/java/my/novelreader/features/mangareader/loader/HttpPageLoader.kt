package my.novelreader.features.mangareader.loader

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.novelreader.core.Response
import my.novelreader.features.mangareader.model.MangaReaderChapter
import my.novelreader.features.mangareader.model.MangaReaderPage
import my.novelreader.scraper.MangaSourceInterface

/**
 * Fetches manga pages from online sources via MangaSourceInterface.
 * Caches fetched image URLs in memory to avoid redundant source calls.
 */
class HttpPageLoader(
    private val chapter: MangaReaderChapter,
    private val source: MangaSourceInterface,
) : PageLoader {

    private val urlCache = LruCache<Int, String>(50)
    private var allImageUrls: List<String>? = null

    override suspend fun loadPage(page: MangaReaderPage): Result<MangaReaderPage> =
        withContext(Dispatchers.IO) {
            try {
                // Check cache first
                urlCache.get(page.index)?.let { cachedUrl ->
                    return@withContext Result.success(page.copy(imageUrl = cachedUrl))
                }

                // If page already has a URL, it's done
                if (page.imageUrl.isNotBlank()) {
                    urlCache.put(page.index, page.imageUrl)
                    return@withContext Result.success(page)
                }

                // Fetch all chapter image URLs if not already cached
                if (allImageUrls == null) {
                    val result = source.getChapterImages(chapter.url)
                    if (result is Response.Success) {
                        allImageUrls = result.data
                    } else {
                        return@withContext Result.failure(Exception("Failed to fetch chapter images"))
                    }
                }

                // Get the URL for this page
                val imageUrl = allImageUrls?.getOrNull(page.index)
                    ?: return@withContext Result.failure(Exception("No image URL for page ${page.index}"))

                urlCache.put(page.index, imageUrl)
                Result.success(page.copy(imageUrl = imageUrl))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun cleanup() {
        urlCache.evictAll()
        allImageUrls = null
    }
}
