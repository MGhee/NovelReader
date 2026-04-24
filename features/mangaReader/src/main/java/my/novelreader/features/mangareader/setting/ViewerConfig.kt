package my.novelreader.features.mangareader.setting

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Configuration for viewers that observes ReaderPreferences and notifies listeners of changes.
 * Follows Komikku's WebtoonConfig pattern for reactive preference binding.
 */
class ViewerConfig(
    preferences: ReaderPreferences,
    scope: CoroutineScope,
) {
    var tapZoneStyle = preferences.tapZoneStyle.value
        private set
    var tappingInvertMode = preferences.tappingInvertMode.value
        private set
    var backgroundColor = preferences.backgroundColor.value
        private set
    var brightness = preferences.brightness.value
        private set
    var colorFilter = preferences.colorFilter.value
        private set
    var colorFilterAlpha = preferences.colorFilterAlpha.value
        private set
    var colorFilterValue = preferences.colorFilterValue.value
        private set
    var cropBorders = preferences.cropBorders.value
        private set
    var pageTransitionAnimation = preferences.pageTransitionAnimation.value
        private set
    var dualPageSpreads = preferences.dualPageSpreads.value
        private set
    var doubleTapZoom = preferences.doubleTapZoom.value
        private set
    var orientation = preferences.orientation.value
        private set

    // Callbacks when settings change
    var imagePropertyChangedListener: (() -> Unit)? = null
    var navigationModeChangedListener: (() -> Unit)? = null

    init {
        // Subscribe to preference changes and update local fields
        preferences.tapZoneStyle
            .onEach { tapZoneStyle = it; navigationModeChangedListener?.invoke() }
            .launchIn(scope)

        preferences.tappingInvertMode
            .onEach { tappingInvertMode = it; navigationModeChangedListener?.invoke() }
            .launchIn(scope)

        // Background, brightness and color filter are rendered by Compose overlays.
        // They must NOT trigger adapter rebuilds — rebuilding resets ViewPager2
        // position and briefly breaks touch handling while views reattach.
        preferences.backgroundColor
            .onEach { backgroundColor = it }
            .launchIn(scope)

        preferences.brightness
            .onEach { brightness = it }
            .launchIn(scope)

        preferences.colorFilter
            .onEach { colorFilter = it }
            .launchIn(scope)

        preferences.colorFilterAlpha
            .onEach { colorFilterAlpha = it }
            .launchIn(scope)

        preferences.colorFilterValue
            .onEach { colorFilterValue = it }
            .launchIn(scope)

        preferences.cropBorders
            // drop(1) avoids a no-op rebuild on init
            .drop(1)
            .onEach { cropBorders = it; imagePropertyChangedListener?.invoke() }
            .launchIn(scope)

        preferences.pageTransitionAnimation
            .drop(1)
            .onEach { pageTransitionAnimation = it }
            .launchIn(scope)

        preferences.dualPageSpreads
            .drop(1)
            .onEach { dualPageSpreads = it; imagePropertyChangedListener?.invoke() }
            .launchIn(scope)

        preferences.doubleTapZoom
            .drop(1)
            .onEach { doubleTapZoom = it; imagePropertyChangedListener?.invoke() }
            .launchIn(scope)

        preferences.orientation
            .drop(1)
            .onEach { orientation = it }
            .launchIn(scope)
    }
}
