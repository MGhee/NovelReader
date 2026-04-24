package my.novelreader.features.mangareader.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import my.novelreader.features.mangareader.MangaReaderViewModel

/**
 * Compose chrome overlay with top and bottom bars.
 * TODO: Add full settings dialogs, chapter list, etc.
 */
@Composable
fun MangaReaderChrome(
    viewModel: MangaReaderViewModel,
    onBack: () -> Unit,
    onSeekToPage: (Int) -> Unit = { },
) {
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val showPageIndicator by viewModel.readerPrefs.showPageIndicator.collectAsState()

    Column(
        modifier = Modifier.fillMaxHeight()
    ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0x80000000))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
                Text(
                    currentChapter?.title ?: "Manga Reader",
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                )
            }
        }

        // Spacer to push bottom bar down
        Spacer(modifier = Modifier.weight(1f))

        // Bottom bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color(0x80000000))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Prev chapter button
                IconButton(onClick = { viewModel.loadPrevChapter() }) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Prev Chapter",
                        tint = Color.White,
                    )
                }

                // Page slider with indicator
                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = currentPageIndex.toFloat(),
                        onValueChange = { onSeekToPage(it.toInt()) },
                        valueRange = 0f..((currentChapter?.pages?.size ?: 1) - 1).toFloat(),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    if (showPageIndicator) {
                        Text(
                            "${currentPageIndex + 1} / ${currentChapter?.pages?.size ?: 1}",
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 4.dp),
                            fontSize = androidx.compose.material3.MaterialTheme.typography.labelSmall.fontSize
                        )
                    }
                }

                // Next chapter button
                IconButton(onClick = { viewModel.loadNextChapter() }) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next Chapter",
                        tint = Color.White,
                    )
                }

                // Chapter list button
                IconButton(onClick = { viewModel.showChapterListSheet.value = true }) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = "Chapters",
                        tint = Color.White,
                    )
                }

                // Settings button
                IconButton(onClick = { viewModel.showSettingsSheet.value = true }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}
