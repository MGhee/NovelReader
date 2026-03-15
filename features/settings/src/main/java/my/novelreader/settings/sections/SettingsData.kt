package my.novelreader.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DataArray
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.novelreader.coreui.theme.ColorAccent
import my.novelreader.coreui.theme.textPadding
import my.novelreader.settings.R

@Composable
internal fun SettingsData(
    databaseSize: String,
    imagesFolderSize: String,
    onCleanDatabase: () -> Unit,
    onCleanImageFolder: () -> Unit,
    onSyncWithServer: () -> Unit = {},
    syncApiKey: String = "",
    onSyncApiKeyChange: (String) -> Unit = {}
) {
    Column {
        Text(
            text = stringResource(id = R.string.data),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.textPadding(),
            color = ColorAccent
        )
        ListItem(
            headlineContent = {
                Text(text = stringResource(R.string.clean_database))
            },
            supportingContent = {
                Column {
                    Text(text = stringResource(id = R.string.size) + " " + databaseSize)
                }
            },
            leadingContent = {
                Icon(Icons.Outlined.DataArray, null, tint = MaterialTheme.colorScheme.onPrimary)
            },
            modifier = Modifier.clickable { onCleanDatabase() }
        )
        ListItem(
            headlineContent = {
                Text(text = stringResource(R.string.clean_images_folder))
            },
            supportingContent = {
                Column {
                    Text(text = stringResource(id = R.string.preserve_only_images_from_library_books))
                    Text(text = stringResource(id = R.string.size) + " " + imagesFolderSize)
                }
            },
            leadingContent = {
                Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.onPrimary)
            },
            modifier = Modifier.clickable { onCleanImageFolder() }
        )
        ListItem(
            headlineContent = {
                Text(text = "Sync with web app")
            },
            supportingContent = {
                Text(text = "Push library and reading progress to NovelApp web server")
            },
            leadingContent = {
                Icon(Icons.Outlined.CloudSync, null, tint = MaterialTheme.colorScheme.onPrimary)
            },
            modifier = Modifier.clickable { onSyncWithServer() }
        )
        Spacer(modifier = Modifier.height(8.dp))
        ListItem(
            headlineContent = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Sync API Key (optional)")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = syncApiKey,
                        onValueChange = { onSyncApiKeyChange(it) },
                        label = { Text("Sync API Key (optional)") },
                        placeholder = { Text("Enter API key for sync server") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        )
    }
}