package my.novelreader.features.mangareader.presentation

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import my.novelreader.features.mangareader.MangaReaderViewModel
import my.novelreader.features.mangareader.setting.ReadingMode
import my.novelreader.features.mangareader.setting.ReaderBackgroundColor
import my.novelreader.features.mangareader.viewer.Viewer
import my.novelreader.features.mangareader.viewer.webtoon.WebtoonViewer
import my.novelreader.features.mangareader.viewer.pager.L2RPagerViewer
import my.novelreader.features.mangareader.viewer.pager.R2LPagerViewer
import my.novelreader.features.mangareader.viewer.pager.VerticalPagerViewer
import kotlinx.coroutines.CoroutineScope

/**
 * Root Compose layout for manga reader.
 * Displays the View-based viewer + Compose chrome overlay (top bar, bottom bar, dialogs).
 */
@Composable
fun MangaReaderRoot(
    viewModel: MangaReaderViewModel,
    onBack: () -> Unit,
) {
    val currentViewer by viewModel.currentViewer.collectAsState()
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    val showMenus by viewModel.showMenus
    val viewerChapters by viewModel.viewerChapters.collectAsState()
    val readingMode by viewModel.readingMode.collectAsState()
    val bgColor by viewModel.backgroundColor.collectAsState()
    val keepScreenOn by viewModel.readerPrefs.keepScreenOn.collectAsState()
    val fullScreen by viewModel.readerPrefs.fullScreen.collectAsState()
    val brightness by viewModel.readerPrefs.brightness.collectAsState()
    val colorFilter by viewModel.readerPrefs.colorFilter.collectAsState()
    val colorFilterAlpha by viewModel.readerPrefs.colorFilterAlpha.collectAsState()
    val colorFilterValue by viewModel.readerPrefs.colorFilterValue.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Apply activity-level settings
    LaunchedEffect(keepScreenOn) {
        activity?.window?.let { window ->
            if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    LaunchedEffect(brightness) {
        activity?.window?.let { window ->
            val lp = window.attributes
            lp.screenBrightness = brightness.coerceIn(0f, 1f)
            window.attributes = lp
        }
    }
    LaunchedEffect(fullScreen, showMenus) {
        val window = activity?.window ?: return@LaunchedEffect
        val decor = window.decorView
        if (fullScreen && !showMenus) {
            @Suppress("DEPRECATION")
            decor.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        } else {
            @Suppress("DEPRECATION")
            decor.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    // Build or switch viewer based on reading mode
    val viewerRef = remember { mutableStateOf<Viewer?>(null) }
    val viewer = remember(readingMode) {
        // Create a new scope for this viewer instance
        val viewerScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        buildViewer(readingMode, context, viewModel, viewerScope, viewerRef).also {
            viewerRef.value = it
        }
    }

    // Destroy the viewer when it's replaced (mode switch) or the screen leaves composition
    DisposableEffect(viewer) {
        onDispose { viewer.destroy() }
    }

    // Convert bgColor enum to Color
    val backgroundColor = when (bgColor) {
        ReaderBackgroundColor.Black -> Color.Black
        ReaderBackgroundColor.White -> Color.White
        ReaderBackgroundColor.Gray -> Color.Gray
        ReaderBackgroundColor.Automatic -> Color.Black
    }

    // Feed chapters into viewer when they load
    LaunchedEffect(viewer, viewerChapters) {
        viewerChapters?.let { chapters ->
            viewer.setChapters(chapters)
            viewer.moveToPage(viewModel.initialPageIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Error message
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Error: ${errorMessage!!}",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxSize(0.9f)
                )
            }
            return@Box
        }

        // Viewer (View-based RecyclerView or ViewPager2).
        // key(viewer) forces AndroidView to rebuild its hosted view when the
        // viewer instance changes (e.g. reading-mode switch) — otherwise the
        // factory is only invoked once and the old view sticks around.
        if (!isLoading) {
            key(viewer) {
                AndroidView(
                    factory = { viewer.view },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Brightness overlay for values > 1x (brightening)
        if (brightness > 1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = (brightness - 1f).coerceIn(0f, 1f) * 0.5f))
            )
        }

        // Color filter overlay
        when (colorFilter) {
            my.novelreader.features.mangareader.setting.ColorFilterMode.None -> {}
            my.novelreader.features.mangareader.setting.ColorFilterMode.BlueLight -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFA000).copy(alpha = colorFilterAlpha))
                    )
            }
            my.novelreader.features.mangareader.setting.ColorFilterMode.Sepia -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE4C590).copy(alpha = colorFilterAlpha))
                    )
            }
            my.novelreader.features.mangareader.setting.ColorFilterMode.Grayscale -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray.copy(alpha = colorFilterAlpha * 0.3f))
                    )
            }
            my.novelreader.features.mangareader.setting.ColorFilterMode.Invert -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = colorFilterAlpha * 0.4f))
                    )
            }
            my.novelreader.features.mangareader.setting.ColorFilterMode.Custom -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(colorFilterValue).copy(alpha = colorFilterAlpha))
                    )
            }
        }

        // Navigation overlay (first-launch tap zone guide)
        if (!isLoading) {
            MangaReaderNavigationOverlay(
                viewModel = viewModel,
                onDismiss = { }
            )
        }

        // Compose chrome overlay (top bar, bottom bar, dialogs)
        if (showMenus && !isLoading) {
            MangaReaderChrome(
                viewModel = viewModel,
                onBack = onBack,
                onSeekToPage = { index ->
                    viewer.moveToPage(index)
                    viewModel.onPageChanged(index)
                }
            )
        }

        // Settings sheet
        if (viewModel.showSettingsSheet.value) {
            ReaderSettingsSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.showSettingsSheet.value = false }
            )
        }

        // Chapter list sheet
        if (viewModel.showChapterListSheet.value) {
            MangaChapterListSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.showChapterListSheet.value = false }
            )
        }

        // Page actions sheet
        if (viewModel.showPageActionsSheet.value) {
            PageActionsSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.showPageActionsSheet.value = false }
            )
        }
    }
}

private fun buildViewer(
    readingMode: ReadingMode,
    context: Context,
    viewModel: MangaReaderViewModel,
    scope: CoroutineScope,
    viewerRef: androidx.compose.runtime.MutableState<Viewer?>,
): Viewer {
    val onPageChanged: (Int) -> Unit = { viewModel.onPageChanged(it) }
    val onTapAction: (my.novelreader.features.mangareader.viewer.ViewerNavigation.NavigationAction) -> Unit = { action ->
        when (action) {
            my.novelreader.features.mangareader.viewer.ViewerNavigation.NavigationAction.PREV_PAGE -> {
                viewerRef.value?.let { v ->
                    val current = v.getCurrentPage()
                    if (current > 0) v.moveToPage(current - 1)
                }
            }
            my.novelreader.features.mangareader.viewer.ViewerNavigation.NavigationAction.NEXT_PAGE -> {
                viewerRef.value?.let { v ->
                    v.moveToPage(v.getCurrentPage() + 1)
                }
            }
            my.novelreader.features.mangareader.viewer.ViewerNavigation.NavigationAction.PREV_CHAPTER -> viewModel.loadPrevChapter()
            my.novelreader.features.mangareader.viewer.ViewerNavigation.NavigationAction.NEXT_CHAPTER -> viewModel.loadNextChapter()
            my.novelreader.features.mangareader.viewer.ViewerNavigation.NavigationAction.MENU -> viewModel.showMenus.value = !viewModel.showMenus.value
        }
    }
    val onLongPress: (my.novelreader.features.mangareader.model.MangaReaderPage) -> Unit = { viewModel.onPageLongPressed() }

    return when (readingMode) {
        ReadingMode.Webtoon -> WebtoonViewer(context, viewModel.readerPrefs, scope, onPageChanged, onTapAction, onLongPress)
        ReadingMode.ContinuousVertical -> WebtoonViewer(context, viewModel.readerPrefs, scope, onPageChanged, onTapAction, onLongPress)
        ReadingMode.LeftToRight -> L2RPagerViewer(context, viewModel.readerPrefs, scope, onPageChanged, onTapAction, onLongPress)
        ReadingMode.RightToLeft -> R2LPagerViewer(context, viewModel.readerPrefs, scope, onPageChanged, onTapAction, onLongPress)
        ReadingMode.Vertical -> VerticalPagerViewer(context, viewModel.readerPrefs, scope, onPageChanged, onTapAction, onLongPress)
    }
}
