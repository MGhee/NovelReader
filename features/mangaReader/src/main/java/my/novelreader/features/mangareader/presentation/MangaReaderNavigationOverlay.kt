package my.novelreader.features.mangareader.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import my.novelreader.features.mangareader.MangaReaderViewModel
import my.novelreader.features.mangareader.setting.TapZoneStyle
import my.novelreader.features.mangareader.viewer.ViewerNavigation
import my.novelreader.features.mangareader.viewer.navigation.DisabledNavigation
import my.novelreader.features.mangareader.viewer.navigation.EdgeNavigation
import my.novelreader.features.mangareader.viewer.navigation.KindlishNavigation
import my.novelreader.features.mangareader.viewer.navigation.LNavigation
import my.novelreader.features.mangareader.viewer.navigation.RightAndLeftNavigation

@Composable
fun MangaReaderNavigationOverlay(
    viewModel: MangaReaderViewModel,
    onDismiss: () -> Unit,
) {
    val tapZoneStyle by viewModel.readerPrefs.tapZoneStyle.collectAsState()
    val showOverlay by viewModel.readerPrefs.showNavOverlayFirstLaunch.collectAsState()

    if (!showOverlay || tapZoneStyle == TapZoneStyle.Disabled) {
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.4f)
            .background(Color.Black)
            .clickable {
                viewModel.readerPrefs.setShowNavOverlayFirstLaunch(false)
                onDismiss()
            }
    ) {
        Text(
            "Tap Guide\n(tap anywhere to dismiss)",
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.titleMedium
        )
    }
}
