package my.novelreader.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.novelreader.core.AppFileResolver
import my.novelreader.core.getOrNull
import my.novelreader.core.tryAsResponse
import my.novelreader.epub_tooling.EpubWriter
import my.novelreader.network.NetworkClient
import okhttp3.Request
import java.io.File
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpubExporterRepository @Inject constructor(
    private val libraryBooks: LibraryBooksRepository,
    private val bookChapters: BookChaptersRepository,
    private val chapterBody: ChapterBodyRepository,
    private val appFileResolver: AppFileResolver,
    private val networkClient: NetworkClient,
) {
    /**
     * Writes the book as an EPUB into [outputStream].
     * Only chapters whose content has been downloaded are included.
     */
    suspend fun exportEpub(
        bookUrl: String,
        outputStream: OutputStream,
        onProgress: (exported: Int, total: Int) -> Unit = { _, _ -> },
    ): Unit = withContext(Dispatchers.IO) {
        val book = libraryBooks.get(bookUrl) ?: throw Exception("Book not found")
        val total = chapterBody.getDownloadedCount(bookUrl)
        if (total == 0)
            throw Exception("No downloaded chapters to export")

        val writer = EpubWriter(
            outputStream = outputStream,
            identifier = "urn:uuid:${UUID.nameUUIDFromBytes(bookUrl.toByteArray())}",
            title = book.title,
            description = book.description,
        )

        if (book.coverImageUrl.isNotBlank()) {
            loadImage(bookUrl, book.coverImageUrl)?.let(writer::setCover)
        }

        var exported = 0
        bookChapters.chapters(bookUrl)
            .sortedBy { it.position }
            .forEach { chapter ->
                val body = chapterBody.get(chapter.url)?.body ?: return@forEach
                writer.addChapter(title = chapter.title, body = body) { loadImage(bookUrl, it) }
                onProgress(++exported, total)
            }

        writer.finish()
    }

    private suspend fun loadImage(bookUrl: String, imagePath: String): ByteArray? = tryAsResponse {
        when (val resolved = appFileResolver.resolvedBookImagePath(bookUrl, imagePath)) {
            is File -> resolved.takeIf { it.isFile && it.isInsideBooksFolder() }?.readBytes()
            is String -> networkClient
                .call(Request.Builder().url(resolved), followRedirects = true)
                .use { if (it.isSuccessful) it.body?.bytes() else null }
            else -> null
        }
    }.getOrNull()

    private fun File.isInsideBooksFolder(): Boolean =
        canonicalPath.startsWith(appFileResolver.folderBooks.canonicalPath + File.separator)
}
