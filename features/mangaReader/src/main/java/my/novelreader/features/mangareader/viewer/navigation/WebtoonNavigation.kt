package my.novelreader.features.mangareader.viewer.navigation

import android.graphics.RectF
import my.novelreader.features.mangareader.viewer.ViewerNavigation

/**
 * Webtoon-mode navigation: entire screen triggers MENU (chrome toggle).
 * Pagination is handled via scrolling, not tap regions, so all taps toggle the UI.
 */
class WebtoonNavigation : ViewerNavigation() {
    override fun getRegions(width: Int, height: Int): List<Region> {
        return listOf(
            Region(
                rect = RectF(0f, 0f, width.toFloat(), height.toFloat()),
                action = NavigationAction.MENU
            ),
        )
    }
}
