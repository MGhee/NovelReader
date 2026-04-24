package my.novelreader.features.mangareader.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import my.novelreader.features.mangareader.MangaReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageActionsSheet(
    viewModel: MangaReaderViewModel,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text("Save Page") },
                leadingContent = {
                    Icon(Icons.Outlined.Download, contentDescription = "Save")
                },
                modifier = Modifier.fillMaxWidth()
            )

            ListItem(
                headlineContent = { Text("Share Page") },
                leadingContent = {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                },
                modifier = Modifier.fillMaxWidth()
            )

            ListItem(
                headlineContent = { Text("Set as Cover") },
                leadingContent = {
                    Icon(Icons.Outlined.Image, contentDescription = "Set Cover")
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
