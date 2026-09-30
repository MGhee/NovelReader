package my.novelreader.libraryexplorer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import my.novelreader.feature.local_database.tables.Book

@Composable
internal fun ExportEpubBookPickerDialog(
    books: List<Book>,
    onBookSelected: (Book) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.export_epub)) },
        text = {
            if (books.isEmpty()) {
                Text(text = stringResource(R.string.none_found))
            } else LazyColumn {
                items(books, key = { it.url }) { book ->
                    ListItem(
                        headlineContent = {
                            Text(text = book.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onBookSelected(book) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.cancel)) }
        }
    )
}
