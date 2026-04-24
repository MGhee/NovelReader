package my.novelreader.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaImagePrefetcher @Inject constructor(
    @ApplicationContext context: Context,
    private val client: OkHttpClient,
) {
    private val cacheDir = File(context.cacheDir, "manga_pages_cache").apply { mkdirs() }
    private var prefetchJob: kotlinx.coroutines.Job? = null

    fun prefetch(imageUrls: List<String>, scope: CoroutineScope) {
        prefetchJob?.cancel()
        prefetchJob = scope.launch(Dispatchers.IO) {
            val semaphore = Semaphore(4)
            imageUrls.map { url ->
                async {
                    semaphore.withPermit {
                        downloadIfMissing(url)
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun prefetchAndAwait(imageUrls: List<String>): Boolean =
        withContext(Dispatchers.IO) {
            val semaphore = Semaphore(4)
            imageUrls.map { url ->
                async {
                    semaphore.withPermit {
                        downloadIfMissing(url)
                    }
                }
            }.awaitAll().all { it }
        }

    fun cancel() {
        prefetchJob?.cancel()
        prefetchJob = null
    }

    private fun downloadIfMissing(imageUrl: String): Boolean {
        val extension = imageUrl.fileExtensionOrDefault()
        val cachedFile = File(cacheDir, "${imageUrl.sha256()}.$extension")

        if (cachedFile.exists() && cachedFile.length() > 0) {
            return true
        }

        val referer = try {
            val uri = java.net.URI(imageUrl)
            "${uri.scheme}://${uri.host}"
        } catch (_: Exception) {
            ""
        }

        val request = Request.Builder()
            .url(imageUrl)
            .header("Referer", referer)
            .build()

        repeat(3) { attempt ->
            val ok = runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use false

                    val tempFile = File.createTempFile("prefetch_", ".tmp", cacheDir)
                    response.body?.byteStream()?.use { input ->
                        tempFile.outputStream().buffered().use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (tempFile.length() == 0L) {
                        tempFile.delete()
                        return@use false
                    }

                    if (!tempFile.renameTo(cachedFile)) {
                        tempFile.copyTo(cachedFile, overwrite = true)
                        tempFile.delete()
                    }
                    true
                }
            }.getOrDefault(false)

            if (ok) return true
            if (attempt < 2) Thread.sleep(300L * (attempt + 1))
        }
        return false
    }

    private fun String.sha256(): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun String.fileExtensionOrDefault(): String {
        val clean = substringBefore('?').substringBefore('#')
        val ext = clean.substringAfterLast('.', "img").lowercase()
        return if (ext.length in 2..5 && ext.all { it.isLetterOrDigit() }) ext else "img"
    }
}
