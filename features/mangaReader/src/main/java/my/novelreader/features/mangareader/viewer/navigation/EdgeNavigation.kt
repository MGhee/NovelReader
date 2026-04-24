package my.novelreader.features.mangareader.viewer.navigation

import android.graphics.RectF
import my.novelreader.features.mangareader.viewer.ViewerNavigation

/**
 * Navigation using left and right edges of the screen.
 * Left: previous page, Right: next page, Center: menu
 */
class EdgeNavigation : ViewerNavigation() {
    override fun getRegions(width: Int, height: Int): List<Region> {
        val edgeWidth = width * 0.2f // 20% of screen width for edge zones
        return listOf(
            // Left edge - previous page
            Region(
                rect = RectF(0f, 0f, edgeWidth, height.toFloat()),
                action = NavigationAction.PREV_PAGE
            ),
            // Right edge - next page
            Region(
                rect = RectF(width - edgeWidth, 0f, width.toFloat(), height.toFloat()),
                action = NavigationAction.NEXT_PAGE
            ),
            // Center - menu
            Region(
                rect = RectF(edgeWidth, 0f, width - edgeWidth, height.toFloat()),
                action = NavigationAction.MENU
            ),
        )
    }
}
