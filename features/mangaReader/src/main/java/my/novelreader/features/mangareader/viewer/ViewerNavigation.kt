package my.novelreader.features.mangareader.viewer

import android.graphics.RectF

/**
 * Defines tap zones for manga page navigation.
 * Each region maps a screen area to a navigation action.
 */
abstract class ViewerNavigation {
    data class Region(
        val rect: RectF,
        val action: NavigationAction,
    )

    enum class NavigationAction {
        PREV_PAGE,
        NEXT_PAGE,
        PREV_CHAPTER,
        NEXT_CHAPTER,
        MENU,
    }

    /**
     * Get the tap regions for the given screen dimensions.
     * Regions are in normalized coordinates (0f to 1f for width/height).
     */
    abstract fun getRegions(width: Int, height: Int): List<Region>

    /**
     * Get the action for a tap at the given normalized coordinates.
     * Returns null if no region matches.
     */
    fun getActionForTap(x: Float, y: Float, width: Int, height: Int): NavigationAction? {
        val regions = getRegions(width, height)
        for (region in regions) {
            if (region.rect.contains(x, y)) {
                return region.action
            }
        }
        return null
    }
}
