package my.novelreader.features.mangareader.setting

import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.novelreader.core.appPreferences.AppPreferences

/**
 * Manga reader preferences exposed as StateFlows for reactive UI binding.
 * All preferences are backed by SharedPreferences (via AppPreferences) for persistence.
 */
class ReaderPreferences(
    private val appPreferences: AppPreferences,
    private val scope: CoroutineScope,
) {
    // Reading mode (Webtoon, LTR, RTL, Vertical, etc.)
    private val _readingMode = MutableStateFlow(ReadingMode.valueOf(appPreferences.MANGA_READING_MODE.value))
    val readingMode: StateFlow<ReadingMode> = _readingMode

    // Screen orientation
    private val _orientation = MutableStateFlow(ReaderOrientation.valueOf(appPreferences.MANGA_ORIENTATION.value))
    val orientation: StateFlow<ReaderOrientation> = _orientation

    // Tap zone style (Edge, L, Kindlish, etc.)
    private val _tapZoneStyle = MutableStateFlow(TapZoneStyle.valueOf(appPreferences.MANGA_TAP_ZONE_STYLE.value))
    val tapZoneStyle: StateFlow<TapZoneStyle> = _tapZoneStyle

    // Invert tap zones (for RTL reading)
    private val _tappingInvertMode = MutableStateFlow(TappingInvertMode.valueOf(appPreferences.MANGA_TAPPING_INVERT_MODE.value))
    val tappingInvertMode: StateFlow<TappingInvertMode> = _tappingInvertMode

    // Page background color
    private val _backgroundColor = MutableStateFlow(ReaderBackgroundColor.valueOf(appPreferences.MANGA_BACKGROUND_COLOR.value))
    val backgroundColor: StateFlow<ReaderBackgroundColor> = _backgroundColor

    // Brightness overlay (0f to 2f)
    private val _brightness = MutableStateFlow(appPreferences.MANGA_BRIGHTNESS.value)
    val brightness: StateFlow<Float> = _brightness

    // Color filter (none, custom, blue light, etc.)
    private val _colorFilter = MutableStateFlow(ColorFilterMode.valueOf(appPreferences.MANGA_COLOR_FILTER.value))
    val colorFilter: StateFlow<ColorFilterMode> = _colorFilter

    // Color filter alpha (for blending)
    private val _colorFilterAlpha = MutableStateFlow(appPreferences.MANGA_COLOR_FILTER_ALPHA.value)
    val colorFilterAlpha: StateFlow<Float> = _colorFilterAlpha

    // Custom color filter RGBA (as packed Int)
    private val _colorFilterValue = MutableStateFlow(appPreferences.MANGA_COLOR_FILTER_VALUE.value)
    val colorFilterValue: StateFlow<Int> = _colorFilterValue

    // Crop borders (removes white space from pages)
    private val _cropBorders = MutableStateFlow(appPreferences.MANGA_CROP_BORDERS.value)
    val cropBorders: StateFlow<Boolean> = _cropBorders

    // Show chapter transition cards between chapters
    private val _showTransitionCard = MutableStateFlow(appPreferences.MANGA_SHOW_TRANSITION_CARD.value)
    val showTransitionCard: StateFlow<Boolean> = _showTransitionCard

    // Keep screen on while reading
    private val _keepScreenOn = MutableStateFlow(appPreferences.MANGA_KEEP_SCREEN_ON.value)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn

    // Full-screen mode (hide system UI)
    private val _fullScreen = MutableStateFlow(appPreferences.MANGA_FULL_SCREEN.value)
    val fullScreen: StateFlow<Boolean> = _fullScreen

    // Show page indicator/number
    private val _showPageIndicator = MutableStateFlow(appPreferences.MANGA_SHOW_PAGE_INDICATOR.value)
    val showPageIndicator: StateFlow<Boolean> = _showPageIndicator

    // Double-tap zoom behavior
    private val _doubleTapZoom = MutableStateFlow(appPreferences.MANGA_DOUBLE_TAP_ZOOM.value)
    val doubleTapZoom: StateFlow<Boolean> = _doubleTapZoom

    // Pager mode options
    private val _pageTransitionAnimation = MutableStateFlow(appPreferences.MANGA_PAGE_TRANSITION_ANIMATION.value)
    val pageTransitionAnimation: StateFlow<Boolean> = _pageTransitionAnimation

    private val _dualPageSpreads = MutableStateFlow(appPreferences.MANGA_DUAL_PAGE_SPREADS.value)
    val dualPageSpreads: StateFlow<Boolean> = _dualPageSpreads

    // First-launch navigation overlay shown flag
    private val _showNavOverlayFirstLaunch = MutableStateFlow(appPreferences.MANGA_SHOW_NAV_OVERLAY_FIRST_LAUNCH.value)
    val showNavOverlayFirstLaunch: StateFlow<Boolean> = _showNavOverlayFirstLaunch

    init {
        // Subscribe to AppPreferences changes and update StateFlows
        scope.launch {
            appPreferences.MANGA_READING_MODE.flow().collect { _readingMode.value = ReadingMode.valueOf(it) }
        }
        scope.launch {
            appPreferences.MANGA_ORIENTATION.flow().collect { _orientation.value = ReaderOrientation.valueOf(it) }
        }
        scope.launch {
            appPreferences.MANGA_TAP_ZONE_STYLE.flow().collect { _tapZoneStyle.value = TapZoneStyle.valueOf(it) }
        }
        scope.launch {
            appPreferences.MANGA_TAPPING_INVERT_MODE.flow().collect { _tappingInvertMode.value = TappingInvertMode.valueOf(it) }
        }
        scope.launch {
            appPreferences.MANGA_BACKGROUND_COLOR.flow().collect { _backgroundColor.value = ReaderBackgroundColor.valueOf(it) }
        }
        scope.launch {
            appPreferences.MANGA_BRIGHTNESS.flow().collect { _brightness.value = it }
        }
        scope.launch {
            appPreferences.MANGA_COLOR_FILTER.flow().collect { _colorFilter.value = ColorFilterMode.valueOf(it) }
        }
        scope.launch {
            appPreferences.MANGA_COLOR_FILTER_ALPHA.flow().collect { _colorFilterAlpha.value = it }
        }
        scope.launch {
            appPreferences.MANGA_COLOR_FILTER_VALUE.flow().collect { _colorFilterValue.value = it }
        }
        scope.launch {
            appPreferences.MANGA_CROP_BORDERS.flow().collect { _cropBorders.value = it }
        }
        scope.launch {
            appPreferences.MANGA_SHOW_TRANSITION_CARD.flow().collect { _showTransitionCard.value = it }
        }
        scope.launch {
            appPreferences.MANGA_KEEP_SCREEN_ON.flow().collect { _keepScreenOn.value = it }
        }
        scope.launch {
            appPreferences.MANGA_FULL_SCREEN.flow().collect { _fullScreen.value = it }
        }
        scope.launch {
            appPreferences.MANGA_SHOW_PAGE_INDICATOR.flow().collect { _showPageIndicator.value = it }
        }
        scope.launch {
            appPreferences.MANGA_DOUBLE_TAP_ZOOM.flow().collect { _doubleTapZoom.value = it }
        }
        scope.launch {
            appPreferences.MANGA_PAGE_TRANSITION_ANIMATION.flow().collect { _pageTransitionAnimation.value = it }
        }
        scope.launch {
            appPreferences.MANGA_DUAL_PAGE_SPREADS.flow().collect { _dualPageSpreads.value = it }
        }
        scope.launch {
            appPreferences.MANGA_SHOW_NAV_OVERLAY_FIRST_LAUNCH.flow().collect { _showNavOverlayFirstLaunch.value = it }
        }
    }

    // Methods to update preferences (write to both in-memory StateFlow and persistent storage)

    fun setReadingMode(mode: ReadingMode) {
        appPreferences.MANGA_READING_MODE.value = mode.name
        _readingMode.value = mode
    }

    fun setOrientation(orientation: ReaderOrientation) {
        appPreferences.MANGA_ORIENTATION.value = orientation.name
        _orientation.value = orientation
    }

    fun setTapZoneStyle(style: TapZoneStyle) {
        appPreferences.MANGA_TAP_ZONE_STYLE.value = style.name
        _tapZoneStyle.value = style
    }

    fun setTappingInvertMode(mode: TappingInvertMode) {
        appPreferences.MANGA_TAPPING_INVERT_MODE.value = mode.name
        _tappingInvertMode.value = mode
    }

    fun setBackgroundColor(color: ReaderBackgroundColor) {
        appPreferences.MANGA_BACKGROUND_COLOR.value = color.name
        _backgroundColor.value = color
    }

    fun setBrightness(value: Float) {
        val coerced = value.coerceIn(0f, 2f)
        appPreferences.MANGA_BRIGHTNESS.value = coerced
        _brightness.value = coerced
    }

    fun setColorFilter(filter: ColorFilterMode) {
        appPreferences.MANGA_COLOR_FILTER.value = filter.name
        _colorFilter.value = filter
    }

    fun setColorFilterAlpha(alpha: Float) {
        val coerced = alpha.coerceIn(0f, 1f)
        appPreferences.MANGA_COLOR_FILTER_ALPHA.value = coerced
        _colorFilterAlpha.value = coerced
    }

    fun setColorFilterValue(color: Int) {
        appPreferences.MANGA_COLOR_FILTER_VALUE.value = color
        _colorFilterValue.value = color
    }

    fun setCropBorders(enabled: Boolean) {
        appPreferences.MANGA_CROP_BORDERS.value = enabled
        _cropBorders.value = enabled
    }

    fun setShowTransitionCard(enabled: Boolean) {
        appPreferences.MANGA_SHOW_TRANSITION_CARD.value = enabled
        _showTransitionCard.value = enabled
    }

    fun setKeepScreenOn(enabled: Boolean) {
        appPreferences.MANGA_KEEP_SCREEN_ON.value = enabled
        _keepScreenOn.value = enabled
    }

    fun setFullScreen(enabled: Boolean) {
        appPreferences.MANGA_FULL_SCREEN.value = enabled
        _fullScreen.value = enabled
    }

    fun setShowPageIndicator(enabled: Boolean) {
        appPreferences.MANGA_SHOW_PAGE_INDICATOR.value = enabled
        _showPageIndicator.value = enabled
    }

    fun setDoubleTapZoom(enabled: Boolean) {
        appPreferences.MANGA_DOUBLE_TAP_ZOOM.value = enabled
        _doubleTapZoom.value = enabled
    }

    fun setPageTransitionAnimation(enabled: Boolean) {
        appPreferences.MANGA_PAGE_TRANSITION_ANIMATION.value = enabled
        _pageTransitionAnimation.value = enabled
    }

    fun setDualPageSpreads(enabled: Boolean) {
        appPreferences.MANGA_DUAL_PAGE_SPREADS.value = enabled
        _dualPageSpreads.value = enabled
    }

    fun setShowNavOverlayFirstLaunch(value: Boolean) {
        appPreferences.MANGA_SHOW_NAV_OVERLAY_FIRST_LAUNCH.value = value
        _showNavOverlayFirstLaunch.value = value
    }
}
