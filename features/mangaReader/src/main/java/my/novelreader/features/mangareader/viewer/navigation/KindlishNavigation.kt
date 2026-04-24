package my.novelreader.features.mangareader.viewer.navigation

import android.graphics.RectF
import my.novelreader.features.mangareader.viewer.ViewerNavigation

/**
 * Kindle-style navigation: edges and center.
 * Left/right edges for page navigation, center for menu.
 */
class KindlishNavigation : ViewerNavigation() {
    override fun getRegions(width: Int, height: Int): List<Region> {
        val edgeWidth = width * 0.15f
        val edgeHeight = height * 0.15f
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
            // Top center - menu
            Region(
                rect = RectF(edgeWidth, 0f, width - edgeWidth, edgeHeight),
                action = NavigationAction.MENU
            ),
            // Bottom center - menu
            Region(
                rect = RectF(edgeWidth, height - edgeHeight, width - edgeWidth, height.toFloat()),
                action = NavigationAction.MENU
            ),
        )
    }
}
