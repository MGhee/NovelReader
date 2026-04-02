package my.novelreader.features.reader.ui.settingDialogs

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.novelreader.core.appPreferences.ReaderOrientation
import my.novelreader.coreui.theme.colorApp
import my.novelreader.reader.R

@Composable
internal fun MoreSettingDialog(
    allowTextSelection: Boolean,
    onAllowTextSelectionChange: (Boolean) -> Unit,
    fullScreen: Boolean,
    onFullScreen: (Boolean) -> Unit,
    orientation: ReaderOrientation,
    onOrientationChange: (ReaderOrientation) -> Unit,
    onOpenChaptersList: () -> Unit,
) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
    ) {
        // Allow text selection
        ListItem(
            modifier = Modifier
                .clickable { onAllowTextSelectionChange(!allowTextSelection) },
            headlineContent = {
                Text(text = stringResource(id = R.string.allow_text_selection))
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.TouchApp,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            },
            trailingContent = {
                Switch(
                    checked = allowTextSelection,
                    onCheckedChange = onAllowTextSelectionChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorApp.accent,
                        checkedBorderColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    )
                )
            }
        )
        // Full screen
        ListItem(
            modifier = Modifier
                .clickable { onFullScreen(!fullScreen) },
            headlineContent = {
                Text(text = stringResource(R.string.features_reader_full_screen))
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.Fullscreen,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            },
            trailingContent = {
                Switch(
                    checked = fullScreen,
                    onCheckedChange = onFullScreen,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorApp.accent,
                        checkedBorderColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    )
                )
            }
        )
        // Orientation
        ListItem(
            modifier = Modifier
                .clickable {
                    val newOrientation = if (orientation == ReaderOrientation.Vertical) {
                        ReaderOrientation.Horizontal
                    } else {
                        ReaderOrientation.Vertical
                    }
                    onOrientationChange(newOrientation)
                },
            headlineContent = {
                Text(text = stringResource(R.string.orientation))
            },
            supportingContent = {
                Text(
                    text = stringResource(
                        if (orientation == ReaderOrientation.Vertical) R.string.vertical else R.string.horizontal
                    ),
                    style = MaterialTheme.typography.labelSmall
                )
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.ViewWeek,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            },
            trailingContent = {
                Switch(
                    checked = orientation == ReaderOrientation.Horizontal,
                    onCheckedChange = { isHorizontal ->
                        onOrientationChange(
                            if (isHorizontal) ReaderOrientation.Horizontal else ReaderOrientation.Vertical
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorApp.accent,
                        checkedBorderColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    )
                )
            }
        )
        ListItem(
            modifier = Modifier
                .clickable(onClick = onOpenChaptersList),
            headlineContent = {
                Text(text = stringResource(R.string.chapters))
            },
            leadingContent = {
                Icon(
                    Icons.AutoMirrored.Outlined.MenuBook,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            },
        )
    }
}