package my.novelreader.features.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.novelreader.coreui.theme.ColorAccent
import my.novelreader.coreui.theme.Success400
import my.novelreader.feature.local_database.ChapterWithContext
import my.novelreader.reader.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderChapterListScreen(
    chapters: List<ChapterWithContext>,
    currentChapterUrl: String,
    onChapterClick: (ChapterWithContext) -> Unit,
    onDownloadAllClick: () -> Unit,
    onDeleteAllClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortAscending by remember { mutableStateOf(true) }

    // Calculate initial scroll position to show current chapter centered
    val initialIndex = remember(chapters, currentChapterUrl) {
        val currentIndex = chapters.indexOfFirst { it.chapter.url == currentChapterUrl }
        if (currentIndex >= 0) maxOf(0, currentIndex - 4) else 0
    }
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    // Filter and sort chapters
    val filteredChapters = remember(chapters, searchQuery, sortAscending) {
        chapters
            .filter { chapter ->
                chapter.chapter.title.contains(searchQuery, ignoreCase = true) ||
                        "Chapter ${chapter.chapter.position + 1}".contains(searchQuery, ignoreCase = true)
            }
            .sortedBy { if (sortAscending) it.chapter.position else -it.chapter.position }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top Bar
        TopAppBar(
            title = { Text(stringResource(id = R.string.chapter_list), style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            },
            actions = {
                IconButton(onClick = onDownloadAllClick) {
                    Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.download_all_chapters))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
        )

        // Search Bar - Compact, extends to edges with top separation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Search, null, modifier = Modifier.padding(end = 4.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (searchQuery.isEmpty()) {
                            Text(stringResource(id = R.string.search_here), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        innerTextField()
                    }
                }
            )

            // Sort Button
            IconButton(onClick = { sortAscending = !sortAscending }, modifier = Modifier.padding(0.dp)) {
                Icon(Icons.Filled.SwapVert, null)
            }
        }

        // Chapters List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp),
            state = lazyListState
        ) {
            items(filteredChapters.size) { index ->
                val chapter = filteredChapters[index]
                val isCurrentChapter = chapter.chapter.url == currentChapterUrl
                ListItem(
                    headlineContent = { Text(chapter.chapter.title, style = MaterialTheme.typography.bodyMedium) },
                    supportingContent = { Text("Chapter ${chapter.chapter.position + 1}", style = MaterialTheme.typography.labelSmall) },
                    leadingContent = {
                        if (chapter.downloaded) {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = null,
                                tint = Success400,
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = if (isCurrentChapter) ColorAccent else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 0.dp)
                        .clickable { onChapterClick(chapter) }
                )
                // Divider between chapters
                if (index < filteredChapters.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}
