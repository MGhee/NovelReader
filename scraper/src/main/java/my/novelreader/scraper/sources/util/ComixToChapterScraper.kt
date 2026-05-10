package my.novelreader.scraper.sources.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Scrapes the chapter list from a comix.to title page via a hidden WebView.
 *
 * Why a WebView: comix.to is a Next.js client-rendered SPA whose chapter list is fetched
 * by JS via an API protected by a session-bound CSRF check we can't replicate from a plain
 * HTTP client. By rendering the title page in WebView we let the site's own JS make the
 * API call (with its real session cookies) and read the resulting DOM directly.
 *
 * The list is paginated client-side (~20 chapters per page). We extract page 1, read the
 * "of N" total, then walk pages 2..N by URL-hash navigation so we collect every chapter.
 */
internal class ComixToChapterScraper(private val context: Context) {

    data class DiscoveredChapterImages(
        val firstImageUrl: String,
        val totalPages: Int? = null,
    )

    data class ScrapedChapter(
        val url: String,
        val rawTitle: String,
        val groupName: String,
    )

    /**
     * Discovers the first image URL on a chapter reader page via a hidden WebView. The
     * URLs follow a strict pattern (`https://{host}/ii/{key}/01.webp`, `02.webp`, …) so
     * the caller only needs the first one to construct the rest by simple string substitution.
     */
    suspend fun discoverChapterImages(
        chapterUrl: String,
        loadTimeoutMs: Long = 45_000L,
        pollIntervalMs: Long = 500L,
    ): DiscoveredChapterImages? = withContext(Dispatchers.Main) {
        val webView = createWebView() ?: return@withContext null
        try {
            Log.i(TAG, "loading chapter $chapterUrl to discover first image")
            awaitPageFinishedOrTimeout(webView, chapterUrl, loadTimeoutMs)

            withTimeoutOrNull(loadTimeoutMs) {
                while (true) {
                    val urls = extractImageUrls(webView)
                    val first = urls.firstOrNull()
                    if (first != null) {
                        val totalPages = extractTotalChapterPages(webView)
                        Log.i(TAG, "first image: $first, total pages: $totalPages")
                        return@withTimeoutOrNull DiscoveredChapterImages(
                            firstImageUrl = first,
                            totalPages = totalPages,
                        )
                    }
                    delay(pollIntervalMs)
                }
                @Suppress("UNREACHABLE_CODE") null
            }
        } finally {
            runCatching {
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
            }
        }
    }

    suspend fun discoverChapterApiUrl(
        bookUrl: String,
        loadTimeoutMs: Long = 45_000L,
        pollIntervalMs: Long = 250L,
    ): String? = withContext(Dispatchers.Main) {
        val webView = createWebView() ?: return@withContext null
        try {
            Log.i(TAG, "loading $bookUrl to discover chapters api")
            awaitPageFinishedOrTimeout(webView, bookUrl, loadTimeoutMs)

            withTimeoutOrNull(loadTimeoutMs) {
                while (true) {
                    val apiUrl = evaluateJavascript(webView, DISCOVER_CHAPTERS_API_JS)
                        ?.let(::unwrap)
                        ?.takeIf { it.isNotBlank() }
                    if (apiUrl != null) {
                        Log.i(TAG, "chapters api: $apiUrl")
                        return@withTimeoutOrNull apiUrl
                    }
                    delay(pollIntervalMs)
                }
                @Suppress("UNREACHABLE_CODE") null
            }
        } finally {
            runCatching {
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
            }
        }
    }

    private suspend fun extractImageUrls(webView: WebView): List<String> {
        val raw = evaluateJavascript(webView, EXTRACT_IMAGES_JS) ?: return emptyList()
        val cleaned = unwrap(raw) ?: return emptyList()
        if (cleaned.isBlank() || cleaned == "[]") return emptyList()
        return parseStringArray(cleaned)
    }

    private suspend fun extractTotalChapterPages(webView: WebView): Int? {
        val raw = evaluateJavascript(webView, TOTAL_CHAPTER_PAGES_JS) ?: return null
        val cleaned = unwrap(raw) ?: return null
        return cleaned.toIntOrNull()?.takeIf { it > 0 }
    }

    private fun parseStringArray(json: String): List<String> {
        val out = mutableListOf<String>()
        val itemRe = Regex(""""((?:[^"\\]|\\.)*)"""")
        for (m in itemRe.findAll(json)) {
            val s = unescape(m.groupValues[1])
            if (s.isNotBlank()) out += s
        }
        return out
    }

    suspend fun extract(
        bookUrl: String,
        loadTimeoutMs: Long = 45_000L,
        pollIntervalMs: Long = 500L,
    ): List<ScrapedChapter> = withContext(Dispatchers.Main) {
        val webView = createWebView() ?: return@withContext emptyList()
        try {
            Log.i(TAG, "loading $bookUrl")
            awaitPageFinishedOrTimeout(webView, bookUrl, loadTimeoutMs)

            val firstPage = withTimeoutOrNull(loadTimeoutMs) {
                pollForChapters(webView, pollIntervalMs)
            }
            if (firstPage.isNullOrEmpty()) {
                Log.w(TAG, "no chapters appeared in DOM within timeout")
                return@withContext emptyList()
            }

            val totalPages = withTimeoutOrNull(5_000L) { readTotalPages(webView) } ?: 1
            Log.i(TAG, "page 1 has ${firstPage.size} chapters; total pages=$totalPages")
            val all = firstPage.toMutableList()

            for (page in 2..totalPages) {
                val nextPage = navigateAndExtract(webView, bookUrl, page, pollIntervalMs, loadTimeoutMs)
                if (nextPage.isEmpty()) {
                    Log.w(TAG, "page $page returned 0 chapters — stopping")
                    break
                }
                Log.i(TAG, "page $page has ${nextPage.size} chapters")
                all += nextPage
            }
            Log.i(TAG, "total scraped chapters=${all.size}")
            all
        } finally {
            runCatching {
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
            }
        }
    }

    private suspend fun navigateAndExtract(
        webView: WebView,
        bookUrl: String,
        page: Int,
        pollIntervalMs: Long,
        loadTimeoutMs: Long,
    ): List<ScrapedChapter> {
        val pageUrl = withPage(bookUrl, page)
        awaitPageFinishedOrTimeout(webView, pageUrl, loadTimeoutMs)
        return withTimeoutOrNull(15_000L) {
            pollForChapters(webView, pollIntervalMs)
        } ?: emptyList()
    }

    private suspend fun pollForChapters(webView: WebView, intervalMs: Long): List<ScrapedChapter> {
        while (true) {
            val items = extractChapters(webView)
            if (items.isNotEmpty()) return items
            delay(intervalMs)
        }
    }

    private suspend fun extractChapters(webView: WebView): List<ScrapedChapter> {
        val raw = evaluateJavascript(webView, EXTRACT_CHAPTERS_JS) ?: return emptyList()
        val cleaned = unwrap(raw) ?: return emptyList()
        if (cleaned.isBlank() || cleaned == "[]") return emptyList()
        return parseChaptersJson(cleaned)
    }

    private suspend fun readTotalPages(webView: WebView): Int {
        val raw = evaluateJavascript(webView, TOTAL_PAGES_JS) ?: return 1
        val cleaned = unwrap(raw) ?: return 1
        return cleaned.toIntOrNull() ?: 1
    }

    /** Tiny hand-rolled JSON-array parser — we know the exact shape and don't need a dep. */
    private fun parseChaptersJson(json: String): List<ScrapedChapter> {
        val out = mutableListOf<ScrapedChapter>()
        // The JS produces strict JSON like: [{"u":"...","t":"...","g":"..."}, ...]
        val itemRe = Regex("""\{\s*"u":"((?:[^"\\]|\\.)*)"\s*,\s*"t":"((?:[^"\\]|\\.)*)"\s*,\s*"g":"((?:[^"\\]|\\.)*)"\s*\}""")
        for (m in itemRe.findAll(json)) {
            val url = unescape(m.groupValues[1])
            val title = unescape(m.groupValues[2])
            val group = unescape(m.groupValues[3])
            if (url.isNotBlank()) out += ScrapedChapter(url, title, group)
        }
        return out
    }

    private fun unescape(s: String): String =
        s.replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\/", "/")
            .replace("\\n", "\n")

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView? = runCatching {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadsImagesAutomatically = true
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        }
    }.getOrNull()

    private suspend fun awaitPageFinishedOrTimeout(webView: WebView, url: String, timeoutMs: Long) {
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Unit> { cont: CancellableContinuation<Unit> ->
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, finishedUrl: String?) {
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
                webView.loadUrl(url)
            }
        }
    }

    private suspend fun evaluateJavascript(webView: WebView, script: String): String? =
        suspendCancellableCoroutine { cont: CancellableContinuation<String?> ->
            webView.evaluateJavascript(script) { result ->
                if (cont.isActive) cont.resume(result)
            }
        }

    private fun unwrap(raw: String?): String? {
        if (raw.isNullOrBlank() || raw == "null") return null
        var s = raw
        if (s.length >= 2 && s.startsWith('"') && s.endsWith('"')) {
            s = s.substring(1, s.length - 1)
        }
        return s.replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\/", "/")
            .replace("\\u002F", "/")
            .replace("\\n", "\n")
    }

    private fun withPage(bookUrl: String, page: Int): String {
        val cleanUrl = bookUrl.substringBefore('#')
        val withoutPage = cleanUrl
            .replace(Regex("([?&])page=\\d+&?"), "$1")
            .replace(Regex("[?&]$"), "")
        return if ('?' in withoutPage) "$withoutPage&page=$page" else "$withoutPage?page=$page"
    }

    private companion object {
        const val TAG = "ComixToScraper"

        val EXTRACT_CHAPTERS_JS = """
            JSON.stringify(
                Array.from(document.querySelectorAll('a[href*="/title/"][href*="-chapter-"]'))
                    .map(function(a) {
                        var href = a.getAttribute('href') || '';
                        var title = (a.textContent || '').trim();
                        if (!href || !title) return null;

                        var group = '';
                        var current = a.parentElement;
                        for (var i = 0; i < 6 && current && !group; i++) {
                            var groupAnchor = current.querySelector('a[href*="/groups/"]');
                            if (groupAnchor) group = (groupAnchor.textContent || '').trim();
                            current = current.parentElement;
                        }

                        var sibling = a.nextElementSibling;
                        for (var j = 0; j < 4 && sibling && !group; j++) {
                            if ((sibling.getAttribute('href') || '').indexOf('/groups/') !== -1) {
                                group = (sibling.textContent || '').trim();
                                break;
                            }
                            sibling = sibling.nextElementSibling;
                        }

                        return { u: href, t: title, g: group };
                    })
                    .filter(Boolean)
            )
        """.trimIndent()

        val TOTAL_PAGES_JS = """
            (function() {
                var text = (document.body && document.body.innerText) || '';
                var m = text.match(/Showing\s+\d+\s+to\s+\d+\s+of\s+([\d,]+)\s+items/i);
                if (!m) return '1';
                var total = parseInt(m[1].replace(/,/g, ''), 10);
                if (!total || total < 1) return '1';
                return String(Math.max(1, Math.ceil(total / 20)));
            })()
        """.trimIndent()

        val DISCOVER_CHAPTERS_API_JS = """
            (function() {
                var entries = performance.getEntriesByType('resource');
                for (var i = entries.length - 1; i >= 0; i--) {
                    var name = entries[i] && entries[i].name ? entries[i].name : '';
                    if ((name.indexOf('/api/v1/manga/') !== -1 || name.indexOf('/api/v2/manga/') !== -1) && name.indexOf('/chapters?') !== -1) {
                        return name;
                    }
                }
                return '';
            })()
        """.trimIndent()

        val TOTAL_CHAPTER_PAGES_JS = """
            (function() {
                var counters = document.querySelectorAll('.progress-line.left > div');
                if (counters.length >= 2) {
                    var total = (counters[counters.length - 1].textContent || '').trim();
                    if (/^\d+$/.test(total)) return total;
                }

                var text = (document.body && document.body.innerText) || '';
                var lines = text.split(/\n+/).map(function(line) { return line.trim(); }).filter(Boolean);
                for (var i = 0; i < lines.length - 1; i++) {
                    if (/^\d+$/.test(lines[i]) && /^\d+$/.test(lines[i + 1])) {
                        var current = parseInt(lines[i], 10);
                        var totalPages = parseInt(lines[i + 1], 10);
                        if (current >= 1 && totalPages >= current) return String(totalPages);
                    }
                }
                return '';
            })()
        """.trimIndent()

        val EXTRACT_IMAGES_JS = """
            JSON.stringify(Array.from(document.querySelectorAll('img'))
                .map(function(img) {
                    return img.getAttribute('src')
                        || img.getAttribute('data-src')
                        || img.getAttribute('data-original')
                        || img.getAttribute('data-lazy-src')
                        || '';
                })
                .filter(function(src) {
                    if (!src) return false;
                    var clean = src.split('?')[0].split('#')[0].toLowerCase();
                    if (!(clean.endsWith('.webp') || clean.endsWith('.jpg') ||
                          clean.endsWith('.jpeg') || clean.endsWith('.png') ||
                          clean.endsWith('.avif'))) return false;
                    if (src.indexOf('static.comix.to') !== -1) return false; // covers / thumbs
                    if (src.indexOf('static.anigo.to') !== -1) return false; // ads
                    return true;
                }))
        """.trimIndent()
    }
}
