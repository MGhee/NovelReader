package my.novelreader.scraper.sources.mangadex

import kotlinx.serialization.Serializable

/**
 * Strongly-typed DTOs for the MangaDex REST API.
 *
 * Every field that the MangaDex API may legitimately return as JSON `null`
 * is declared nullable here so that kotlinx.serialization maps it to Kotlin
 * `null` instead of crashing the way Gson's `JsonNull.getAsString()` did.
 *
 * Only the fields the app actually consumes are listed; the JSON parser is
 * configured with `ignoreUnknownKeys = true` so additional API fields are
 * silently dropped.
 */

// ----- Manga (catalog / details) -----

@Serializable
data class MangaListResponse(
    val data: List<MangaData> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
)

@Serializable
data class MangaSingleResponse(
    val data: MangaData? = null,
)

@Serializable
data class MangaData(
    val id: String,
    val type: String? = null,
    val attributes: MangaAttributes,
    val relationships: List<Relationship> = emptyList(),
)

@Serializable
data class MangaAttributes(
    /**
     * Title is a localised map keyed by language code, e.g. `{"en": "Foo"}`.
     * Some titles only have non-English keys.
     */
    val title: Map<String, String> = emptyMap(),
    /**
     * Description is also a localised map. Optional.
     */
    val description: Map<String, String> = emptyMap(),
)

@Serializable
data class Relationship(
    val id: String,
    val type: String,
    val attributes: RelationshipAttributes? = null,
)

@Serializable
data class RelationshipAttributes(
    val fileName: String? = null,
)

// ----- Chapters -----

@Serializable
data class ChapterListResponse(
    val data: List<ChapterData> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
)

@Serializable
data class ChapterData(
    val id: String,
    val type: String? = null,
    val attributes: ChapterAttributes,
)

@Serializable
data class ChapterAttributes(
    /** Chapter number as a string ("12", "12.5", etc.). Null for oneshots. */
    val chapter: String? = null,
    /** Optional human title (e.g. "The First Step"). Often null. */
    val title: String? = null,
    /** ISO 639-1 language code of the translation. */
    val translatedLanguage: String? = null,
    /**
     * If the chapter is hosted off-MangaDex (MangaPlus, Azuki, ComiKey, …)
     * this is set and the chapter is unreadable inside our app — we skip it.
     */
    val externalUrl: String? = null,
    val pages: Int = 0,
)

// ----- At-Home (chapter image server) -----

@Serializable
data class AtHomeResponse(
    val baseUrl: String? = null,
    val chapter: AtHomeChapter? = null,
)

@Serializable
data class AtHomeChapter(
    val hash: String? = null,
    val data: List<String> = emptyList(),
    val dataSaver: List<String> = emptyList(),
)
