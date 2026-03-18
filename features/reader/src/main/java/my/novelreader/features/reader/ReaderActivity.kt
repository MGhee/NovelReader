package my.novelreader.features.reader

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.BasicAlertDialog
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.novelreader.coreui.BaseActivity
import my.novelreader.coreui.composableActions.SetSystemBarTransparent
import my.novelreader.coreui.mappers.toPreferenceTheme
import my.novelreader.coreui.theme.Theme
import my.novelreader.coreui.theme.colorAttrRes
import my.novelreader.core.utils.Extra_Boolean
import my.novelreader.core.utils.Extra_String
import my.novelreader.core.utils.dpToPx
import my.novelreader.core.utils.fadeIn
import my.novelreader.core.appPreferences.ReaderOrientation
import my.novelreader.features.reader.domain.ChapterState
import my.novelreader.features.reader.domain.PageCalculator
import my.novelreader.features.reader.domain.PageData
import my.novelreader.features.reader.domain.ReaderItem
import my.novelreader.features.reader.domain.ReaderItemAdapter
import my.novelreader.features.reader.domain.ReaderItemBinder
import my.novelreader.features.reader.domain.ReaderPageAdapter
import my.novelreader.features.reader.domain.ReaderState
import my.novelreader.features.reader.domain.indexOfReaderItem
import my.novelreader.features.reader.view.ReaderLayoutManager
import my.novelreader.features.reader.tools.FontsLoader
import my.novelreader.features.reader.ui.ReaderScreen
import my.novelreader.features.reader.ui.ReaderViewHandlersActions
import my.novelreader.navigation.NavigationRoutes
import my.novelreader.reader.R
import my.novelreader.reader.databinding.ActivityReaderBinding
import my.novelreader.text_to_speech.Utterance
import javax.inject.Inject

@AndroidEntryPoint
class ReaderActivity : BaseActivity() {
    class IntentData : Intent, ReaderStateBundle {
        override var bookUrl by Extra_String()
        override var chapterUrl by Extra_String()
        override var introScrollToSpeaker by Extra_Boolean()

        constructor(intent: Intent) : super(intent)
        constructor(
            ctx: Context,
            bookUrl: String,
            chapterUrl: String,
            scrollToSpeakingItem: Boolean = false
        ) : super(
            ctx,
            ReaderActivity::class.java
        ) {
            this.bookUrl = bookUrl
            this.chapterUrl = chapterUrl
            this.introScrollToSpeaker = scrollToSpeakingItem
        }
    }

    @Inject
    lateinit var navigationRoutes: NavigationRoutes

    @Inject
    internal lateinit var readerViewHandlersActions: ReaderViewHandlersActions

    private var listIsScrolling = false
    private val fadeInTextLiveData = MutableLiveData(false)

    private val viewModel by viewModels<ReaderViewModel>()

    private val viewBind by lazy { ActivityReaderBinding.inflate(layoutInflater) }

    // Pages for horizontal mode
    private val pages = mutableListOf<PageData>()

    private val readerAdapter by lazy {
        ReaderItemAdapter(
            this@ReaderActivity,
            viewModel.items,
            viewModel.bookUrl,
            currentTextSelectability = { appPreferences.READER_SELECTABLE_TEXT.value },
            currentFontSize = { appPreferences.READER_FONT_SIZE.value },
            currentTypeface = { fontsLoader.getTypeFaceNORMAL(appPreferences.READER_FONT_FAMILY.value) },
            currentTypefaceBold = { fontsLoader.getTypeFaceBOLD(appPreferences.READER_FONT_FAMILY.value) },
            currentSpeakerActiveItem = { viewModel.readerSpeaker.currentTextPlaying.value },
            onChapterStartVisible = viewModel::markChapterStartAsSeen,
            onChapterEndVisible = viewModel::markChapterEndAsSeen,
            onReloadReader = viewModel::reloadReader,
            onClick = {
                viewModel.state.showReaderInfo.value = !viewModel.state.showReaderInfo.value
            },
            currentMarginDp = { appPreferences.READER_MARGIN_LEVEL.value.dpValue },
            currentLineSpacingMultiplier = { appPreferences.READER_LINE_SPACING_LEVEL.value.multiplier },
            currentLineBreakHeight = { appPreferences.READER_LINE_BREAK_HEIGHT.value },
            currentTextIndent = { appPreferences.READER_TEXT_INDENT.value },
        )
    }

    private val readerItemBinder by lazy {
        ReaderItemBinder(
            ctx = this@ReaderActivity,
            bookUrl = viewModel.bookUrl,
            currentSpeakerActiveItem = { viewModel.readerSpeaker.currentTextPlaying.value },
            currentTextSelectability = { appPreferences.READER_SELECTABLE_TEXT.value },
            currentFontSize = { appPreferences.READER_FONT_SIZE.value },
            currentTypeface = { fontsLoader.getTypeFaceNORMAL(appPreferences.READER_FONT_FAMILY.value) },
            currentTypefaceBold = { fontsLoader.getTypeFaceBOLD(appPreferences.READER_FONT_FAMILY.value) },
            currentMarginDp = { appPreferences.READER_MARGIN_LEVEL.value.dpValue },
            currentLineSpacingMultiplier = { appPreferences.READER_LINE_SPACING_LEVEL.value.multiplier },
            currentLineBreakHeight = { appPreferences.READER_LINE_BREAK_HEIGHT.value },
            currentTextIndent = { appPreferences.READER_TEXT_INDENT.value },
            onChapterStartVisible = viewModel::markChapterStartAsSeen,
            onChapterEndVisible = viewModel::markChapterEndAsSeen,
            onReloadReader = viewModel::reloadReader,
            onClick = {
                viewModel.state.showReaderInfo.value = !viewModel.state.showReaderInfo.value
            },
        )
    }

    private val readerPageAdapter by lazy {
        ReaderPageAdapter(
            ctx = this@ReaderActivity,
            pages = pages,
            binder = readerItemBinder,
            onClick = {
                viewModel.state.showReaderInfo.value = !viewModel.state.showReaderInfo.value
            }
        )
    }

    private val pageCalculator by lazy {
        PageCalculator(ctx = this@ReaderActivity, binder = readerItemBinder)
    }

    private val fontsLoader = FontsLoader(this)

    private fun isHorizontalMode(): Boolean =
        appPreferences.READER_ORIENTATION.value == ReaderOrientation.Horizontal

    private fun applyReaderOrientation() {
        if (isHorizontalMode()) {
            if (viewBind.readerView.visibility == android.view.View.VISIBLE) {
                // Switching from vertical to horizontal - get current position from RecyclerView
                val currentItemIndex = getCurrentItemIndexFromRecyclerView()

                // Hide RecyclerView
                viewBind.readerView.visibility = android.view.View.GONE

                // Recalculate pages - this runs on background thread
                recalculatePages()

                // After a short delay, show ViewPager2 (pages should be ready by then)
                lifecycleScope.launch {
                    delay(150)
                    viewBind.readerPager.visibility = android.view.View.VISIBLE
                    val pageIndex = findPageForItemIndex(currentItemIndex)
                    if (pages.isNotEmpty()) {
                        viewBind.readerPager.setCurrentItem(pageIndex, false)
                    }
                }
            } else {
                // Already in horizontal mode, just ensure ViewPager2 is visible
                viewBind.readerPager.visibility = android.view.View.VISIBLE
            }
        } else {
            // Switching from horizontal to vertical
            val currentItemIndex = if (viewBind.readerPager.visibility == android.view.View.VISIBLE) {
                getCurrentItemIndexFromPager()
            } else {
                getCurrentItemIndexFromRecyclerView()
            }

            viewBind.readerPager.visibility = android.view.View.GONE
            viewBind.readerView.visibility = android.view.View.VISIBLE
            val position = readerAdapter.fromIndexToPosition(currentItemIndex)
            if (position >= 0) {
                viewBind.readerView.layoutManager?.scrollToPosition(position)
            }
            readerAdapter.notifyDataSetChanged()
        }
    }

    private fun recalculatePages() {
        lifecycleScope.launch(Dispatchers.Default) {
            pages.clear()

            // Try multiple sources for available height
            val availableHeight = when {
                viewBind.readerPager.height > 0 -> viewBind.readerPager.height
                viewBind.readerView.height > 0 -> viewBind.readerView.height
                else -> {
                    // If not laid out yet, use window height minus status/navigation bars
                    val displayMetrics = resources.displayMetrics
                    (displayMetrics.heightPixels * 0.85).toInt() // Rough estimate
                }
            }

            android.util.Log.d("ReaderActivity", "recalculatePages: availableHeight=$availableHeight, itemCount=${viewModel.items.size}")

            if (availableHeight > 100 && viewModel.items.isNotEmpty()) {
                val calculatedPages = pageCalculator.calculatePages(viewModel.items, availableHeight)
                withContext(Dispatchers.Main) {
                    pages.addAll(calculatedPages)
                    android.util.Log.d("ReaderActivity", "recalculatePages: calculated ${pages.size} pages")
                    readerPageAdapter.notifyDataSetChanged()
                }
            } else {
                android.util.Log.w("ReaderActivity", "recalculatePages: skipped - height=$availableHeight, items=${viewModel.items.size}")
            }
        }
    }

    private fun getCurrentItemIndexFromRecyclerView(): Int {
        val layoutManager = viewBind.readerView.layoutManager
        val firstVisiblePosition = (layoutManager as? ReaderLayoutManager)?.findFirstVisibleItemPosition() ?: 0
        return readerAdapter.fromPositionToIndex(firstVisiblePosition)
    }

    private fun getCurrentItemIndexFromPager(): Int {
        if (pages.isEmpty()) return 0
        val currentPage = viewBind.readerPager.currentItem
        return pages.getOrNull(currentPage)?.firstItemIndex ?: 0
    }

    private fun findPageForItemIndex(itemIndex: Int): Int {
        for ((pageIndex, page) in pages.withIndex()) {
            if (itemIndex >= page.firstItemIndex && itemIndex < page.firstItemIndex + page.items.size) {
                return pageIndex
            }
        }
        return 0
    }

    override fun onBackPressed() {
        viewModel.onCloseManually()
        super.onBackPressed()
    }


    override fun onDestroy() {
        readerViewHandlersActions.invalidate()
        super.onDestroy()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind.readerView.adapter = readerAdapter

        // Set up ViewPager2 for horizontal mode
        viewBind.readerPager.adapter = readerPageAdapter
        viewBind.readerPager.offscreenPageLimit = 1
        viewBind.readerPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val page = pages.getOrNull(position) ?: return
                updateCurrentReadingPosSavingState(firstVisibleItemIndex = page.firstItemIndex)
                updateInfoView()
                if (position >= pages.size - 2) viewModel.chaptersLoader.tryLoadNext()
                if (position <= 1) viewModel.chaptersLoader.tryLoadPrevious()
            }
        })

        fadeInTextLiveData.distinctUntilChanged().observe(this) {
            if (it) {
                viewBind.readerView.fadeIn(durationMillis = 150)
            }
        }

        lifecycleScope.launch {
            viewModel.onTranslatorChanged.collect {
                viewModel.reloadReader()
            }
        }

        // Observe margin changes
        snapshotFlow { viewModel.state.settings.style.marginLevel.value }.drop(1)
            .asLiveData()
            .observe(this) { hardRefreshReaderListWithFlicker() }

        // Observe line spacing changes
        snapshotFlow { viewModel.state.settings.style.lineSpacingLevel.value }.drop(1)
            .asLiveData()
            .observe(this) { hardRefreshReaderListWithFlicker() }

        // Observe line break height changes
        snapshotFlow { viewModel.state.settings.style.lineBreakHeight.value }.drop(1)
            .asLiveData()
            .observe(this) { hardRefreshReaderListWithFlicker() }

        // Observe text indent changes
        snapshotFlow { viewModel.state.settings.style.textIndent.value }.drop(1)
            .asLiveData()
            .observe(this) { hardRefreshReaderListWithFlicker() }

        readerViewHandlersActions.forceUpdateListViewState = {
            withContext(Dispatchers.Main.immediate) {
                readerAdapter.notifyDataSetChanged()
            }
        }

        readerViewHandlersActions.maintainStartPosition = {
            withContext(Dispatchers.Main.immediate) {
                it()
                val titleIndex = (0..readerAdapter.count)
                    .indexOfFirst { readerAdapter.getItem(it) is ReaderItem.Title }

                if (titleIndex != -1) {
                    viewBind.readerView.scrollToPosition(titleIndex)
                }
            }
        }

        readerViewHandlersActions.setInitialPosition = {
            withContext(Dispatchers.Main.immediate) {
                initialScrollToChapterItemPosition(
                    chapterIndex = it.chapterIndex,
                    chapterItemPosition = it.chapterItemPosition,
                    offset = it.chapterItemOffset
                )
            }
        }

        readerViewHandlersActions.maintainLastVisiblePosition = {
            withContext(Dispatchers.Main.immediate) {
                val layoutManager = viewBind.readerView.layoutManager as my.novelreader.features.reader.view.ReaderLayoutManager
                val oldSize = readerAdapter.itemCount
                val position = layoutManager.findLastVisibleItemPosition()
                val view = layoutManager.findViewByPosition(position)
                val top = view?.top?.minus(view.paddingTop) ?: 0
                it()
                val displacement = readerAdapter.itemCount - oldSize
                layoutManager.scrollToPositionWithOffset(position + displacement, top)
            }
        }

        viewModel.ttsScrolledToTheTop.asLiveData().observe(this) {
            readerAdapter.notifyDataSetChanged()
            if (readerAdapter.itemCount < 1) {
                return@observe
            }
            val layoutManager = viewBind.readerView.layoutManager as my.novelreader.features.reader.view.ReaderLayoutManager
            layoutManager.smoothScrollToPositionWithOffset(1, 300.dpToPx(this), 250)
        }

        viewModel.ttsScrolledToTheBottom.asLiveData().observe(this) {
            readerAdapter.notifyDataSetChanged()
            if (readerAdapter.itemCount < 2) {
                return@observe
            }
            val layoutManager = viewBind.readerView.layoutManager as my.novelreader.features.reader.view.ReaderLayoutManager
            layoutManager.smoothScrollToPositionWithOffset(readerAdapter.itemCount - 2, 300.dpToPx(this), 250)
        }

        viewModel.readerSpeaker.currentReaderItem
            .filter { it.playState == Utterance.PlayState.PLAYING || it.playState == Utterance.PlayState.LOADING }
            .asLiveData().observe(this) {
                scrollToReadingPositionOptional(
                    chapterIndex = it.itemPos.chapterIndex,
                    chapterItemPosition = it.itemPos.chapterItemPosition,
                )
            }

        viewModel.readerSpeaker.scrollToReaderItem.asLiveData().observe(this) {
            if (it !is ReaderItem.Position) return@observe
            scrollToReadingPositionForced(
                chapterIndex = it.chapterIndex,
                chapterItemPosition = it.chapterItemPosition,
            )
        }

        viewModel.readerSpeaker.scrollToChapterTop.asLiveData()
            .observe(this) { chapterIndex ->
                scrollToReadingPositionForced(
                    chapterIndex = chapterIndex,
                    chapterItemPosition = 0,
                )
            }

        viewModel.readerSpeaker.startReadingFromFirstVisibleItem.asLiveData().observe(this) {
            val layoutManager = viewBind.readerView.layoutManager as my.novelreader.features.reader.view.ReaderLayoutManager
            val firstPosition = layoutManager.findFirstVisibleItemPosition()
            viewModel.startSpeaker(
                itemIndex = readerAdapter.getFirstVisibleItemIndexGivenPosition(firstPosition)
            )
        }

        // Notify manually text font changed for list view
        snapshotFlow { viewModel.state.settings.style.textFont.value }.drop(1)
            .asLiveData()
            .observe(this) { refreshReaderListLayout() }

        // Notify manually text size changed for list view
        snapshotFlow { viewModel.state.settings.style.textSize.value }.drop(1)
            .asLiveData()
            .observe(this) { refreshReaderListLayout() }

        // Notify manually selectable text changed for list view
        snapshotFlow { viewModel.state.settings.isTextSelectable.value }.drop(1)
            .asLiveData()
            .observe(this) { refreshReaderListLayout() }

        // Set current screen to be kept bright always or not
        snapshotFlow { viewModel.state.settings.keepScreenOn.value }
            .asLiveData()
            .observe(this) { keepScreenOn ->
                val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                if (keepScreenOn) window.addFlags(flag) else window.clearFlags(flag)
            }

        setContent {
            Theme(themeProvider) {
                SetSystemBarTransparent()

                // Reader info
                ReaderScreen(
                    state = viewModel.state,
                    onTextFontChanged = { appPreferences.READER_FONT_FAMILY.value = it },
                    onTextSizeChanged = { appPreferences.READER_FONT_SIZE.value = it },
                    onSelectableTextChange = { appPreferences.READER_SELECTABLE_TEXT.value = it },
                    onKeepScreenOn = { appPreferences.READER_KEEP_SCREEN_ON.value = it },
                    onFollowSystem = { appPreferences.THEME_FOLLOW_SYSTEM.value = it },
                    onThemeSelected = { appPreferences.THEME_ID.value = it.toPreferenceTheme },
                    onFullScreen = { appPreferences.READER_FULL_SCREEN.value = it },
                    onOrientationChange = {
                        appPreferences.READER_ORIENTATION.value = it
                        applyReaderOrientation()
                    },
                    onTextIndentChange = { appPreferences.READER_TEXT_INDENT.value = it },
                    onMarginLevelChange = { appPreferences.READER_MARGIN_LEVEL.value = it },
                    onLineSpacingLevelChange = { appPreferences.READER_LINE_SPACING_LEVEL.value = it },
                    onLineBreakHeightChange = { appPreferences.READER_LINE_BREAK_HEIGHT.value = it },
                    onPressBack = {
                        viewModel.onCloseManually()
                        finish()
                    },
                    onOpenChapterInWeb = {
                        val url = viewModel.state.readerInfo.chapterUrl.value
                        if (url.isNotBlank()) {
                            navigationRoutes.webView(this, url = url).let(::startActivity)
                        }
                    },
                    onOpenChapter = viewModel::openChapterFromList,
                    onDownloadChapter = viewModel::downloadChapterFromList,
                    onNavigateToNextChapter = viewModel::navigateToNextChapter,
                    onNavigateToPreviousChapter = viewModel::navigateToPreviousChapter,
                    onOpenChaptersList = {
                        viewModel.state.showChapterList.value = true
                    },
                    onDownloadAllChapters = viewModel::downloadAllChapters,
                    onDeleteAllChapters = viewModel::deleteAllChapters,
                    readerContent = {
                        AndroidView(factory = { viewBind.root })
                    },
                )

                if (viewModel.state.showInvalidChapterDialog.value) {
                    BasicAlertDialog(onDismissRequest = {
                        viewModel.state.showInvalidChapterDialog.value = false
                    }) {
                        Text(stringResource(id = R.string.invalid_chapter))
                    }
                }
            }
        }

        val layoutManager = viewBind.readerView.layoutManager as my.novelreader.features.reader.view.ReaderLayoutManager
        viewBind.readerView.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                    updateCurrentReadingPosSavingState(
                        firstVisibleItemIndex = readerAdapter.fromPositionToIndex(firstVisiblePosition)
                    )
                    updateInfoView()
                    updateReadingState()
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    listIsScrolling = newState != RecyclerView.SCROLL_STATE_IDLE
                }
            }
        )

        snapshotFlow { viewModel.state.settings.fullScreen.value }
            .asLiveData()
            .observe(this) { fullscreen ->
                when {
                    fullscreen -> setupFullScreenMode()
                    else -> setupNormalScreenMode()
                }
            }
        setupSystemBarAppearance()


        readerAdapter.notifyDataSetChanged()

        // Apply saved orientation after data is fully loaded
        lifecycleScope.launch {
            var attempts = 0
            while (attempts < 20 && viewModel.items.isEmpty()) {
                delay(50)
                attempts++
            }
            // Now data should be loaded, apply orientation
            if (isHorizontalMode() && viewModel.items.isNotEmpty()) {
                applyReaderOrientation()
            }
        }

        lifecycleScope.launch {
            delay(200)
            fadeInTextLiveData.postValue(true)
        }



        when {
            // Use case: user opens app from media control intent
            viewModel.introScrollToSpeaker -> {
                viewModel.introScrollToSpeaker = false
                val itemPos = viewModel.readerSpeaker.currentTextPlaying.value.itemPos
                scrollToReadingPositionImmediately(
                    chapterIndex = itemPos.chapterIndex,
                    chapterItemPosition = itemPos.chapterItemPosition,
                )
            }
            // Use case: user opens reader on the same book, on the same chapter url (session is maintained)
            // Reload from scratch to get a clean item list and position. Trying to scroll
            // within the old session's stale items on a fresh ListView causes positioning
            // failures (the ListView hasn't laid out the old items yet, so adapter position
            // mappings and maintainPosition calls work against stale layout state).
            readerViewHandlersActions.introScrollToCurrentChapter -> {
                readerViewHandlersActions.introScrollToCurrentChapter = false
                viewModel.reloadReader()
            }
        }
    }

    private fun setupNormalScreenMode() {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.displayCutout())
        controller.show(WindowInsetsCompat.Type.systemBars())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.statusBarColor = R.attr.colorSurface.colorAttrRes(this)
    }

    private fun setupFullScreenMode() {
        enableEdgeToEdge()
        // Fullscreen mode that ignores any cutout, notch etc.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.displayCutout())
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.statusBarColor = R.attr.colorSurface.colorAttrRes(this)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    }

    private fun setupSystemBarAppearance() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        combine(
            snapshotFlow { viewModel.state.showReaderInfo.value },
            snapshotFlow { viewModel.state.settings.fullScreen.value }
        ) { showReaderInfo, fullScreen -> showReaderInfo to fullScreen }
            .distinctUntilChangedBy { (showReaderInfo, fullScreen) -> showReaderInfo || !fullScreen }
            .asLiveData().observe(this) { (showReaderInfo, fullScreen) ->
                val show = showReaderInfo || !fullScreen
                when {
                    show -> controller.show(WindowInsetsCompat.Type.statusBars())
                    else -> controller.hide(WindowInsetsCompat.Type.statusBars())
                }
            }
    }

    private fun refreshReaderListLayout() {
        readerAdapter.notifyDataSetChanged()
        viewBind.readerView.requestLayout()
    }

    private fun hardRefreshReaderListWithFlicker() {
        if (isHorizontalMode()) {
            recalculatePages()
            readerPageAdapter.notifyDataSetChanged()
            return
        }

        val readerView = viewBind.readerView
        val layoutManager = readerView.layoutManager as my.novelreader.features.reader.view.ReaderLayoutManager
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val firstChildTop = layoutManager.findViewByPosition(firstVisiblePosition)?.top ?: 0

        readerView.animate().cancel()
        readerView.alpha = 0.94f

        // Force a stronger refresh cycle for visible rows, preserving scroll position.
        readerAdapter.notifyDataSetChanged()

        if (firstVisiblePosition >= 0) {
            layoutManager.scrollToPositionWithOffset(firstVisiblePosition, firstChildTop)
        }

        readerView.requestLayout()
        readerView.animate()
            .alpha(1f)
            .setDuration(120)
            .start()
    }

    private fun scrollToReadingPositionOptional(chapterIndex: Int, chapterItemPosition: Int) {
        if (isHorizontalMode()) {
            val itemIndex = indexOfReaderItem(
                list = viewModel.items,
                chapterIndex = chapterIndex,
                chapterItemPosition = chapterItemPosition
            )
            if (itemIndex != -1) {
                val pageIndex = findPageForItemIndex(itemIndex)
                viewBind.readerPager.setCurrentItem(pageIndex, true)
            }
            return
        }

        // If user already scrolling ignore
        if (listIsScrolling) {
            readerAdapter.notifyDataSetChanged()
            return
        }
        // Search for the item being read otherwise do nothing
        val layoutManager = viewBind.readerView.layoutManager as my.novelreader.features.reader.view.ReaderLayoutManager
        val firstIndex = layoutManager.findFirstVisibleItemPosition()
        val lastIndex = layoutManager.findLastVisibleItemPosition()
        for (index in firstIndex..lastIndex) {
            val item = readerAdapter.getItem(index)
            if (
                item.chapterIndex == chapterIndex &&
                item is ReaderItem.Position &&
                item.chapterItemPosition == chapterItemPosition
            ) {
                val view = layoutManager.findViewByPosition(index)
                val currentOffsetPx = view?.top?.minus(view.paddingTop) ?: 0
                val newOffsetPx = 200.dpToPx(this@ReaderActivity)
                readerAdapter.notifyDataSetChanged()
                // Scroll if item below new scroll position
                if (currentOffsetPx > newOffsetPx) {
                    (layoutManager as my.novelreader.features.reader.view.ReaderLayoutManager).smoothScrollToPositionWithOffset(index, newOffsetPx, 1000)
                }
                return
            }
        }
        readerAdapter.notifyDataSetChanged()
    }

    private fun scrollToReadingPositionForced(chapterIndex: Int, chapterItemPosition: Int) {
        // Search for the item being read otherwise do nothing
        val itemIndex = indexOfReaderItem(
            list = viewModel.items,
            chapterIndex = chapterIndex,
            chapterItemPosition = chapterItemPosition
        )
        if (itemIndex == -1) return

        if (isHorizontalMode()) {
            val pageIndex = findPageForItemIndex(itemIndex)
            viewBind.readerPager.setCurrentItem(pageIndex, true)
        } else {
            val itemPosition = readerAdapter.fromIndexToPosition(itemIndex)
            val newOffsetPx = 200.dpToPx(this@ReaderActivity)
            readerAdapter.notifyDataSetChanged()
            val layoutManager = viewBind.readerView.layoutManager as my.novelreader.features.reader.view.ReaderLayoutManager
            layoutManager.smoothScrollToPositionWithOffset(itemPosition, newOffsetPx, 500)
            readerAdapter.notifyDataSetChanged()
        }
    }

    private fun scrollToReadingPositionImmediately(chapterIndex: Int, chapterItemPosition: Int) {
        // Search for the item being read otherwise do nothing
        val itemIndex = indexOfReaderItem(
            list = viewModel.items,
            chapterIndex = chapterIndex,
            chapterItemPosition = chapterItemPosition
        )
        if (itemIndex == -1) return

        if (isHorizontalMode()) {
            val pageIndex = findPageForItemIndex(itemIndex)
            viewBind.readerPager.setCurrentItem(pageIndex, false)
        } else {
            val itemPosition = readerAdapter.fromIndexToPosition(itemIndex)
            val newOffsetPx = 200.dpToPx(this@ReaderActivity)
            readerAdapter.notifyDataSetChanged()
            val layoutManager = viewBind.readerView.layoutManager as my.novelreader.features.reader.view.ReaderLayoutManager
            layoutManager.scrollToPositionWithOffset(itemPosition, newOffsetPx)
            readerAdapter.notifyDataSetChanged()
        }
    }

    private fun updateReadingState() {
        val layoutManager = viewBind.readerView.layoutManager as my.novelreader.features.reader.view.ReaderLayoutManager
        val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()
        val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
        val totalItemCount = readerAdapter.itemCount
        val visibleItemCount =
            if (totalItemCount == 0) 0 else (lastVisibleItem - firstVisibleItem + 1)

        val isTop = visibleItemCount != 0 && firstVisibleItem <= 1
        // isBottom: when scrolled to bottomPadding (position count-1), or when last visible item is within 2 positions of end
        val isBottom = visibleItemCount != 0 && (firstVisibleItem + visibleItemCount) >= totalItemCount - 2

        when (viewModel.chaptersLoader.readerState) {
            ReaderState.IDLE -> {
                if (isBottom) {
                    viewModel.chaptersLoader.tryLoadNext()
                }
                if (isTop) {
                    viewModel.chaptersLoader.tryLoadPrevious()
                }
            }
            ReaderState.LOADING -> run {}
            ReaderState.INITIAL_LOAD -> run {}
        }
    }

    private fun initialScrollToChapterItemPosition(
        chapterIndex: Int,
        chapterItemPosition: Int,
        offset: Int
    ) {
        val index = indexOfReaderItem(
            list = viewModel.items,
            chapterIndex = chapterIndex,
            chapterItemPosition = chapterItemPosition
        )
        val position = readerAdapter.fromIndexToPosition(index)
        if (index != -1) {
            val layoutManager = viewBind.readerView.layoutManager as my.novelreader.features.reader.view.ReaderLayoutManager
            layoutManager.scrollToPositionWithOffset(position, offset)
        }
        fadeInTextLiveData.postValue(true)
        viewBind.readerView.doOnNextLayout { updateReadingState() }
    }

    private fun updateInfoView() {
        val layoutManager = viewBind.readerView.layoutManager as my.novelreader.features.reader.view.ReaderLayoutManager
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
        val itemIndex = readerAdapter.fromPositionToIndex(lastVisiblePosition)
        viewModel.updateInfoViewTo(itemIndex)
    }

    override fun onPause() {
        val firstVisibleItemIndex = if (isHorizontalMode()) {
            val currentPageIndex = viewBind.readerPager.currentItem
            pages.getOrNull(currentPageIndex)?.firstItemIndex ?: 0
        } else {
            val layoutManager = viewBind.readerView.layoutManager as my.novelreader.features.reader.view.ReaderLayoutManager
            readerAdapter.fromPositionToIndex(layoutManager.findFirstVisibleItemPosition())
        }

        updateCurrentReadingPosSavingState(firstVisibleItemIndex = firstVisibleItemIndex)
        // Explicitly save to database when app pauses
        viewModel.saveCurrentReadingPosition()
        super.onPause()
    }

    private fun updateCurrentReadingPosSavingState(firstVisibleItemIndex: Int) {
        // Don't overwrite readingCurrentChapter during a full chapter reload.
        // openChapterFromList sets it to the new chapter URL before tryLoadInitial,
        // and onScroll events during the reload could corrupt it with the old chapter's URL.
        if (viewModel.chaptersLoader.readerState == ReaderState.INITIAL_LOAD) return
        val item = viewModel.items.getOrNull(firstVisibleItemIndex) ?: return
        if (item !is ReaderItem.Position) return

        val layoutManager = viewBind.readerView.layoutManager as my.novelreader.features.reader.view.ReaderLayoutManager
        val firstPos = layoutManager.findFirstVisibleItemPosition()
        val view = layoutManager.findViewByPosition(firstPos)
        val offset = view?.top?.minus(view.paddingTop) ?: 0
        viewModel.readingCurrentChapter = ChapterState(
            chapterUrl = item.chapterUrl,
            chapterItemPosition = item.chapterItemPosition,
            offset = offset
        )
    }
}
