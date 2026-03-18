package my.novelreader.features.reader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.novelreader.reader.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderBookInfoSheet(
    bookTitle: String,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onDismiss: () -> Unit,
    onOpenChapterList: () -> Unit,
    onDownloadAllChapters: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Book Title
            Text(
                text = bookTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Chapter List option
            ListItem(
                headlineContent = { Text(stringResource(id = R.string.chapter_list)) },
                leadingContent = { Icon(Icons.Filled.List, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenChapterList()
                        onDismiss()
                    }
            )

            // Download All Chapters option
            ListItem(
                headlineContent = { Text(stringResource(id = R.string.download_all_chapters)) },
                leadingContent = { Icon(Icons.Filled.Download, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDownloadAllChapters()
                        onDismiss()
                    }
            )
        }
    }
}
