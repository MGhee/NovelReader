package my.novelreader.libraryexplorer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.novelreader.coreui.components.ImageView
import my.novelreader.coreui.theme.ImageBorderShape
import my.novelreader.core.rememberResolvedBookImagePath
import my.novelreader.feature.local_database.BookWithContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryBookOptionsSheet(
    book: BookWithContext,
    onDismiss: () -> Unit,
    onDownloadChapters: () -> Unit,
    onReDownloadChapters: () -> Unit,
    onDeleteDownloadedChapters: () -> Unit,
    onDeleteBook: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        // Header with book cover and title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            ImageView(
                imageModel = rememberResolvedBookImagePath(
                    bookUrl = book.book.url,
                    imagePath = book.book.coverImageUrl
                ),
                error = R.drawable.default_book_cover,
                modifier = Modifier
                    .width(64.dp)
                    .aspectRatio(1 / 1.45f)
                    .clip(ImageBorderShape)
            )
            Text(
                text = book.book.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            )
        }

        // Menu options
        ListItem(
            headlineContent = { Text(stringResource(R.string.download_chapters)) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.CloudDownload,
                    contentDescription = null
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onDismiss()
                    onDownloadChapters()
                }
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.re_download_chapters)) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onDismiss()
                    onReDownloadChapters()
                }
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.delete_downloaded_chapters)) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = null
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onDismiss()
                    onDeleteDownloadedChapters()
                }
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.delete_book)) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onDismiss()
                    onDeleteBook()
                }
        )
    }
}
