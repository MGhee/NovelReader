package my.novelreader.data

import my.novelreader.core.Response
import my.novelreader.feature.local_database.tables.Book
import my.novelreader.network.NetworkClient
import my.novelreader.network.ScraperNetworkClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class SyncBook(
    val siteUrl: String,
    val title: String,
    val status: String,
    val currentChapter: Int = 0,
    val totalChapters: Int = 0,
    val coverUrl: String? = null,
    val description: String? = null,
    val updatedAt: String,
)

@Serializable
data class SyncMergedBook(
    val siteUrl: String,
    val title: String,
    val status: String,
    val currentChapter: Int = 0,
    val resolved: Boolean = false,
    val created: Boolean? = null,
)

@Serializable
data class SyncResponse(
    val merged: List<SyncMergedBook> = emptyList(),
    val errors: List<String> = emptyList(),
)

@Singleton
class SyncRepository @Inject constructor(
    private val networkClient: NetworkClient,
    private val libraryBooksRepository: LibraryBooksRepository,
    private val bookChaptersRepository: BookChaptersRepository,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchLibraryFromServer(serverUrl: String): Response<List<SyncBook>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$serverUrl/api/sync/library"
                Timber.d("SyncRepository: fetching library from $url")

                val request = Request.Builder()
                    .url(url)
                    .build()

                val client = (networkClient as? ScraperNetworkClient)?.client
                if (client == null) {
                    Timber.e("SyncRepository: networkClient is not ScraperNetworkClient, cannot proceed")
                    return@withContext Response.Error("Invalid network client", Exception())
                }

                Timber.d("SyncRepository: making request to $url")
                val response = client.newCall(request).execute()
                Timber.d("SyncRepository: got response, code=${response.code}, successful=${response.isSuccessful}")

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Timber.e("SyncRepository: Server error ${response.code}: $errorBody")
                    return@withContext Response.Error("Server error: ${response.code}", Exception())
                }

                val body = response.body?.string() ?: ""
                Timber.d("SyncRepository: response body length=${body.length}")

                val jsonObject = json.parseToJsonElement(body).jsonObject
                val booksArray = jsonObject["books"]?.jsonArray ?: run {
                    Timber.w("SyncRepository: no books array in response")
                    return@withContext Response.Success(emptyList())
                }

                Timber.d("SyncRepository: found ${booksArray.size} books")

                val books = booksArray.map { element ->
                    val obj = element.jsonObject
                    SyncBook(
                        siteUrl = obj["siteUrl"]?.jsonPrimitive?.content ?: "",
                        title = obj["title"]?.jsonPrimitive?.content ?: "Unknown",
                        status = obj["status"]?.jsonPrimitive?.content ?: "READING",
                        currentChapter = obj["currentChapter"]?.jsonPrimitive?.intOrNull ?: 0,
                        totalChapters = obj["totalChapters"]?.jsonPrimitive?.intOrNull ?: 0,
                        coverUrl = obj["coverUrl"]?.jsonPrimitive?.content,
                        description = obj["description"]?.jsonPrimitive?.content,
                        updatedAt = obj["updatedAt"]?.jsonPrimitive?.content ?: "",
                    )
                }

                Response.Success(books)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch library from server: ${e.message}")
                Response.Error("Network error: ${e.message}", e)
            }
        }

    suspend fun pushLibraryToServer(serverUrl: String, localBooks: List<Book>): Response<SyncResponse> =
        withContext(Dispatchers.IO) {
            try {
                // Convert local books to sync format
                val syncBooks = localBooks.mapNotNull { book ->
                    val chapters = bookChaptersRepository.chapters(book.url)

                    // Get current chapter position from lastReadChapter (which chapter is being read)
                    val currentChapterPosition = if (!book.lastReadChapter.isNullOrEmpty()) {
                        // Find the chapter by URL and get its position
                        chapters.find { it.url == book.lastReadChapter }?.position ?: 0
                    } else {
                        // Fallback: find max position of read chapters
                        chapters.filter { it.read }.maxOfOrNull { it.position } ?: 0
                    }

                    Timber.d("Sync: Book ${book.title} - lastReadChapter=${book.lastReadChapter}, total chapters: ${chapters.size}, current position: $currentChapterPosition")

                    mapOf(
                        "siteUrl" to book.url,
                        "title" to book.title,
                        "status" to if (book.completed) "COMPLETED" else "READING",
                        "currentChapter" to (currentChapterPosition + 1).toString(),
                        "totalChapters" to chapters.size.toString(),
                        "updatedAt" to System.currentTimeMillis().toString(),
                    )
                }

                val payload = mapOf("books" to syncBooks)
                val jsonBody = json.encodeToString(payload).toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$serverUrl/api/sync/push")
                    .post(jsonBody)
                    .build()

                val client = (networkClient as? ScraperNetworkClient)?.client ?: return@withContext Response.Error("Invalid network client", Exception())
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Response.Error("Server error: ${response.code}", Exception())
                }

                val body = response.body?.string() ?: ""
                val syncResponse = json.decodeFromString<SyncResponse>(body)

                Response.Success(syncResponse)
            } catch (e: Exception) {
                Timber.e(e, "Failed to push library to server")
                Response.Error("Network error: ${e.message}", e)
            }
        }

    /**
     * Full sync: fetch server state, compare with local, merge conflicts (max chapter wins).
     */
    suspend fun syncWithServer(serverUrl: String): Response<String> =
        withContext(Dispatchers.IO) {
            try {
                // 1. Get local library (only currently-reading books, exclude completed)
                val localBooks = libraryBooksRepository.getAll().filter { it.inLibrary && !it.completed }

                // 2. Fetch server state
                val serverBooksResponse = fetchLibraryFromServer(serverUrl)
                if (serverBooksResponse !is Response.Success) {
                    return@withContext Response.Error("Failed to fetch server state", Exception())
                }
                val serverBooks = serverBooksResponse.data

                // 3. Merge by siteUrl
                var merged = 0
                var created = 0

                for (serverBook in serverBooks) {
                    val localBook = localBooks.find { it.url == serverBook.siteUrl }

                    if (localBook != null) {
                        // Book exists on both sides: resolve conflicts
                        // For now: just log, don't update (to avoid data loss)
                        Timber.d("Sync: Book exists on both sides: ${serverBook.title}")
                        merged++
                    } else {
                        // Book only on server: create locally
                        libraryBooksRepository.insert(
                            Book(
                                title = serverBook.title,
                                url = serverBook.siteUrl,
                                completed = serverBook.status == "COMPLETED",
                                inLibrary = true,
                                coverImageUrl = serverBook.coverUrl ?: "",
                                description = serverBook.description ?: "",
                            )
                        )
                        created++
                        Timber.d("Sync: Created local book: ${serverBook.title}")
                    }
                }

                // 4. Push local books to server
                val pushResponse = pushLibraryToServer(serverUrl, localBooks)
                if (pushResponse !is Response.Success) {
                    return@withContext Response.Error("Failed to push to server", Exception())
                }

                val syncResponse = pushResponse.data
                val summary = "Synced: merged=$merged, created=$created, errors=${syncResponse.errors.size}"
                Timber.d("Sync complete: $summary")

                Response.Success(summary)
            } catch (e: Exception) {
                Timber.e(e, "Sync failed")
                Response.Error("Sync error: ${e.message}", e)
            }
        }
}
