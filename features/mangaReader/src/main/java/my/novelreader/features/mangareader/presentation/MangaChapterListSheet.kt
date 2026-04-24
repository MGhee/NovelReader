package my.novelreader.features.mangareader.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import my.novelreader.feature.local_database.tables.Chapter
import my.novelreader.features.mangareader.MangaReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaChapterListSheet(
    viewModel: MangaReaderViewModel,
    onDismiss: () -> Unit,
) {
    val chapters by viewModel.chapterList.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Chapters",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(chapters, key = { it.url }) { chapter ->
                    ChapterListItem(
                        chapter = chapter,
                        isCurrentChapter = chapter.url == currentChapter?.url,
                        onClick = {
                            viewModel.openChapter(chapter.url)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChapterListItem(
    chapter: Chapter,
    isCurrentChapter: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isCurrentChapter) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                chapter.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrentChapter) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            if (chapter.lastReadMangaPage > 0) {
                Text(
                    "Page ${chapter.lastReadMangaPage}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        if (isCurrentChapter) {
            Icon(
                Icons.Default.Done,
                contentDescription = "Current",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
