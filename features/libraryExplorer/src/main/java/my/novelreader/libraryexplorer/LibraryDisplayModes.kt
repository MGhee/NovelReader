package my.novelreader.libraryexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.novelreader.core.rememberResolvedBookImagePath
import my.novelreader.coreui.components.BookImageButtonView
import my.novelreader.coreui.components.BookTitlePosition
import my.novelreader.coreui.modifiers.bounceOnPressed
import my.novelreader.coreui.theme.colorApp
import my.novelreader.data.LibraryDisplayMode
import my.novelreader.feature.local_database.BookWithContext

/**
 * Displays library with the selected display mode (CompactGrid, ComfortableGrid, or List)
 */
@Composable
internal fun LibraryDisplayModeContent(
    list: List<BookWithContext>,
    displayMode: LibraryDisplayMode,
    onClick: (BookWithContext) -> Unit,
    onMenuClick: (BookWithContext) -> Unit,
    downloadProgress: Map<String, Pair<Int, Int>> = emptyMap(),
) {
    when (displayMode) {
        LibraryDisplayMode.CompactGrid -> {
            LazyLibraryCompactGrid(list, onClick, onMenuClick, downloadProgress)
        }
        LibraryDisplayMode.ComfortableGrid -> {
            LazyLibraryComfortableGrid(list, onClick, onMenuClick, downloadProgress)
        }
        LibraryDisplayMode.List -> {
            LazyLibraryList(list, onClick, onMenuClick, downloadProgress)
        }
    }
}

/**
 * Compact grid display: small items with tight spacing
 */
@Composable
internal fun LazyLibraryCompactGrid(
    list: List<BookWithContext>,
    onClick: (BookWithContext) -> Unit,
    onMenuClick: (BookWithContext) -> Unit,
    downloadProgress: Map<String, Pair<Int, Int>> = emptyMap(),
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(90.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 400.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = list, key = { it.book.url }) { book ->
            LibraryBookItem(book, onClick, onMenuClick, downloadProgress)
        }
    }
}

/**
 * Comfortable grid display: medium items with comfortable spacing
 */
@Composable
internal fun LazyLibraryComfortableGrid(
    list: List<BookWithContext>,
    onClick: (BookWithContext) -> Unit,
    onMenuClick: (BookWithContext) -> Unit,
    downloadProgress: Map<String, Pair<Int, Int>> = emptyMap(),
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(120.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 400.dp, start = 12.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = list, key = { it.book.url }) { book ->
            LibraryBookItem(book, onClick, onMenuClick, downloadProgress)
        }
    }
}

/**
 * List display: single-column full-width items
 */
@Composable
internal fun LazyLibraryList(
    list: List<BookWithContext>,
    onClick: (BookWithContext) -> Unit,
    onMenuClick: (BookWithContext) -> Unit,
    downloadProgress: Map<String, Pair<Int, Int>> = emptyMap(),
) {
    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp, bottom = 400.dp, start = 12.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        lazyItems(items = list, key = { it.book.url }) { book ->
            LibraryBookListItem(book, onClick, onMenuClick, downloadProgress)
        }
    }
}

/**
 * Grid item for library book (CompactGrid and ComfortableGrid)
 */
@Composable
internal fun LibraryBookItem(
    book: BookWithContext,
    onClick: (BookWithContext) -> Unit,
    onMenuClick: (BookWithContext) -> Unit,
    downloadProgress: Map<String, Pair<Int, Int>> = emptyMap(),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val progress = downloadProgress[book.book.url]
    val isDownloading = progress != null

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            BookImageButtonView(
                title = book.book.title,
                coverImageModel = rememberResolvedBookImagePath(
                    bookUrl = book.book.url,
                    imagePath = book.book.coverImageUrl
                ),
                bookTitlePosition = BookTitlePosition.Outside,
                onClick = { onClick(book) },
                interactionSource = interactionSource,
                modifier = Modifier.bounceOnPressed(interactionSource)
            )

            // Progress bar overlaid at bottom of image when downloading
            if (isDownloading && progress != null) {
                val (downloaded, total) = progress
                val progressValue = if (total > 0) downloaded.toFloat() / total else 0f
                val percentage = (progressValue * 100).toInt()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 51.dp, start = 4.dp, end = 4.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    LinearProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        color = MaterialTheme.colorApp.accent,
                        trackColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                    Text(
                        text = "$percentage%",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        // Badges
        LibraryBadges(book, Modifier.align(Alignment.TopStart))

        // Menu button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp, top = 8.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    CircleShape
                )
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(id = R.string.open_for_more_options),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clickable { onMenuClick(book) }
                    .padding(2.dp)
            )
        }
    }
}

/**
 * List item for library book (List mode)
 */
@Composable
internal fun LibraryBookListItem(
    book: BookWithContext,
    onClick: (BookWithContext) -> Unit,
    onMenuClick: (BookWithContext) -> Unit,
    downloadProgress: Map<String, Pair<Int, Int>> = emptyMap(),
) {
    val progress = downloadProgress[book.book.url]
    val isDownloading = progress != null
    val (downloaded, total) = progress ?: Pair(0, 0)
    val progressValue = if (total > 0) downloaded.toFloat() / total else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(book) }
            .padding(12.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover image
        Box {
            BookImageButtonView(
                title = book.book.title,
                coverImageModel = rememberResolvedBookImagePath(
                    bookUrl = book.book.url,
                    imagePath = book.book.coverImageUrl
                ),
                bookTitlePosition = BookTitlePosition.Hidden,
                onClick = { onClick(book) },
                modifier = Modifier.size(width = 60.dp, height = 85.dp)
            )

            // Badges
            if (book.newChaptersCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 4.dp, top = 4.dp)
                        .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (book.newChaptersCount > 99) "99+" else "${book.newChaptersCount}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.onTertiary,
                        maxLines = 1,
                    )
                }
            }
        }

        // Book info
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Text(
                text = book.book.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )

            // Reading progress
            if (book.chaptersCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (book.chaptersReadCount / book.chaptersCount).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${book.chaptersReadCount}/${book.chaptersCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Download progress
            if (isDownloading) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
            }
        }

        // Menu button
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = stringResource(id = R.string.open_for_more_options),
            modifier = Modifier.clickable { onMenuClick(book) }
        )
    }
}

/**
 * Badges component for library items (new chapters badge, etc.)
 */
@Composable
internal fun LibraryBadges(
    book: BookWithContext,
    modifier: Modifier = Modifier
) {
    if (book.newChaptersCount > 0) {
        Box(
            modifier = modifier
                .padding(start = 4.dp, top = 4.dp)
                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                .size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (book.newChaptersCount > 99) "99+" else "${book.newChaptersCount}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onTertiary,
                maxLines = 1,
            )
        }
    }
}
