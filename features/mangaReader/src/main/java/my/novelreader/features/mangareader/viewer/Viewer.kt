package my.novelreader.features.mangareader.viewer

import android.view.View
import my.novelreader.features.mangareader.model.ViewerChapters

/**
 * Base interface for manga readers (Webtoon, Pager, etc.)
 */
interface Viewer {
    /**
     * The root View that displays manga pages.
     */
    val view: View

    /**
     * Set the chapters to display (current and adjacent).
     */
    fun setChapters(chapters: ViewerChapters)

    /**
     * Move to a specific page index in the current chapter.
     */
    fun moveToPage(index: Int)

    /**
     * Get the current page index.
     */
    fun getCurrentPage(): Int

    /**
     * Clean up resources.
     */
    fun destroy()
}
