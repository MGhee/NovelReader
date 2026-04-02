package my.novelreader.libraryexplorer


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.novelreader.coreui.components.BookImageButtonView
import my.novelreader.coreui.theme.colorApp
import my.novelreader.coreui.components.BookTitlePosition
import my.novelreader.coreui.modifiers.bounceOnPressed
import my.novelreader.core.rememberResolvedBookImagePath
import my.novelreader.feature.local_database.BookWithContext

@Composable
internal fun LibraryPageBody(
    list: List<BookWithContext>,
    onClick: (BookWithContext) -> Unit,
    onMenuClick: (BookWithContext) -> Unit,
    downloadProgress: Map<String, Pair<Int, Int>> = emptyMap(),
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(100.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 400.dp, start = 12.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = list,
            key = { it.book.url }
        ) { book ->
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

                // New chapters badge
                if (book.newChaptersCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 4.dp, top = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.tertiary,
                                CircleShape
                            )
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
    }
}

@Composable
internal fun ContinueReadingBanner(
    book: BookWithContext,
    chapterTitle: String?,
    chapterPosition: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorApp.tintedSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
            BookImageButtonView(
                title = book.book.title,
                coverImageModel = rememberResolvedBookImagePath(
                    bookUrl = book.book.url,
                    imagePath = book.book.coverImageUrl
                ),
                bookTitlePosition = BookTitlePosition.Hidden,
                onClick = onClick,
                modifier = Modifier
                    .size(width = 60.dp, height = 85.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.resume_reading),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = book.book.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (chapterTitle != null) {
                    Text(
                        text = chapterTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (book.chaptersCount > 0 && chapterPosition > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { chapterPosition.toFloat() / book.chaptersCount },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "$chapterPosition/${book.chaptersCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
}
