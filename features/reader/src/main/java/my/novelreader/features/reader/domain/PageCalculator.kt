package my.novelreader.features.reader.domain

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Represents one "page" of content for horizontal mode.
 * Each page contains multiple items stacked vertically.
 */
internal data class PageData(
    val items: List<ReaderItem>,
    val firstItemIndex: Int,  // index into the global items list (excluding padding)
)

/**
 * Calculates which items belong to which page based on available screen height.
 */
internal class PageCalculator(
    private val ctx: Context,
    private val binder: ReaderItemBinder,
) {
    // Cache measured heights to avoid re-inflating views on subsequent calculatePages calls
    private val heightCache = HashMap<ReaderItem, Int>(256)

    /**
     * Groups items into pages based on available height.
     * Each page is filled as much as possible (items <= availableHeight),
     * and items that don't fit start a new page.
     */
    internal fun calculatePages(items: List<ReaderItem>, availableHeight: Int): List<PageData> {
        if (items.isEmpty() || availableHeight <= 0) return emptyList()

        val pages = mutableListOf<PageData>()
        val currentPageItems = mutableListOf<ReaderItem>()
        var currentPageHeight = 0
        var firstItemIndexOnPage = 0

        // Small fixed bottom padding for breathing room (8dp)
        val bottomPadding = (8 * ctx.resources.displayMetrics.density).toInt()
        val maxPageHeight = availableHeight - bottomPadding

        // Track chapter boundaries
        var lastChapterIndex: Int? = null

        // Measurement container for off-screen layout
        val measureContainer = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                View.MeasureSpec.UNSPECIFIED,
                View.MeasureSpec.UNSPECIFIED
            )
        }

        for ((index, item) in items.withIndex()) {
            // Skip padding items in horizontal mode (they're for vertical scroll spacing)
            if (item is ReaderItem.Padding) continue

            val viewType = getItemViewType(item)
            val measuredHeight = heightCache.getOrPut(item) {
                measureItemHeight(measureContainer, viewType, item)
            }

            // Force page break at chapter boundary
            val isChapterBoundary = lastChapterIndex != null && item.chapterIndex != lastChapterIndex
            if (isChapterBoundary && currentPageItems.isNotEmpty()) {
                pages.add(PageData(currentPageItems.toList(), firstItemIndexOnPage))
                currentPageItems.clear()
                currentPageHeight = 0
            }

            // If this item doesn't fit on the current page, move it to the next page
            if (currentPageItems.isNotEmpty() && currentPageHeight + measuredHeight > maxPageHeight) {
                pages.add(PageData(currentPageItems.toList(), firstItemIndexOnPage))
                currentPageItems.clear()
                currentPageHeight = 0
            }

            if (currentPageItems.isEmpty()) {
                firstItemIndexOnPage = index
            }
            currentPageItems.add(item)
            currentPageHeight += measuredHeight

            // Update chapter tracking
            lastChapterIndex = item.chapterIndex
        }

        // Don't forget the last page
        if (currentPageItems.isNotEmpty()) {
            pages.add(PageData(currentPageItems.toList(), firstItemIndexOnPage))
        }

        return pages
    }

    /**
     * Measures the height of a single item when laid out with current styling.
     */
    private fun measureItemHeight(container: ViewGroup, viewType: Int, item: ReaderItem): Int {
        val holder = binder.inflateAndBindViewHolder(container, viewType, item)
        val view = holder.itemView

        val widthSpec = View.MeasureSpec.makeMeasureSpec(
            container.resources.displayMetrics.widthPixels,
            View.MeasureSpec.EXACTLY
        )
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        view.measure(widthSpec, heightSpec)
        return view.measuredHeight
    }

    /**
     * Clears the height cache. Call when text styling changes (font size, margins, etc.)
     */
    fun clearCache() {
        heightCache.clear()
    }

    /**
     * Returns the view type for a given item (matches ReaderItemAdapter logic).
     */
    private fun getItemViewType(item: ReaderItem): Int = when (item) {
        is ReaderItem.Body -> 0
        is ReaderItem.Image -> 1
        is ReaderItem.BookEnd -> 2
        is ReaderItem.BookStart -> 3
        is ReaderItem.Divider -> 4
        is ReaderItem.Error -> 5
        is ReaderItem.Padding -> 6
        is ReaderItem.Progressbar -> 7
        is ReaderItem.Title -> 8
        is ReaderItem.Translating -> 9
        is ReaderItem.GoogleTranslateAttribution -> 10
        is ReaderItem.TranslateAttribution -> 11
    }
}
