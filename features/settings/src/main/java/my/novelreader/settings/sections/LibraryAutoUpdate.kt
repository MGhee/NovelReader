package my.novelreader.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
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
import my.novelreader.coreui.theme.colorApp
import my.novelreader.coreui.theme.InternalTheme
import my.novelreader.coreui.theme.textPadding
import my.novelreader.core.domain.AppVersion
import my.novelreader.core.domain.RemoteAppVersion
import my.novelreader.settings.R
import my.novelreader.settings.SettingsScreenState
import my.novelreader.settings.views.NewAppUpdateDialog

@Composable
fun LibraryAutoUpdate(
    state: SettingsScreenState.LibraryAutoUpdate,
) {
    Column {
        Text(
            text = "Library updates",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.textPadding(),
            color = MaterialTheme.colorApp.accent
        )
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

@Preview
@Composable
private fun PreviewView() {
    InternalTheme {
        NewAppUpdateDialog(
            updateApp = SettingsScreenState.UpdateApp(
                currentAppVersion = "1.2.3",
                appUpdateCheckerEnabled = remember { mutableStateOf(true) },
                showNewVersionDialog = remember {
                    mutableStateOf(
                        RemoteAppVersion(
                            sourceUrl = "url",
                            version = AppVersion(1, 4, 5)
                        )
                    )
                },
                checkingForNewVersion = remember { mutableStateOf(true) }
            )
        )
    }
}
