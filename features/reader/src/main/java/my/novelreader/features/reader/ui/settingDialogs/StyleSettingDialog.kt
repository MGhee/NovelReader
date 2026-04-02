package my.novelreader.features.reader.ui.settingDialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import my.novelreader.core.appPreferences.ReaderLineSpacingLevel
import my.novelreader.core.appPreferences.ReaderMarginLevel
import my.novelreader.core.appPreferences.ReaderOrientation
import my.novelreader.coreui.theme.colorApp
import my.novelreader.coreui.theme.Themes
import my.novelreader.features.reader.tools.FontsLoader
import my.novelreader.features.reader.ui.ReaderScreenState
import my.novelreader.reader.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StyleSettingDialog(
    state: ReaderScreenState.Settings.StyleSettingsData,
    onTextSizeChange: (Float) -> Unit,
    onTextFontChange: (String) -> Unit,
    onFollowSystemChange: (Boolean) -> Unit,
    onThemeChange: (Themes) -> Unit,
    onReaderThemeChange: (Themes) -> Unit = {},
    onTextIndentChange: (Boolean) -> Unit,
    onMarginLevelChange: (ReaderMarginLevel) -> Unit,
    onLineSpacingLevelChange: (ReaderLineSpacingLevel) -> Unit,
    onLineBreakHeightChange: (Int) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onOrientationChange: (ReaderOrientation) -> Unit,
    onClose: () -> Unit = {},
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable { onClose() }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.BottomCenter)
                    .clickable(enabled = false) { },
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Background - 5 theme buttons
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(id = R.string.background),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        if (state.isDynamicColorActive.value) {
                            Text(
                                text = "Reading background (app colors follow your book)",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .alpha(0.7f),
                                fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.85f
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Themes.entries.forEach { theme ->
                                val isSelected = theme == if (state.isDynamicColorActive.value)
                                    state.readerTheme.value else state.currentTheme.value
                                val bgColor = when (theme) {
                                    Themes.LIGHT -> Color(0xFFF5F5F5)
                                    Themes.DARK -> Color(0xFF303030)
                                    Themes.BLACK -> Color(0xFF000000)
                                    Themes.DARK_TEAL -> Color(0xFF0A3D4A)
                                    Themes.SEPIA -> Color(0xFFF4E5D3)
                                }
                                val textColor = when (theme) {
                                    Themes.LIGHT, Themes.SEPIA -> Color(0xFF333333)
                                    else -> Color(0xFFEEEEEE)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(bgColor, shape = MaterialTheme.shapes.small)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorApp.accent else Color.Gray,
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .clickable {
                                            if (state.isDynamicColorActive.value)
                                                onReaderThemeChange(theme)
                                            else
                                                onThemeChange(theme)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "T",
                                        color = textColor,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorApp.accent,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .align(Alignment.TopEnd)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Font Size - Stepper
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(id = R.string.font_size),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (state.textSize.value > 8f) onTextSizeChange(state.textSize.value - 1) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("-", style = MaterialTheme.typography.headlineSmall)
                            }
                            Text(
                                text = state.textSize.value.toInt().toString(),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(8.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            IconButton(
                                onClick = { if (state.textSize.value < 40f) onTextSizeChange(state.textSize.value + 1) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+", style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }

                    // Font Family - FlowRow of chips
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(id = R.string.font_family),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val fontLoader = remember { FontsLoader() }
                            FontsLoader.availableFonts.forEach { font ->
                                FilterChip(
                                    selected = font == state.textFont.value,
                                    onClick = { onTextFontChange(font) },
                                    label = {
                                        Text(
                                            text = font,
                                            fontFamily = fontLoader.getFontFamily(font),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Margin
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(id = R.string.margin),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReaderMarginLevel.entries.forEach { level ->
                                val isSelected = level == state.marginLevel.value
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onMarginLevelChange(level) },
                                    label = { Text(level.toString()) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Line Spacing
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(id = R.string.line_spacing),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReaderLineSpacingLevel.entries.forEach { level ->
                                val isSelected = level == state.lineSpacingLevel.value
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onLineSpacingLevelChange(level) },
                                    label = { Text(level.toString()) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Line Break Height
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(id = R.string.line_break_height),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(10, 20, 30, 40).forEach { height ->
                                FilterChip(
                                    selected = height == state.lineBreakHeight.value,
                                    onClick = { onLineBreakHeightChange(height) },
                                    label = { Text(height.toString()) }
                                )
                            }
                        }
                    }

                    // Checkboxes
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Keep Screen On
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onKeepScreenOnChange(!state.keepScreenOn.value) },
                            headlineContent = { Text(stringResource(id = R.string.keep_screen_on)) },
                            trailingContent = {
                                Checkbox(
                                    checked = state.keepScreenOn.value,
                                    onCheckedChange = onKeepScreenOnChange,
                                    colors = androidx.compose.material3.CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorApp.accent
                                    )
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        // Use Text Indent
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTextIndentChange(!state.textIndent.value) },
                            headlineContent = { Text(stringResource(id = R.string.use_text_indent)) },
                            trailingContent = {
                                Checkbox(
                                    checked = state.textIndent.value,
                                    onCheckedChange = onTextIndentChange,
                                    colors = androidx.compose.material3.CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorApp.accent
                                    )
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        // Orientation
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newOrientation = when (state.orientation.value) {
                                        ReaderOrientation.Vertical -> ReaderOrientation.Horizontal
                                        ReaderOrientation.Horizontal -> ReaderOrientation.Vertical
                                    }
                                    onOrientationChange(newOrientation)
                                },
                            headlineContent = { Text(stringResource(id = R.string.orientation)) },
                            supportingContent = {
                                Text(
                                    when (state.orientation.value) {
                                        ReaderOrientation.Vertical -> stringResource(id = R.string.vertical)
                                        ReaderOrientation.Horizontal -> stringResource(id = R.string.horizontal)
                                    }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        // Follow System (moved from old position)
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFollowSystemChange(!state.followSystem.value) },
                            headlineContent = { Text(stringResource(id = R.string.follow_system)) },
                            trailingContent = {
                                Switch(
                                    checked = state.followSystem.value,
                                    onCheckedChange = onFollowSystemChange,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorApp.accent,
                                    )
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}
