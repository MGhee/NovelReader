package my.novelreader.interactor

import my.novelreader.core.domain.LibraryCategory

interface WorkersInteractions {
    fun checkForLibraryUpdates(libraryCategory: LibraryCategory)
    fun syncWithServer(serverUrl: String, authToken: String = "")
    fun downloadAllBookChapters(bookUrl: String)
}