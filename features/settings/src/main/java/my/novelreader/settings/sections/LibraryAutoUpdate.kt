package my.novelreader.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.novelreader.coreui.theme.colorApp
import my.novelreader.coreui.theme.InternalTheme
import my.novelreader.coreui.theme.textPadding
import my.novelreader.settings.R
import my.novelreader.settings.SettingsScreenState

private val INTERVAL_OPTIONS = listOf(6, 12, 24)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryAutoUpdate(
    state: SettingsScreenState.LibraryAutoUpdate,
) {
    Column {
        Text(
            text = stringResource(R.string.library_updates),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.textPadding(),
            color = MaterialTheme.colorApp.accent
        )
        // Auto-update toggle
        ListItem(
            modifier = Modifier.clickable {
                state.autoUpdateEnabled.value = !state.autoUpdateEnabled.value
            },
            headlineContent = {
                Text(text = stringResource(R.string.automatically_update_library))
            },
            supportingContent = {
                Text(text = stringResource(R.string.automatically_update_library_description))
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.Update,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            },
            trailingContent = {
                Switch(
                    checked = state.autoUpdateEnabled.value,
                    onCheckedChange = { state.autoUpdateEnabled.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorApp.accent,
                        checkedBorderColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    )
                )
            }
        )
        // Interval picker (only meaningful when auto-update is enabled)
        if (state.autoUpdateEnabled.value) {
            ListItem(
                headlineContent = {
                    Text(text = stringResource(R.string.library_update_interval))
                },
                supportingContent = {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        INTERVAL_OPTIONS.forEach { hours ->
                            FilterChip(
                                selected = state.autoUpdateIntervalHours.value == hours,
                                onClick = { state.autoUpdateIntervalHours.value = hours },
                                label = { Text(text = stringResource(intervalLabel(hours))) }
                            )
                        }
                    }
                },
                leadingContent = {
                    Icon(
                        Icons.Outlined.Schedule,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            )
        }
        // Auto-download new chapters option
        ListItem(
            modifier = Modifier.clickable {
                state.autoDownloadNewChapters.value = !state.autoDownloadNewChapters.value
            },
            headlineContent = {
                Text(text = stringResource(R.string.automatically_download_new_chapters))
            },
            supportingContent = {
                Text(text = stringResource(R.string.automatically_download_new_chapters_description))
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.CloudDownload,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            },
            trailingContent = {
                Switch(
                    checked = state.autoDownloadNewChapters.value,
                    onCheckedChange = {
                        state.autoDownloadNewChapters.value = !state.autoDownloadNewChapters.value
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorApp.accent,
                        checkedBorderColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    )
                )
            }
        )
    }
}

private fun intervalLabel(hours: Int): Int = when (hours) {
    6 -> R.string.library_update_interval_6h
    12 -> R.string.library_update_interval_12h
    else -> R.string.library_update_interval_24h
}

@Preview
@Composable
private fun PreviewView() {
    InternalTheme {
        LibraryAutoUpdate(
            state = SettingsScreenState.LibraryAutoUpdate(
                autoDownloadNewChapters = remember { mutableStateOf(false) },
                autoUpdateEnabled = remember { mutableStateOf(true) },
                autoUpdateIntervalHours = remember { mutableStateOf(24) },
            )
        )
    }
}
