package my.novelreader.catalogexplorer

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.novelreader.coreui.components.BookImageButtonView
import my.novelreader.coreui.components.BookTitlePosition
import my.novelreader.coreui.composableActions.ListLoadWatcher
import my.novelreader.coreui.modifiers.bounceOnPressed
import my.novelreader.coreui.states.IteratorState
import my.novelreader.coreui.states.PagedListIteratorState
import my.novelreader.coreui.theme.colorApp
import my.novelreader.core.rememberResolvedBookImagePath
import my.novelreader.core.Response
import my.novelreader.scraper.domain.BookResult
import my.novelreader.feature.local_database.BookMetadata
import my.novelreader.mappers.mapToBookMetadata

@Composable
internal fun SourcePopularCarousel(
    fetchIterator: PagedListIteratorState<BookResult>,
    onBookClick: (book: BookMetadata) -> Unit,
    modifier: Modifier = Modifier,
    fetchCoverUrl: (suspend (bookUrl: String) -> String?)? = null
) {
    val state = rememberLazyListState()

    ListLoadWatcher(listState = state, loadState = fetchIterator.state, onLoadNext = { fetchIterator.fetchNext() })

    LazyRow(
        state = state,
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 30.dp,
        ),
        modifier = modifier,
    ) {
        items(fetchIterator.list) {
            val interactionSource = remember { MutableInteractionSource() }
            var resolvedCover by remember(it.url) { mutableStateOf(it.coverImageUrl) }

            if (resolvedCover.isBlank() && fetchCoverUrl != null) {
                LaunchedEffect(it.url) {
                    fetchCoverUrl(it.url)?.let { url -> resolvedCover = url }
                }
            }

            BookImageButtonView(
                title = it.title,
                coverImageModel = rememberResolvedBookImagePath(
                    bookUrl = it.url,
                    imagePath = resolvedCover
                ),
                onClick = { onBookClick(it.mapToBookMetadata()) },
                onLongClick = { },
                modifier = Modifier
                    .width(120.dp)
                    .bounceOnPressed(interactionSource),
                bookTitlePosition = BookTitlePosition.Inside,
                interactionSource = interactionSource
            )
        }

        item {
            fun Modifier.topPadding() = padding(top = (120 / 1.45f).dp - 8.dp)

            Box(
                contentAlignment = Alignment.TopStart,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                when (fetchIterator.state) {
                    IteratorState.LOADING -> CircularProgressIndicator(
                        color = MaterialTheme.colorApp.accent,
                        modifier = Modifier.padding(36.dp)
                    )

                    IteratorState.CONSUMED -> when {
                        fetchIterator.error != null -> Text(
                            text = stringResource(R.string.error_loading),
                            color = MaterialTheme.colorScheme.error,
                            modifier = if (fetchIterator.list.isEmpty()) Modifier else Modifier.topPadding()
                        )

                        fetchIterator.list.isEmpty() -> Text(
                            text = stringResource(R.string.no_results_found),
                            color = MaterialTheme.colorApp.accent,
                        )

                        else -> Text(
                            text = stringResource(R.string.no_more_results),
                            color = MaterialTheme.colorApp.accent,
                            modifier = Modifier.topPadding()
                        )
                    }

                    IteratorState.IDLE -> {}
                }
            }
        }
    }
}
