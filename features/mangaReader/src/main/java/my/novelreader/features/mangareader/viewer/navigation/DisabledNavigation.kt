package my.novelreader.features.mangareader.viewer.navigation

import android.graphics.RectF
import my.novelreader.features.mangareader.viewer.ViewerNavigation

/**
 * No tap-based navigation. Menu is accessible via back button / top bar only.
 */
class DisabledNavigation : ViewerNavigation() {
    override fun getRegions(width: Int, height: Int): List<Region> {
        return emptyList()
    }
}
