package my.novelreader.features.reader.domain

import my.novelreader.feature.local_database.tables.Chapter

internal data class ChapterStats(
    val itemsCount: Int,
    val chapter: Chapter,
    val orderedChaptersIndex: Int
)