package my.novelreader.features.mangareader.loader

import my.novelreader.features.mangareader.model.MangaReaderPage

interface PageLoader {
    /**
     * Fetch the image URL for a page (if not already available).
     * Returns the updated page with imageUrl populated.
     */
    suspend fun loadPage(page: MangaReaderPage): Result<MangaReaderPage>

    /**
     * Clean up resources.
     */
    fun cleanup()
}
