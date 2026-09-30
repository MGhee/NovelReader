package my.novelreader.epub_tooling

import my.novelreader.core.BookTextMapper
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Streams a book into an EPUB 3 file (with an NCX table of contents for EPUB 2 readers).
 * Chapter bodies use the app's text format: paragraphs separated by blank lines and
 * [BookTextMapper.ImgEntry] images. Call [finish] once all chapters have been added.
 */
class EpubWriter(
    outputStream: OutputStream,
    private val identifier: String,
    private val title: String,
    private val description: String?,
    private val language: String = "en",
) {
    private class Item(
        val id: String,
        val href: String,
        val mediaType: String,
        val properties: String? = null
    )

    private class NavEntry(val title: String, val href: String)

    private val zip = ZipOutputStream(outputStream)
    private val items = mutableListOf<Item>()
    private val navEntries = mutableListOf<NavEntry>()
    private val imageHrefs = mutableMapOf<String, String?>()
    private var hasCover = false

    init {
        // The spec requires an uncompressed "mimetype" file as the very first zip entry.
        val mimetype = "application/epub+zip".toByteArray(Charsets.US_ASCII)
        val entry = ZipEntry("mimetype").apply {
            method = ZipEntry.STORED
            size = mimetype.size.toLong()
            compressedSize = mimetype.size.toLong()
            crc = CRC32().apply { update(mimetype) }.value
        }
        zip.putNextEntry(entry)
        zip.write(mimetype)
        zip.closeEntry()
        writeEntry("META-INF/container.xml", CONTAINER_XML)
    }

    fun setCover(image: ByteArray) {
        if (hasCover) return
        addImage(id = COVER_ID, data = image, properties = "cover-image")
        hasCover = true
    }

    suspend fun addChapter(
        title: String,
        body: String,
        loadImage: suspend (path: String) -> ByteArray?
    ) {
        val index = navEntries.size + 1
        val chapterTitle = title.ifBlank { "Chapter $index" }
        val href = "text/chapter_${index.toString().padStart(5, '0')}.xhtml"

        val content = StringBuilder("<h1>${chapterTitle.escapeXml()}</h1>\n")
        for (paragraph in body.split("\n\n")) {
            val text = paragraph.trim()
            if (text.isEmpty()) continue
            val imgEntry = if (text.contains("<img")) BookTextMapper.ImgEntry.fromXMLString(text) else null
            if (imgEntry == null) {
                content.append("<p>")
                    .append(text.escapeXml().replace("\n", "<br/>"))
                    .append("</p>\n")
            } else {
                val imageHref = imageHref(imgEntry.path, loadImage) ?: continue
                content.append("<div class=\"image\"><img src=\"../")
                    .append(imageHref)
                    .append("\" alt=\"\"/></div>\n")
            }
        }

        writeEntry("$ROOT/$href", xhtmlDocument(chapterTitle, content.toString()))
        items += Item(id = "chapter-$index", href = href, mediaType = XHTML_MEDIA_TYPE)
        navEntries += NavEntry(title = chapterTitle, href = href)
    }

    fun finish() {
        writeEntry("$ROOT/nav.xhtml", navDocument())
        writeEntry("$ROOT/toc.ncx", ncxDocument())
        writeEntry("$ROOT/content.opf", opfDocument())
        zip.close()
    }

    private suspend fun imageHref(
        path: String,
        loadImage: suspend (path: String) -> ByteArray?
    ): String? {
        if (path in imageHrefs) return imageHrefs[path]
        val id = "image-${imageHrefs.size + 1}"
        val href = loadImage(path)?.let { addImage(id = id, data = it) }
        imageHrefs[path] = href
        return href
    }

    private fun addImage(id: String, data: ByteArray, properties: String? = null): String {
        val (mediaType, extension) = imageType(data)
        val href = "images/$id.$extension"
        writeEntry("$ROOT/$href", data)
        items += Item(id = id, href = href, mediaType = mediaType, properties = properties)
        return href
    }

    private fun writeEntry(path: String, data: ByteArray) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(data)
        zip.closeEntry()
    }

    private fun writeEntry(path: String, text: String) =
        writeEntry(path, text.toByteArray(Charsets.UTF_8))

    private fun xhtmlDocument(title: String, body: String) =
        """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="$language" lang="$language">
<head>
<title>${title.escapeXml()}</title>
</head>
<body>
$body
</body>
</html>
"""

    private fun navDocument(): String {
        val entries = navEntries.joinToString("\n") {
            "<li><a href=\"${it.href}\">${it.title.escapeXml()}</a></li>"
        }
        return xhtmlDocument(
            title = title,
            body = "<nav epub:type=\"toc\" id=\"toc\">\n<h1>${title.escapeXml()}</h1>\n<ol>\n$entries\n</ol>\n</nav>"
        )
    }

    private fun ncxDocument(): String {
        val navPoints = navEntries.withIndex().joinToString("\n") { (i, entry) ->
            """<navPoint id="navPoint-${i + 1}" playOrder="${i + 1}">
<navLabel><text>${entry.title.escapeXml()}</text></navLabel>
<content src="${entry.href}"/>
</navPoint>"""
        }
        return """<?xml version="1.0" encoding="UTF-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
<head>
<meta name="dtb:uid" content="${identifier.escapeXml()}"/>
<meta name="dtb:depth" content="1"/>
<meta name="dtb:totalPageCount" content="0"/>
<meta name="dtb:maxPageNumber" content="0"/>
</head>
<docTitle><text>${title.escapeXml()}</text></docTitle>
<navMap>
$navPoints
</navMap>
</ncx>
"""
    }

    private fun opfDocument(): String = buildString {
        val modified = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())

        append(
            """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="book-id">
<metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
<dc:identifier id="book-id">${identifier.escapeXml()}</dc:identifier>
<dc:title>${title.escapeXml()}</dc:title>
<dc:language>${language.escapeXml()}</dc:language>
"""
        )
        if (!description.isNullOrBlank())
            append("<dc:description>${description.escapeXml()}</dc:description>\n")
        append("<meta property=\"dcterms:modified\">$modified</meta>\n")
        if (hasCover)
            append("<meta name=\"cover\" content=\"$COVER_ID\"/>\n")
        append("</metadata>\n<manifest>\n")
        append("<item id=\"ncx\" href=\"toc.ncx\" media-type=\"application/x-dtbncx+xml\"/>\n")
        append("<item id=\"nav\" href=\"nav.xhtml\" media-type=\"$XHTML_MEDIA_TYPE\" properties=\"nav\"/>\n")
        items.forEach { item ->
            append("<item id=\"${item.id}\" href=\"${item.href}\" media-type=\"${item.mediaType}\"")
            if (item.properties != null) append(" properties=\"${item.properties}\"")
            append("/>\n")
        }
        append("</manifest>\n<spine toc=\"ncx\">\n")
        items.filter { it.mediaType == XHTML_MEDIA_TYPE }.forEach {
            append("<itemref idref=\"${it.id}\"/>\n")
        }
        append("</spine>\n</package>\n")
    }

    private companion object {
        const val ROOT = "OEBPS"
        const val COVER_ID = "cover-image"
        const val XHTML_MEDIA_TYPE = "application/xhtml+xml"

        val CONTAINER_XML = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
<rootfiles>
<rootfile full-path="$ROOT/content.opf" media-type="application/oebps-package+xml"/>
</rootfiles>
</container>
"""

        fun imageType(data: ByteArray): Pair<String, String> {
            val header = String(data, 0, minOf(data.size, 1024), Charsets.ISO_8859_1)
            return when {
                header.startsWith("\u00FF\u00D8") -> "image/jpeg" to "jpg"
                header.startsWith("\u0089PNG") -> "image/png" to "png"
                header.startsWith("GIF8") -> "image/gif" to "gif"
                header.startsWith("RIFF") && header.startsWith("WEBP", 8) -> "image/webp" to "webp"
                header.contains("<svg") -> "image/svg+xml" to "svg"
                else -> "image/jpeg" to "jpg"
            }
        }

        fun String.escapeXml(): String = buildString(length) {
            for (c in this@escapeXml) when (c) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                '\t', '\n', '\r' -> append(c)
                // Drop control characters that are not allowed in XML documents.
                else -> if (c >= ' ' && c != '\uFFFE' && c != '\uFFFF') append(c)
            }
        }
    }
}
