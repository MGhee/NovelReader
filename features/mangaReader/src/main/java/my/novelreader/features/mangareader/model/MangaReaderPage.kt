package my.novelreader.features.mangareader.model

data class MangaReaderPage(
    val index: Int,
    val imageUrl: String,
) {
    enum class State {
        QUEUE, LOAD_PAGE, DOWNLOAD_IMAGE, CACHE_IMAGE, READY, ERROR
    }

    var state: State = State.QUEUE
    var progress: Int = 0
    var error: Exception? = null
}
