package my.novelreader.scraper.sources.asurascans

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AsuraSearchResponse(
    val data: List<AsuraComic> = emptyList(),
    val meta: MetaInfo? = null
)

@Serializable
data class AsuraChaptersResponse(
    val data: List<AsuraChapter> = emptyList(),
    val meta: MetaInfo? = null
)

@Serializable
data class AsuraComic(
    val id: Int = 0,
    val slug: String = "",
    val title: String = "",
    val description: String? = null,
    val cover: String? = null,
    @SerialName("public_url")
    val publicUrl: String = ""
)

@Serializable
data class AsuraChapter(
    val id: Int = 0,
    val number: Double = 0.0,
    val title: String? = null,
    val slug: String = ""
)

@Serializable
data class MetaInfo(
    val total: Int = 0,
    val per_page: Int = 20
)
