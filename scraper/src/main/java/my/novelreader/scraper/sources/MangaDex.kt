package my.novelreader.scraper.sources

import androidx.annotation.StringRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import my.novelreader.core.LanguageCode
import my.novelreader.core.PagedList
import my.novelreader.core.Response
import my.novelreader.network.NetworkClient
import my.novelreader.network.bodyString
import my.novelreader.network.tryConnect
import my.novelreader.scraper.MangaDirection
import my.novelreader.scraper.MangaReadingMode
import my.novelreader.scraper.MangaSourceInterface
import my.novelreader.scraper.domain.BookResult
import my.novelreader.scraper.domain.ChapterResult
import my.novelreader.scraper.sources.mangadex.AtHomeResponse
import my.novelreader.scraper.sources.mangadex.ChapterListResponse
import my.novelreader.scraper.sources.mangadex.MangaListResponse
import my.novelreader.scraper.sources.mangadex.MangaSingleResponse
import my.novelreader.strings.R
import java.net.URLEncoder

/**
 * MangaDex REST API source.
 *
 * Base API: https://api.mangadex.org
 * Book URLs:    https://mangadex.org/title/{uuid}
 * Chapter URLs: https://mangadex.org/chapter/{uuid}
 *
 * Implemented with kotlinx.serialization DTOs (see [mangadex.MangaDexDto]) so
 * nullable JSON fields like `chapter`, `title`, `description.en` deserialize
 * cleanly to Kotlin null instead of crashing the way Gson's `JsonNull` did.
 */
class MangaDex(
    private val networkClient: NetworkClient
) : MangaSourceInterface {
    override val id: String = "mangadex"

    @StringRes
    override val nameStrId: Int = R.string.source_mangadex

    override val baseUrl: String = "https://mangadex.org"
    override val catalogUrl: String = "$baseUrl/titles"
    override val language: LanguageCode? = LanguageCode.ENGLISH

    override val readingMode: MangaReadingMode = MangaReadingMode.WEBTOON
    override val direction: MangaDirection = MangaDirection.LTR

    private val apiBaseUrl = "https://api.mangadex.org"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val mangaId = bookUrl.toMangaId()
                val body = networkClient
                    .get("$apiBaseUrl/manga/$mangaId?includes[]=cover_art")
                    .bodyString()
                val parsed = json.decodeFromString<MangaSingleResponse>(body)
                parsed.data?.coverImageUrl()
            }
        }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val mangaId = bookUrl.toMangaId()
                val body = networkClient
                    .get("$apiBaseUrl/manga/$mangaId")
                    .bodyString()
                val parsed = json.decodeFromString<MangaSingleResponse>(body)
                parsed.data?.attributes?.description?.preferEnglish()
            }
        }

    override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val mangaId = bookUrl.toMangaId()
                val chapters = mutableListOf<ChapterResult>()
                var offset = 0

                while (true) {
                    val body = networkClient.get(
                        "$apiBaseUrl/manga/$mangaId/feed?limit=500&offset=$offset" +
                            "&order[volume]=asc&order[chapter]=asc" +
                            "&translatedLanguage[]=en" +
                            "&contentRating[]=safe&contentRating[]=suggestive" +
                            "&contentRating[]=erotica&contentRating[]=pornographic" +
                            "&includes[]=scanlation_group"
                    ).bodyString()

                    val parsed = json.decodeFromString<ChapterListResponse>(body)
                    if (parsed.data.isEmpty()) break

                    parsed.data.forEach { chapter ->
                        // Skip off-site chapters (MangaPlus, Azuki, ComiKey, …)
                        // — we cannot read them inside this app.
                        if (!chapter.attributes.externalUrl.isNullOrBlank()) return@forEach

                        val chapterNum = chapter.attributes.chapter ?: "?"
                        val titlePart = chapter.attributes.title?.takeIf { it.isNotBlank() }
                        val displayTitle = if (titlePart != null) {
                            "Ch.$chapterNum: $titlePart"
                        } else {
                            "Ch.$chapterNum"
                        }

                        chapters.add(
                            ChapterResult(
                                title = displayTitle,
                                url = "$baseUrl/chapter/${chapter.id}",
                            )
                        )
                    }

                    offset += parsed.limit.takeIf { it > 0 } ?: 500
                    if (offset >= parsed.total) break
                }

                chapters
            }
        }

    override suspend fun getChapterImages(chapterUrl: String): Response<List<String>> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val chapterId = chapterUrl.removePrefix("$baseUrl/chapter/").substringBefore("?")
                val body = networkClient
                    .get("$apiBaseUrl/at-home/server/$chapterId")
                    .bodyString()

                val parsed = json.decodeFromString<AtHomeResponse>(body)
                val cdn = parsed.baseUrl ?: return@tryConnect emptyList()
                val hash = parsed.chapter?.hash ?: return@tryConnect emptyList()
                val pages = parsed.chapter.data
                pages.map { fileName -> "$cdn/data/$hash/$fileName" }
            }
        }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            tryConnect("index=$index") {
                val offset = index * 20
                val body = networkClient.get(
                    "$apiBaseUrl/manga?limit=20&offset=$offset" +
                        "&order[latestUploadedChapter]=desc" +
                        "&includes[]=cover_art" +
                        "&availableTranslatedLanguage[]=en" +
                        "&contentRating[]=safe&contentRating[]=suggestive"
                ).bodyString()

                val parsed = json.decodeFromString<MangaListResponse>(body)
                val books = parsed.data.map { it.toBookResult() }

                PagedList(
                    list = books,
                    index = index,
                    isLastPage = offset + parsed.data.size >= parsed.total,
                )
            }
        }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String,
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            if (input.isBlank()) return@tryConnect PagedList.createEmpty(index)

            val offset = index * 20
            val encodedQuery = URLEncoder.encode(input, "UTF-8")
            val body = networkClient.get(
                "$apiBaseUrl/manga?title=$encodedQuery&limit=20&offset=$offset" +
                    "&includes[]=cover_art" +
                    "&contentRating[]=safe&contentRating[]=suggestive" +
                    "&contentRating[]=erotica&contentRating[]=pornographic"
            ).bodyString()

            val parsed = json.decodeFromString<MangaListResponse>(body)
            val books = parsed.data.map { it.toBookResult() }

            PagedList(
                list = books,
                index = index,
                isLastPage = offset + parsed.data.size >= parsed.total,
            )
        }
    }

    override suspend fun latest(page: Int): Response<PagedList<BookResult>> =
        getCatalogList(page - 1)

    override suspend fun popular(page: Int): Response<PagedList<BookResult>> =
        // MangaDex has no distinct "popular" endpoint — fall back to latest.
        getCatalogList(page - 1)

    // ----- helpers -----

    private fun String.toMangaId(): String =
        removePrefix("$baseUrl/title/").substringBefore("/").substringBefore("?")

    private fun Map<String, String>.preferEnglish(): String? =
        this["en"] ?: values.firstOrNull()

    private fun my.novelreader.scraper.sources.mangadex.MangaData.coverImageUrl(): String? {
        val coverFileName = relationships
            .firstOrNull { it.type == "cover_art" }
            ?.attributes
            ?.fileName
            ?: return null
        return "https://uploads.mangadex.org/covers/$id/$coverFileName"
    }

    private fun my.novelreader.scraper.sources.mangadex.MangaData.toBookResult(): BookResult {
        val title = attributes.title.preferEnglish() ?: "Unknown"
        return BookResult(
            title = title,
            url = "$baseUrl/title/$id",
            coverImageUrl = coverImageUrl().orEmpty(),
        )
    }
}
