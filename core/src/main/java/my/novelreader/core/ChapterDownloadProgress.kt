package my.novelreader.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.URLDecoder

/**
 * In-memory bus for byte-level chapter-download progress, keyed by chapter URL.
 *
 * Sources that pull large per-chapter payloads (e.g. multi-megabyte EPUBs) call [update]
 * as bytes arrive and [clear] when the download completes or fails. The chapter list
 * subscribes to [flow] so users can see how far along an in-flight download is and judge
 * how long until they can open the chapter — the existing batch-progress signal from
 * `ChapterDownloadWorker` is just (chapters done / chapters total) and doesn't help when a
 * single chapter is itself a 20+ MB file.
 *
 * Held as an `object` rather than a Hilt-injected singleton on purpose: the producer side
 * (a scraper source running on a worker dispatcher) and the consumer side (a Compose
 * composable in the chapters list) live in different modules with different DI graphs, and
 * threading a Hilt singleton through both is more wiring than a global state flow buys.
 */
object ChapterDownloadProgress {
    private val _flow = MutableStateFlow<Map<String, Float>>(emptyMap())
    val flow: StateFlow<Map<String, Float>> = _flow.asStateFlow()

    fun update(chapterUrl: String, fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        val aliases = aliasesFor(chapterUrl)
        _flow.update { current ->
            buildMap(current.size + aliases.size) {
                putAll(current)
                aliases.forEach { put(it, clamped) }
            }
        }
    }

    fun clear(chapterUrl: String) {
        val aliases = aliasesFor(chapterUrl)
        _flow.update { current -> current.filterKeys { it !in aliases } }
    }

    private fun aliasesFor(chapterUrl: String): Set<String> {
        val raw = chapterUrl.trim()
        if (raw.isBlank()) return emptySet()

        val decoded = runCatching { URLDecoder.decode(raw, "UTF-8") }
            .getOrDefault(raw)

        return buildSet(4) {
            add(raw)
            add(decoded)
            add(raw.replace(" ", "%20"))
            add(decoded.replace(" ", "%20"))
        }
    }
}
