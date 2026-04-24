package my.novelreader.features.chapterslist

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import my.novelreader.data.AppRepository
import my.novelreader.data.DownloaderRepository
import my.novelreader.core.appPreferences.AppPreferences
import my.novelreader.core.appPreferences.TernaryState
import my.novelreader.feature.local_database.tables.Book
import my.novelreader.feature.local_database.tables.ContentType
import my.novelreader.scraper.MangaSourceInterface
import my.novelreader.scraper.Scraper
import my.novelreader.scraper.SourceInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ChaptersRepository @Inject constructor(
    private val appRepository: AppRepository,
    private val downloaderRepository: DownloaderRepository,
    private val appPreferences: AppPreferences,
    private val scraper: Scraper,
) {

    suspend fun downloadBookMetadata(bookUrl: String, bookTitle: String) = coroutineScope {
        val coverUrl = async { downloaderRepository.bookCoverImageUrl(bookUrl = bookUrl) }
        val description = async { downloaderRepository.bookDescription(bookUrl = bookUrl) }

        val source = scraper.getCompatibleSource(bookUrl) as? SourceInterface.Catalog
        Log.d("ChaptersRepository", "bookUrl=$bookUrl, source=${source?.id ?: "null"}, isManga=${source is MangaSourceInterface}")
        val contentType = if (source is MangaSourceInterface) ContentType.MANGA else ContentType.NOVEL
        Log.d("ChaptersRepository", "contentType=$contentType")

        appRepository.libraryBooks.insert(
            Book(
                title = bookTitle,
                url = bookUrl,
                coverImageUrl = coverUrl.await().toSuccessOrNull()?.data ?: "",
                description = description.await().toSuccessOrNull()?.data ?: "",
                contentType = contentType
            )
        )

        // Backfill existing rows with correct contentType (handles case where book was already in library with wrong type)
        appRepository.libraryBooks.updateContentType(bookUrl, contentType)
    }


    fun getChaptersSortedFlow(bookUrl: String) = appRepository.bookChapters
        .getChaptersWithContextFlow(bookUrl = bookUrl)
        .map(::removeCommonTextFromTitles)
        // Sort the chapters given the order preference
        .combine(appPreferences.CHAPTERS_SORT_ASCENDING.flow()) { chapters, sorted ->
            when (sorted) {
                TernaryState.Active -> chapters.sortedBy { it.chapter.position }
                TernaryState.Inverse -> chapters.sortedByDescending { it.chapter.position }
                TernaryState.Inactive -> chapters
            }
        }
        .flowOn(Dispatchers.Default)

    suspend fun getLastReadChapter(bookUrl: String): String? =
        appRepository.libraryBooks.get(bookUrl)?.lastReadChapter
            ?: appRepository.bookChapters.getFirstChapter(bookUrl)?.url

}