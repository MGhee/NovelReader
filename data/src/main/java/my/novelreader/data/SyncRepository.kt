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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
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
    val currentChapterUrl: String? = null,
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

    // Cache of server chapter counts to prevent pushing lower chapters
    private val serverChapterCounts = mutableMapOf<String, Int>()

    suspend fun fetchLibraryFromServer(serverUrl: String, apiKey: String = ""): Response<List<SyncBook>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$serverUrl/api/sync/library"
                Timber.d("SyncRepository: fetching library from $url")

                val requestBuilder = Request.Builder().url(url)
                if (apiKey.isNotBlank()) requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                val request = requestBuilder.build()

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
                        currentChapterUrl = obj["currentChapterUrl"]?.jsonPrimitive?.content,
                        updatedAt = obj["updatedAt"]?.jsonPrimitive?.content ?: "",
                    )
                }

                Response.Success(books)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch library from server: ${e.message}")
                Response.Error("Network error: ${e.message}", e)
            }
        }

    private suspend fun buildBookSyncData(book: Book): Map<String, Any>? =
        withContext(Dispatchers.IO) {
            try {
                val chapters = bookChaptersRepository.chapters(book.url)

                // Get current chapter position from lastReadChapter (which chapter is being read)
                val currentChapterPosition = if (!book.lastReadChapter.isNullOrEmpty()) {
                    // Find the chapter by URL and get its position
                    chapters.find { it.url == book.lastReadChapter }?.position ?: 0
                } else {
                    // Fallback: find max position of read chapters
                    chapters.filter { it.read }.maxOfOrNull { it.position } ?: 0
                }

                val chaptersData = chapters.map { chapter ->
                    mapOf(
                        "number" to (chapter.position + 1),
                        "title" to (chapter.title ?: ""),
                        "url" to chapter.url
                    )
                }

                mapOf(
                    "siteUrl" to book.url,
                    "title" to book.title,
                    "status" to if (book.completed) "COMPLETED" else "READING",
                    "currentChapter" to (currentChapterPosition + 1),
                    "totalChapters" to chapters.size,
                    "chapters" to chaptersData,
                    "coverUrl" to book.coverImageUrl,
                    "description" to book.description,
                    "updatedAt" to System.currentTimeMillis().toString(),
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to build sync data for book: ${book.url}")
                null
            }
        }

    suspend fun pushLibraryToServer(
        serverUrl: String,
        localBooks: List<Book>,
        apiKey: String = "",
        serverChapterCounts: Map<String, Int> = emptyMap()
    ): Response<SyncResponse> =
        withContext(Dispatchers.IO) {
            try {
                // Convert local books to sync format
                val syncBooks = localBooks.mapNotNull { book ->
                    buildBookSyncData(book)
                }

                val jsonPayload = buildJsonObject {
                    put("books", buildJsonArray {
                        for (book in syncBooks) {
                            val siteUrl = book["siteUrl"] as String
                            val localChapter = book["currentChapter"] as Int
                            val serverChapter = serverChapterCounts[siteUrl] ?: 0
                            add(buildJsonObject {
                                put("siteUrl", siteUrl)
                                put("title", book["title"] as String)
                                put("status", book["status"] as String)
                                put("currentChapter", maxOf(localChapter, serverChapter))
                                put("totalChapters", book["totalChapters"] as Int)
                                put("coverUrl", book["coverUrl"] as? String ?: "")
                                put("description", book["description"] as? String ?: "")
                                put("updatedAt", book["updatedAt"] as String)
                                put("chapters", buildJsonArray {
                                    @Suppress("UNCHECKED_CAST")
                                    val chapters = book["chapters"] as List<Map<String, Any>>
                                    for (chapter in chapters) {
                                        add(buildJsonObject {
                                            put("number", chapter["number"] as Int)
                                            put("title", chapter["title"] as String)
                                            put("url", chapter["url"] as String)
                                        })
                                    }
                                })
                            })
                        }
                    })
                }
                val jsonBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())

                val requestBuilder = Request.Builder().url("$serverUrl/api/sync/push").post(jsonBody)
                if (apiKey.isNotBlank()) requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                val request = requestBuilder.build()

                val client = (networkClient as? ScraperNetworkClient)?.client ?: return@withContext Response.Error("Invalid network client", Exception())
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Timber.e("SyncRepository: Push failed ${response.code}: $errorBody")
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

    suspend fun pushSingleBookToServer(
        serverUrl: String,
        bookUrl: String,
        apiKey: String = ""
    ): Response<SyncResponse> =
        withContext(Dispatchers.IO) {
            try {
                val book = libraryBooksRepository.get(bookUrl)
                if (book == null) {
                    Timber.w("SyncRepository: Book not found: $bookUrl")
                    return@withContext Response.Error("Book not found", Exception())
                }

                val syncBook = buildBookSyncData(book) ?: return@withContext Response.Error("Failed to build book data", Exception())

                val localChapter = syncBook["currentChapter"] as Int
                val cachedServerChapter = serverChapterCounts[bookUrl] ?: 0
                val chapterToSend = maxOf(localChapter, cachedServerChapter)

                // Update cache if local is ahead
                if (localChapter > cachedServerChapter) {
                    serverChapterCounts[bookUrl] = localChapter
                }

                val jsonPayload = buildJsonObject {
                    put("books", buildJsonArray {
                        add(buildJsonObject {
                            put("siteUrl", syncBook["siteUrl"] as String)
                            put("title", syncBook["title"] as String)
                            put("status", syncBook["status"] as String)
                            put("currentChapter", chapterToSend)
                            put("totalChapters", syncBook["totalChapters"] as Int)
                            put("coverUrl", syncBook["coverUrl"] as? String ?: "")
                            put("description", syncBook["description"] as? String ?: "")
                            put("updatedAt", syncBook["updatedAt"] as String)
                            put("chapters", buildJsonArray {
                                @Suppress("UNCHECKED_CAST")
                                val chapters = syncBook["chapters"] as List<Map<String, Any>>
                                for (chapter in chapters) {
                                    add(buildJsonObject {
                                        put("number", chapter["number"] as Int)
                                        put("title", chapter["title"] as String)
                                        put("url", chapter["url"] as String)
                                    })
                                }
                            })
                        })
                    })
                }
                val jsonBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())

                val requestBuilder = Request.Builder().url("$serverUrl/api/sync/push").post(jsonBody)
                if (apiKey.isNotBlank()) requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                val request = requestBuilder.build()

                val client = (networkClient as? ScraperNetworkClient)?.client ?: return@withContext Response.Error("Invalid network client", Exception())
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Timber.e("SyncRepository: Push failed, code=${response.code}")
                    return@withContext Response.Error("Server error: ${response.code}", Exception())
                }

                val body = response.body?.string() ?: ""
                val syncResponse = json.decodeFromString<SyncResponse>(body)
                Timber.d("SyncRepository: Single book pushed successfully: $bookUrl")

                Response.Success(syncResponse)
            } catch (e: Exception) {
                Timber.e(e, "Failed to push single book to server: $bookUrl")
                Response.Error("Network error: ${e.message}", e)
            }
        }

    /**
     * Full sync: fetch server state, compare with local, merge conflicts (max chapter wins).
     */
    suspend fun syncWithServer(serverUrl: String, apiKey: String = ""): Response<String> =
        withContext(Dispatchers.IO) {
            try {
                // 1. Get local library (only currently-reading books, exclude completed)
                val localBooks = libraryBooksRepository.getAll().filter { it.inLibrary && !it.completed }

                // 2. Fetch server state
                val serverBooksResponse = fetchLibraryFromServer(serverUrl, apiKey)
                if (serverBooksResponse !is Response.Success) {
                    return@withContext Response.Error("Failed to fetch server state", Exception())
                }
                val serverBooks = serverBooksResponse.data

                // Update cached server chapter counts
                for (book in serverBooks) {
                    serverChapterCounts[book.siteUrl] = book.currentChapter
                }

                // 3. Merge by siteUrl: pull updates from server for existing books
                var merged = 0
                var created = 0
                val updatedBookUrls = mutableSetOf<String>()

                for (serverBook in serverBooks) {
                    val localBook = localBooks.find { it.url == serverBook.siteUrl }

                    if (localBook != null) {
                        // Book exists on both sides: check if server is ahead
                        val localChapters = bookChaptersRepository.chapters(localBook.url)

                        // Determine local chapter number
                        val localChapterNumber = if (!localBook.lastReadChapter.isNullOrEmpty()) {
                            (localChapters.find { it.url == localBook.lastReadChapter }?.position ?: 0) + 1
                        } else {
                            (localChapters.filter { it.read }.maxOfOrNull { it.position } ?: -1) + 1
                        }

                        // Check if server is ahead
                        if (serverBook.currentChapter > localChapterNumber) {
                            // Pull: update local book to server's progress
                            val targetPosition = serverBook.currentChapter - 1  // Convert 1-indexed to 0-indexed
                            val targetChapter = localChapters.find { it.position == targetPosition }

                            if (targetChapter != null) {
                                // Target chapter exists locally: mark all chapters up to it as read
                                val chaptersToMark = localChapters
                                    .filter { it.position <= targetPosition && !it.read }
                                    .map { it.url }
                                if (chaptersToMark.isNotEmpty()) {
                                    bookChaptersRepository.setAsRead(chaptersToMark)
                                }
                                libraryBooksRepository.updateLastReadChapter(localBook.url, targetChapter.url)
                            } else if (localChapters.isNotEmpty()) {
                                // Server is further ahead than local chapter list
                                val chaptersToMark = localChapters.filter { !it.read }.map { it.url }
                                if (chaptersToMark.isNotEmpty()) {
                                    bookChaptersRepository.setAsRead(chaptersToMark)
                                }
                                val lastLocal = localChapters.maxByOrNull { it.position }
                                if (lastLocal != null) {
                                    libraryBooksRepository.updateLastReadChapter(localBook.url, lastLocal.url)
                                }
                            }

                            // Update lastReadEpochTimeMilli to server's timestamp to prevent push from regressing
                            try {
                                val serverTimestamp = java.time.Instant.parse(serverBook.updatedAt).toEpochMilli()
                                libraryBooksRepository.updateLastReadEpochTimeMilli(localBook.url, serverTimestamp)
                            } catch (e: Exception) {
                                Timber.w(e, "Sync: Failed to parse server timestamp for ${localBook.title}")
                            }

                            updatedBookUrls.add(localBook.url)
                            Timber.d("Sync: Updated ${localBook.title} from ch $localChapterNumber to ch ${serverBook.currentChapter}")
                        }

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

                // 4. Re-fetch local books to get updated timestamps from pull
                val localBooksForPush = libraryBooksRepository.getAll()
                    .filter { it.inLibrary && !it.completed }

                // 5. Push local books to server (using cached server chapters to prevent regression)
                val pushResponse = pushLibraryToServer(serverUrl, localBooksForPush, apiKey, serverChapterCounts)
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
