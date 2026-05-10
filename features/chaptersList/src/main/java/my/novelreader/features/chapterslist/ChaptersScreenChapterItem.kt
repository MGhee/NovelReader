package my.novelreader.features.chapterslist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import my.novelreader.core.ChapterDownloadProgress
import my.novelreader.coreui.components.AnimatedTransition
import my.novelreader.coreui.theme.ColorNotice
import my.novelreader.coreui.theme.InternalTheme
import my.novelreader.coreui.theme.PreviewThemes
import my.novelreader.coreui.theme.colorApp
import my.novelreader.chapterslist.R
import my.novelreader.feature.local_database.ChapterWithContext
import my.novelreader.feature.local_database.tables.Chapter

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
internal fun ChaptersScreenChapterItem(
    chapterWithContext: ChapterWithContext,
    selected: Boolean,
    isLocalSource: Boolean,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onBookmarkChange: (Boolean) -> Unit = {}
) {
    val chapter = chapterWithContext.chapter
    val downloadProgressMap by ChapterDownloadProgress.flow.collectAsState()
    val downloadProgress = remember(downloadProgressMap, chapter.url, chapterWithContext.downloaded) {
        when {
            chapterWithContext.downloaded -> 1f
            else -> downloadProgressMap[chapter.url] ?: 0f
        }
    }
    ListItem(
        headlineContent = {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        supportingContent = {
            AnimatedTransition(
                targetState = chapterWithContext.lastReadChapter to chapter.read,
                transitionSpec = { fadeIn() togetherWith fadeOut(tween(delayMillis = 150)) }
            ) { (lastReadPosition, read) ->
                when {
                    lastReadPosition -> Text(
                        stringResource(id = R.string.last_read),
                        color = ColorNotice
                    )
                    read -> Text(stringResource(id = R.string.read))
                    else -> Text("")
                }
            }
        },
        trailingContent = if (isLocalSource) null else {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onBookmarkChange(!chapter.bookmarked) }) {
                        Icon(
                            if (chapter.bookmarked) Icons.Filled.Bookmark
                            else Icons.Outlined.BookmarkBorder,
                            null
                        )
                    }
                    IconButton(onClick = onDownload) {
                        DownloadCloud(progress = downloadProgress)
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(
            supportingColor = MaterialTheme.colorScheme.onTertiary,
            containerColor =
            if (selected) MaterialTheme.colorApp.tintedSelectedSurface
            else MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
    )
}


/**
 * Cloud-download glyph that "fills up" as the chapter downloads. Three stacked layers:
 *  1. The outlined cloud in the row's content color — always drawn, so an empty cloud is
 *     visible at progress = 0.
 *  2. The filled cloud clipped to the bottom-up progress portion in the same content
 *     color — paints the solid "filled" body up to the current fill line.
 *  3. The outlined cloud again, but tinted in the row's surface color and clipped to the
 *     same progress portion. Its strokes (cloud edge + the inner arrow) appear in the row
 *     background color over the filled body, so the arrow reads as a "punched-out" cutout
 *     against the white fill instead of vanishing into it.
 */
@Composable
private fun DownloadCloud(progress: Float) {
    val cloudTint = LocalContentColor.current
    val cutoutTint = MaterialTheme.colorScheme.surface
    val clamped = progress.coerceIn(0f, 1f)
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Outlined.CloudDownload,
            contentDescription = null,
            tint = cloudTint,
        )
        if (clamped > 0f) {
            val clipModifier = Modifier.drawWithContent {
                clipRect(top = size.height * (1f - clamped)) {
                    this@drawWithContent.drawContent()
                }
            }
            Icon(
                imageVector = Icons.Filled.CloudDownload,
                contentDescription = null,
                tint = cloudTint,
                modifier = clipModifier,
            )
            Icon(
                imageVector = Icons.Outlined.CloudDownload,
                contentDescription = null,
                tint = cutoutTint,
                modifier = clipModifier,
            )
        }
    }
}

@PreviewThemes
@Composable
private fun PreviewView(
    @PreviewParameter(PreviewProvider::class) previewProviderState: PreviewProviderState
) {
    InternalTheme {
        ChaptersScreenChapterItem(
            chapterWithContext = previewProviderState.chapterWithContext,
            selected = previewProviderState.selected,
            isLocalSource = false,
            onLongClick = {},
            onClick = {},
            onDownload = {},
            onBookmarkChange = {}
        )
    }
}


private data class PreviewProviderState(
    val chapterWithContext: ChapterWithContext,
    val selected: Boolean
)

private class PreviewProvider : PreviewParameterProvider<PreviewProviderState> {
    override val values = sequenceOf(
        PreviewProviderState(
            chapterWithContext = ChapterWithContext(
                chapter = Chapter(
                    title = "Title of the chapter",
                    url = "url",
                    bookUrl = "bookUrl",
                    lastReadOffset = 0,
                    lastReadPosition = 0,
                    position = 0,
                    read = false
                ),
                downloaded = false,
                lastReadChapter = false,
            ),
            selected = false
        ),
        PreviewProviderState(
            chapterWithContext = ChapterWithContext(
                chapter = Chapter(
                    title = "Title of the chapter, Title of the chapter, Title of the chapter, Title of the chapter, Title of the chapter,Title of the chapter ,Title of the chapter",
                    url = "url",
                    bookUrl = "bookUrl",
                    lastReadOffset = 0,
                    lastReadPosition = 0,
                    position = 0,
                    read = true
                ),
                downloaded = true,
                lastReadChapter = false,
            ),
            selected = false
        ),
        PreviewProviderState(
            chapterWithContext = ChapterWithContext(
                chapter = Chapter(
                    title = "Title of the chapter",
                    url = "url",
                    bookUrl = "bookUrl",
                    lastReadOffset = 0,
                    lastReadPosition = 0,
                    position = 0,
                    read = false
                ),
                downloaded = true,
                lastReadChapter = true,
            ),
            selected = true
        )
    )
}









