package my.novelreader.features.mangareader.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import my.novelreader.features.mangareader.MangaReaderViewModel
import my.novelreader.features.mangareader.setting.ColorFilterMode
import my.novelreader.features.mangareader.setting.ReadingMode
import my.novelreader.features.mangareader.setting.ReaderBackgroundColor
import my.novelreader.features.mangareader.setting.ReaderOrientation
import my.novelreader.features.mangareader.setting.TapZoneStyle
import my.novelreader.features.mangareader.setting.TappingInvertMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderSettingsSheet(
    viewModel: MangaReaderViewModel,
    onDismiss: () -> Unit,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Reading Mode") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("General") }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Color Filter") }
                )
            }

            when (selectedTabIndex) {
                0 -> ReadingModeTab(viewModel)
                1 -> GeneralTab(viewModel)
                2 -> ColorFilterTab(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingModeTab(viewModel: MangaReaderViewModel) {
    val readingMode by viewModel.readerPrefs.readingMode.collectAsState()
    val orientation by viewModel.readerPrefs.orientation.collectAsState()
    val pageTransitionAnimation by viewModel.readerPrefs.pageTransitionAnimation.collectAsState()
    val dualPageSpreads by viewModel.readerPrefs.dualPageSpreads.collectAsState()
    val cropBorders by viewModel.readerPrefs.cropBorders.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Reading Mode", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            ReadingMode.values().forEach { mode ->
                FilterChip(
                    selected = readingMode == mode,
                    onClick = { viewModel.onReadingModeChanged(mode) },
                    label = { Text(mode.label) },
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Text("Orientation", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            ReaderOrientation.values().forEach { o ->
                FilterChip(
                    selected = orientation == o,
                    onClick = { viewModel.readerPrefs.setOrientation(o) },
                    label = { Text(o.name) },
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Page Transition Animation", modifier = Modifier.weight(1f))
            Switch(checked = pageTransitionAnimation, onCheckedChange = { viewModel.readerPrefs.setPageTransitionAnimation(it) })
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dual Page Spreads", modifier = Modifier.weight(1f))
            Switch(checked = dualPageSpreads, onCheckedChange = { viewModel.readerPrefs.setDualPageSpreads(it) })
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Crop Borders", modifier = Modifier.weight(1f))
            Switch(checked = cropBorders, onCheckedChange = { viewModel.readerPrefs.setCropBorders(it) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneralTab(viewModel: MangaReaderViewModel) {
    val backgroundColor by viewModel.readerPrefs.backgroundColor.collectAsState()
    val tapZoneStyle by viewModel.readerPrefs.tapZoneStyle.collectAsState()
    val tappingInvertMode by viewModel.readerPrefs.tappingInvertMode.collectAsState()
    val fullScreen by viewModel.readerPrefs.fullScreen.collectAsState()
    val keepScreenOn by viewModel.readerPrefs.keepScreenOn.collectAsState()
    val showPageIndicator by viewModel.readerPrefs.showPageIndicator.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Background Color", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            ReaderBackgroundColor.values().forEach { color ->
                val bgColor = when (color) {
                    ReaderBackgroundColor.Black -> Color.Black
                    ReaderBackgroundColor.White -> Color.White
                    ReaderBackgroundColor.Gray -> Color.Gray
                    ReaderBackgroundColor.Automatic -> Color.DarkGray
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(bgColor, RoundedCornerShape(4.dp))
                        .border(
                            if (backgroundColor == color) 3.dp else 1.dp,
                            if (backgroundColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                        .selectable(backgroundColor == color) { viewModel.readerPrefs.setBackgroundColor(color) }
                        .padding(4.dp)
                )
            }
        }

        Text("Tap Zone Style", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            TapZoneStyle.values().forEach { style ->
                FilterChip(
                    selected = tapZoneStyle == style,
                    onClick = { viewModel.readerPrefs.setTapZoneStyle(style) },
                    label = { Text(style.name) },
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Text("Tapping Invert Mode", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            TappingInvertMode.values().forEach { mode ->
                FilterChip(
                    selected = tappingInvertMode == mode,
                    onClick = { viewModel.readerPrefs.setTappingInvertMode(mode) },
                    label = { Text(mode.name) },
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Full Screen", modifier = Modifier.weight(1f))
            Switch(checked = fullScreen, onCheckedChange = { viewModel.readerPrefs.setFullScreen(it) })
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Keep Screen On", modifier = Modifier.weight(1f))
            Switch(checked = keepScreenOn, onCheckedChange = { viewModel.readerPrefs.setKeepScreenOn(it) })
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Page Indicator", modifier = Modifier.weight(1f))
            Switch(checked = showPageIndicator, onCheckedChange = { viewModel.readerPrefs.setShowPageIndicator(it) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorFilterTab(viewModel: MangaReaderViewModel) {
    val colorFilterMode by viewModel.readerPrefs.colorFilter.collectAsState()
    val brightness by viewModel.readerPrefs.brightness.collectAsState()
    val colorFilterAlpha by viewModel.readerPrefs.colorFilterAlpha.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Color Filter Mode", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            ColorFilterMode.values().forEach { mode ->
                FilterChip(
                    selected = colorFilterMode == mode,
                    onClick = { viewModel.readerPrefs.setColorFilter(mode) },
                    label = { Text(mode.name) },
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Text("Brightness", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        Slider(
            value = brightness,
            onValueChange = { viewModel.readerPrefs.setBrightness(it) },
            valueRange = 0f..2f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )
        Text("${String.format("%.1f", brightness)}x", modifier = Modifier.align(Alignment.CenterHorizontally))

        if (colorFilterMode != ColorFilterMode.None) {
            Text("Filter Opacity", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            Slider(
                value = colorFilterAlpha,
                onValueChange = { viewModel.readerPrefs.setColorFilterAlpha(it) },
                valueRange = 0f..1f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
            Text("${String.format("%.0f", colorFilterAlpha * 100)}%", modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
