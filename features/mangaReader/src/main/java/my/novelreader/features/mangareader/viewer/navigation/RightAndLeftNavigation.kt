package my.novelreader.features.mangareader.viewer.navigation

import android.graphics.RectF
import my.novelreader.features.mangareader.viewer.ViewerNavigation

/**
 * Simple left/right halves navigation.
 * Left half: previous, Right half: next, both include menu tap detection in center strip.
 */
class RightAndLeftNavigation : ViewerNavigation() {
    override fun getRegions(width: Int, height: Int): List<Region> {
        val mid = width / 2f
        val centerStripWidth = width * 0.1f
        return listOf(
            // Left half - previous page
            Region(
                rect = RectF(0f, 0f, mid - centerStripWidth / 2, height.toFloat()),
                action = NavigationAction.PREV_PAGE
            ),
            // Center strip - menu
            Region(
                rect = RectF(mid - centerStripWidth / 2, 0f, mid + centerStripWidth / 2, height.toFloat()),
                action = NavigationAction.MENU
            ),
            // Right half - next page
            Region(
                rect = RectF(mid + centerStripWidth / 2, 0f, width.toFloat(), height.toFloat()),
                action = NavigationAction.NEXT_PAGE
            ),
        )
    }
}
