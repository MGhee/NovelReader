package my.novelreader.features.mangareader.viewer.webtoon

import android.content.Context
import android.view.View
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
import my.novelreader.features.mangareader.viewer.navigation.WebtoonNavigation
import my.novelreader.features.mangareader.setting.TapZoneStyle

/**
 * Webtoon-mode manga viewer (continuous vertical scroll).
 * Uses RecyclerView with SubsamplingScaleImageView for large page support.
 * Observes ReaderPreferences for reactive config updates.
 */
class WebtoonViewer(
    context: Context,
    preferences: ReaderPreferences,
    scope: CoroutineScope,
    private val onPageChanged: (Int) -> Unit = {},
    private val onTapAction: (ViewerNavigation.NavigationAction) -> Unit = {},
    private val onLongPress: (MangaReaderPage) -> Unit = {},
) : Viewer {

    override val view = WebtoonRecyclerView(context)
    private var currentPages: List<MangaReaderPage> = emptyList()
    private var currentNavigation: ViewerNavigation = WebtoonNavigation()
    private var currentAdapter: WebtoonAdapter? = null

    private val config: ViewerConfig
    private val configScope = CoroutineScope(scope.coroutineContext + Job())

    init {
        view.layoutManager = WebtoonLayoutManager(context)

        // Wire page change callback
        view.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as? WebtoonLayoutManager
                val position = layoutManager?.findFirstVisibleItemPosition() ?: return
                onPageChanged(position)
            }
        })

        // Tap detection lives on the recycler, not per-item — this way SSIV
        // can't eat ACTION_DOWN and break scrolling.
        view.tapListener = { x, y, w, h ->
            val action = currentNavigation.getActionForTap(x, y, w, h)
            if (action != null) onTapAction(action)
        }
        view.longTapListener = {
            currentPages.getOrNull(getCurrentPage())?.let { onLongPress(it) }
        }

        // Create lambda callbacks BEFORE creating config so they're ready when subscriptions emit
        val navigationListener: () -> Unit = {
            updateNavigation()
            rebuildAdapter()
        }
        val imagePropertyListener: () -> Unit = {
            // Force adapter to rebind visible holders so they apply new settings
            currentAdapter?.notifyDataSetChanged()
        }

        // Now create config that observes preferences
        config = ViewerConfig(preferences, configScope).apply {
            navigationModeChangedListener = navigationListener
            imagePropertyChangedListener = imagePropertyListener
        }
    }

    override fun setChapters(chapters: ViewerChapters) {
        // Combine all chapters into a single page list
        val allPages = mutableListOf<MangaReaderPage>()

        // Pages from current chapter
        chapters.currentChapter.pages.forEach { page ->
            allPages.add(page)
        }

        currentPages = allPages
        setupAdapter()
    }

    override fun moveToPage(index: Int) {
        if (index >= 0 && index < currentPages.size) {
            val layoutManager = view.layoutManager as? WebtoonLayoutManager
            layoutManager?.scrollToPosition(index)
        }
    }

    override fun getCurrentPage(): Int {
        val layoutManager = view.layoutManager as? WebtoonLayoutManager ?: return 0
        return layoutManager.findFirstVisibleItemPosition()
    }

    override fun destroy() {
        view.adapter = null
        currentAdapter = null
        configScope.cancel()
    }

    private fun setupAdapter() {
        updateNavigation()
        rebuildAdapter()
    }

    private fun rebuildAdapter() {
        val adapter = WebtoonAdapter(pages = currentPages, config = config)
        currentAdapter = adapter
        view.adapter = adapter
    }

    private fun updateNavigation() {
        // Webtoon mode always uses WebtoonNavigation (whole screen = MENU).
        // Pagination is handled via scrolling, not tap zones.
        currentNavigation = WebtoonNavigation()
    }
}
