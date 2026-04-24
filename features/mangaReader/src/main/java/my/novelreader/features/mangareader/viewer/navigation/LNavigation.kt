package my.novelreader.features.mangareader.viewer.navigation

import android.graphics.RectF
import my.novelreader.features.mangareader.viewer.ViewerNavigation

/**
 * L-shaped navigation: corners and sides.
 */
class LNavigation : ViewerNavigation() {
    override fun getRegions(width: Int, height: Int): List<Region> {
        val cornerSize = width * 0.33f
        return listOf(
            // Top-left corner - previous page
            Region(
                rect = RectF(0f, 0f, cornerSize, cornerSize),
                action = NavigationAction.PREV_PAGE
            ),
            // Top-right corner - next page
            Region(
                rect = RectF(width - cornerSize, 0f, width.toFloat(), cornerSize),
                action = NavigationAction.NEXT_PAGE
            ),
            // Bottom-left corner - previous chapter
            Region(
                rect = RectF(0f, height - cornerSize, cornerSize, height.toFloat()),
                action = NavigationAction.PREV_CHAPTER
            ),
            // Bottom-right corner - next chapter
            Region(
                rect = RectF(width - cornerSize, height - cornerSize, width.toFloat(), height.toFloat()),
                action = NavigationAction.NEXT_CHAPTER
            ),
            // Center area - menu
            Region(
                rect = RectF(cornerSize, cornerSize, width - cornerSize, height - cornerSize),
                action = NavigationAction.MENU
            ),
        )
    }
}
