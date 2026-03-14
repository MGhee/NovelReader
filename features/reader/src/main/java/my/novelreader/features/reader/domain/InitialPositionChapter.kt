package my.novelreader.features.reader.domain

internal data class InitialPositionChapter(
    val chapterIndex: Int,
    val chapterItemPosition: Int,
    val chapterItemOffset: Int
)