package my.novelreader.features.mangareader.model

data class MangaReaderChapter(
    val url: String,
    val title: String,
    val scanlator: String = "",
    val dateUpload: Long = 0,
    val pages: List<MangaReaderPage> = emptyList(),
    val number: Float = -1f,
)
