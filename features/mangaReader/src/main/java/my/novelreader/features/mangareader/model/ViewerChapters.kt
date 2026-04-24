package my.novelreader.features.mangareader.model

data class ViewerChapters(
    val prevChapter: MangaReaderChapter? = null,
    val currentChapter: MangaReaderChapter,
    val nextChapter: MangaReaderChapter? = null,
)
