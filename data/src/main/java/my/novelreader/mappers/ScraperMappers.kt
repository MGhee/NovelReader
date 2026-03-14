package my.novelreader.mappers

import my.novelreader.core.PagedList
import my.novelreader.core.Response
import my.novelreader.core.syncMap
import my.novelreader.feature.local_database.BookMetadata
import my.novelreader.feature.local_database.ChapterMetadata
import my.novelreader.scraper.domain.BookResult
import my.novelreader.scraper.domain.ChapterResult

fun ChapterResult.mapToChapterMetadata() = ChapterMetadata(
    title = this.title,
    url = this.url,
)

fun List<ChapterResult>.mapToChapterMetadata() = map { it.mapToChapterMetadata() }

@Suppress("unused")
fun Response<List<ChapterResult>>.mapToChapterMetadata() = syncMap { it.mapToChapterMetadata() }

fun BookResult.mapToBookMetadata() = BookMetadata(
    title = this.title,
    url = this.url,
    coverImageUrl = this.coverImageUrl,
    description = this.description,
)

fun List<BookResult>.mapToBookMetadata() = map { it.mapToBookMetadata() }

fun PagedList<BookResult>.mapToBookMetadata() = PagedList(
    list = this.list.mapToBookMetadata(),
    index = this.index,
    isLastPage = this.isLastPage
)

fun Response<PagedList<BookResult>>.mapToBookMetadata() = syncMap { it.mapToBookMetadata() }

