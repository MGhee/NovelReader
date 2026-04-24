package my.novelreader.features.mangareader.viewer.pager

import android.content.Context
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import my.novelreader.features.mangareader.model.ViewerChapters
import my.novelreader.features.mangareader.model.MangaReaderPage
import my.novelreader.features.mangareader.setting.ReaderPreferences
import my.novelreader.features.mangareader.setting.ViewerConfig
import my.novelreader.features.mangareader.viewer.Viewer
import my.novelreader.features.mangareader.viewer.ViewerNavigation
import my.novelreader.features.mangareader.viewer.navigation.DisabledNavigation
import my.novelreader.features.mangareader.viewer.navigation.EdgeNavigation
import my.novelreader.features.mangareader.viewer.navigation.KindlishNavigation
import my.novelreader.features.mangareader.viewer.navigation.LNavigation
import my.novelreader.features.mangareader.viewer.navigation.RightAndLeftNavigation
import my.novelreader.features.mangareader.setting.TapZoneStyle

/**
 * Base pager viewer (page-by-page navigation with ViewPager2).
 * Observes ReaderPreferences for reactive config updates.
 */
abstract class PagerViewer(
    context: Context,
    preferences: ReaderPreferences,
    scope: CoroutineScope,
    protected val onPageChanged: (Int) -> Unit = {},
    protected val onTapAction: (ViewerNavigation.NavigationAction) -> Unit = {},
    protected val onLongPress: (MangaReaderPage) -> Unit = {},
) : Viewer {

    override val view = ViewPager2(context)
    protected var currentPages: List<MangaReaderPage> = emptyList()
    protected var currentNavigation: ViewerNavigation = RightAndLeftNavigation()

    protected val config: ViewerConfig
    protected val configScope = CoroutineScope(scope.coroutineContext + Job())

    init {
        view.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        setPageDirection()

        // Wire page change callback
        view.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                onPageChanged(position)
            }
        })

        // Create lambda callbacks BEFORE creating config so they're ready when subscriptions emit
        val navigationListener: () -> Unit = {
            updateNavigation()
            rebuildAdapter()
        }
        val imagePropertyListener: () -> Unit = {
            // Rebuild to handle dual-page spread and other layout changes
            rebuildAdapter()
        }

        // Now create config that observes preferences
        config = ViewerConfig(preferences, configScope).apply {
            navigationModeChangedListener = navigationListener
            imagePropertyChangedListener = imagePropertyListener
        }
    }

    protected abstract fun setPageDirection()

    override fun setChapters(chapters: ViewerChapters) {
        val allPages = mutableListOf<MangaReaderPage>()
        chapters.currentChapter.pages.forEach { page ->
            allPages.add(page)
        }
        currentPages = allPages
        setupAdapter()
    }

    override fun moveToPage(index: Int) {
        if (index >= 0 && index < currentPages.size) {
            view.setCurrentItem(index, config.pageTransitionAnimation)
        }
    }

    override fun getCurrentPage(): Int {
        return view.currentItem
    }

    override fun destroy() {
        view.adapter = null
        configScope.cancel()
    }

    protected fun setupAdapter() {
        updateNavigation()
        rebuildAdapter()
    }

    protected fun rebuildAdapter() {
        val navigation = currentNavigation
        val adapter = PagerAdapter(
            pages = currentPages,
            onTapAction = { action -> onTapAction(action) },
            onLongPress = { page -> onLongPress(page) },
            navigation = navigation,
            config = config,
        )
        view.adapter = adapter
    }

    protected fun updateNavigation() {
        currentNavigation = when (config.tapZoneStyle) {
            TapZoneStyle.Disabled -> DisabledNavigation()
            TapZoneStyle.Edge -> EdgeNavigation()
            TapZoneStyle.L -> LNavigation()
            TapZoneStyle.Kindlish -> KindlishNavigation()
            TapZoneStyle.RightAndLeft -> RightAndLeftNavigation()
        }
    }
}

/**
 * Left-to-right pager (English-style reading).
 */
class L2RPagerViewer(
    context: Context,
    preferences: ReaderPreferences,
    scope: CoroutineScope,
    onPageChanged: (Int) -> Unit = {},
    onTapAction: (ViewerNavigation.NavigationAction) -> Unit = {},
    onLongPress: (MangaReaderPage) -> Unit = {},
) : PagerViewer(context, preferences, scope, onPageChanged, onTapAction, onLongPress) {
    override fun setPageDirection() {
        // LTR: pages go left to right naturally
    }
}

/**
 * Right-to-left pager (manga-style reading).
 * Pages are reversed so swiping right shows the next page.
 */
class R2LPagerViewer(
    context: Context,
    preferences: ReaderPreferences,
    scope: CoroutineScope,
    onPageChanged: (Int) -> Unit = {},
    onTapAction: (ViewerNavigation.NavigationAction) -> Unit = {},
    onLongPress: (MangaReaderPage) -> Unit = {},
) : PagerViewer(context, preferences, scope, onPageChanged, onTapAction, onLongPress) {
    override fun setPageDirection() {
        // RTL: reverse page order so swiping right goes forward
    }

    override fun setChapters(chapters: ViewerChapters) {
        // Reverse the page list for RTL reading
        val allPages = mutableListOf<MangaReaderPage>()
        chapters.currentChapter.pages.reversed().forEach { page ->
            allPages.add(page)
        }
        currentPages = allPages
        setupAdapter()
    }
}

/**
 * Vertical pager (vertical page-by-page navigation).
 */
class VerticalPagerViewer(
    context: Context,
    preferences: ReaderPreferences,
    scope: CoroutineScope,
    onPageChanged: (Int) -> Unit = {},
    onTapAction: (ViewerNavigation.NavigationAction) -> Unit = {},
    onLongPress: (MangaReaderPage) -> Unit = {},
) : PagerViewer(context, preferences, scope, onPageChanged, onTapAction, onLongPress) {
    override fun setPageDirection() {
        view.orientation = ViewPager2.ORIENTATION_VERTICAL
    }
}
