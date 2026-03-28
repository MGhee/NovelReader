package my.novelreader.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DataArray
import androidx.compose.material.icons.outlined.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    isLoggedIn: Boolean = false,
    syncUserEmail: String = "",
    onSignInWithGoogle: () -> Unit = {},
    onSignOut: () -> Unit = {},
    isSyncSigningIn: Boolean = false,
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
            modifier = Modifier.clickable(enabled = isLoggedIn) { onSyncWithServer() }
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (isLoggedIn) {
            // User is logged in - show account info and sign out button
            ListItem(
                headlineContent = {
                    Text(text = "Google Account")
                },
                supportingContent = {
                    Text(text = syncUserEmail)
                }
            )
            ListItem(
                headlineContent = {
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
                    ) {
                        Icon(Icons.Outlined.Logout, null, modifier = Modifier.padding(end = 8.dp))
                        Text("Sign Out")
                    }
                }
            )
        } else {
            // User is not logged in - show sign in button
            ListItem(
                headlineContent = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedButton(
                            onClick = onSignInWithGoogle,
                            enabled = !isSyncSigningIn,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
                        ) {
                            if (isSyncSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .height(20.dp)
                                        .padding(end = 8.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            Text("Sign in with Google")
                        }
                    }
                }
            )
        }
    }
}