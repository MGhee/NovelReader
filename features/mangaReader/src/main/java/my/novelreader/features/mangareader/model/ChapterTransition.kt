package my.novelreader.features.mangareader.model

sealed class ChapterTransition {
    data class Prev(val chapter: MangaReaderChapter) : ChapterTransition()
    data class Next(val chapter: MangaReaderChapter) : ChapterTransition()
    object End : ChapterTransition()
}
