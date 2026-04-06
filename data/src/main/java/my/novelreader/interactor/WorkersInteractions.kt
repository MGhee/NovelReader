package my.novelreader.interactor

import kotlinx.coroutines.flow.Flow
import my.novelreader.core.domain.LibraryCategory

interface WorkersInteractions {
    fun checkForLibraryUpdates(libraryCategory: LibraryCategory)
    fun syncWithServer(serverUrl: String, authToken: String = "")
    fun downloadAllBookChapters(bookUrl: String)
    fun cancelDownload(bookUrl: String)
    fun observeDownloadProgress(bookUrl: String): Flow<Pair<Int, Int>?>
    fun observeActiveDownloads(): Flow<Map<String, Pair<Int, Int>>>
}