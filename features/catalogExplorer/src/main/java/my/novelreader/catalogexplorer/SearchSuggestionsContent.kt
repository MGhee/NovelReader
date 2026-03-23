package my.novelreader.catalogexplorer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.novelreader.coreui.states.PagedListIteratorState
import my.novelreader.data.CatalogItem
import my.novelreader.feature.local_database.BookMetadata
import my.novelreader.scraper.domain.BookResult

data class SourceSearchResult(
    val source: CatalogItem,
    val fetchIterator: PagedListIteratorState<BookResult>
)

@Composable
internal fun SearchSuggestionsContent(
    searchResults: List<SourceSearchResult>,
    onBookClick: (book: BookMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasAnyResults = searchResults.any { it.fetchIterator.list.isNotEmpty() }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = 240.dp, start = 8.dp, end = 8.dp)
    ) {
        items(searchResults) { entry ->
            Text(
                text = stringResource(id = entry.source.catalog.nameStrId),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            SourcePopularCarousel(
                fetchIterator = entry.fetchIterator,
                onBookClick = onBookClick
            )
        }

        if (!hasAnyResults) {
            item {
                Text(
                    text = stringResource(R.string.no_results_found),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
