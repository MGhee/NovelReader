package my.novelreader.catalogexplorer

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.novelreader.coreui.components.AnimatedTransition
import my.novelreader.coreui.components.ImageViewGlide
import my.novelreader.coreui.theme.colorApp
import my.novelreader.coreui.theme.InternalTheme
import my.novelreader.coreui.theme.PreviewThemes
import my.novelreader.data.CatalogItem
import my.novelreader.scraper.DatabaseInterface
import my.novelreader.scraper.SourceInterface
import my.novelreader.scraper.fixtures.fixturesCatalogList
import my.novelreader.scraper.fixtures.fixturesDatabaseList

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun CatalogList(
    innerPadding: PaddingValues,
    databasesList: List<DatabaseInterface>,
    sourcesList: List<CatalogItem>,
    onDatabaseClick: (DatabaseInterface) -> Unit,
    onSourceClick: (SourceInterface.Catalog) -> Unit,
    onSourceSetPinned: (id: String, pinned: Boolean) -> Unit,
    getOrCreatePopularBooksIterator: (CatalogItem) -> my.novelreader.coreui.states.PagedListIteratorState<my.novelreader.scraper.domain.BookResult>,
    onBookClick: (my.novelreader.feature.local_database.BookMetadata) -> Unit,
) {
    val pinnedSources = sourcesList.filter { it.pinned }
    val unpinnedSources = sourcesList.filter { !it.pinned }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 300.dp, start = 8.dp, end = 8.dp, top = 8.dp),
        modifier = Modifier.padding(paddingValues = innerPadding)
    ) {
        // Databases section
        item {
            Text(
                text = stringResource(id = R.string.database),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorApp.accent,
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                databasesList.forEach { database ->
                    ElevatedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDatabaseClick(database) }
                    ) {
                        Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            ImageViewGlide(
                                imageModel = database.iconUrl,
                                modifier = Modifier.size(32.dp),
                                error = R.drawable.default_icon
                            )
                            Text(
                                text = stringResource(id = database.nameStrId),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Pinned sources section
        if (pinnedSources.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(id = R.string.pinned_sources),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorApp.accent,
                )
            }

            items(
                items = pinnedSources,
                key = { it.catalog.id }
            ) { catalogItem ->
                SourceCard(
                    catalogItem = catalogItem,
                    onSourceClick = onSourceClick,
                    onSourceSetPinned = onSourceSetPinned,
                    getOrCreatePopularBooksIterator = getOrCreatePopularBooksIterator,
                    onBookClick = onBookClick,
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }

        // All sources section
        if (unpinnedSources.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(id = R.string.all_sources),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorApp.accent,
                )
            }

            items(
                items = unpinnedSources,
                key = { it.catalog.id }
            ) { catalogItem ->
                SourceCard(
                    catalogItem = catalogItem,
                    onSourceClick = onSourceClick,
                    onSourceSetPinned = onSourceSetPinned,
                    getOrCreatePopularBooksIterator = getOrCreatePopularBooksIterator,
                    onBookClick = onBookClick,
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun SourceCard(
    catalogItem: CatalogItem,
    onSourceClick: (SourceInterface.Catalog) -> Unit,
    onSourceSetPinned: (id: String, pinned: Boolean) -> Unit,
    getOrCreatePopularBooksIterator: (CatalogItem) -> my.novelreader.coreui.states.PagedListIteratorState<my.novelreader.scraper.domain.BookResult>,
    onBookClick: (my.novelreader.feature.local_database.BookMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSourceClick(catalogItem.catalog) }
                    .padding(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val icon = catalogItem.catalog.iconUrl
                    if (icon is ImageVector) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                    } else {
                        ImageViewGlide(
                            imageModel = icon,
                            modifier = Modifier.size(28.dp),
                            error = R.drawable.default_icon
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = catalogItem.catalog.nameStrId),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        catalogItem.catalog.language?.let {
                            Text(
                                text = stringResource(id = it.nameResId),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                Row {
                    val catalog = catalogItem.catalog
                    if (catalog is SourceInterface.Configurable) {
                        var openConfig by rememberSaveable { mutableStateOf(false) }
                        IconButton(onClick = { openConfig = !openConfig }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.configuration),
                            )
                        }
                        if (openConfig) {
                            AlertDialog(
                                onDismissRequest = { openConfig = false },
                                confirmButton = {
                                    FilledTonalButton(onClick = { openConfig = !openConfig }) {
                                        Text(text = stringResource(R.string.close))
                                    }
                                },
                                text = { catalog.ScreenConfig() },
                                icon = {
                                    Icon(
                                        Icons.Filled.Settings,
                                        stringResource(id = R.string.configuration),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            )
                        }
                    }
                    IconButton(onClick = { onSourceSetPinned(catalogItem.catalog.id, !catalogItem.pinned) }) {
                        AnimatedTransition(targetState = catalogItem.pinned) { pinned ->
                            Icon(
                                imageVector = if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = stringResource(R.string.pin_or_unpin_source),
                            )
                        }
                    }
                }
            }

            // Popular books carousel
            Text(
                text = stringResource(R.string.popular),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 12.dp, start = 8.dp, bottom = 4.dp)
            )

            val iterator = getOrCreatePopularBooksIterator(catalogItem)
            LaunchedEffect(catalogItem.catalog.id) {
                if (iterator.list.isEmpty() && iterator.state == my.novelreader.coreui.states.IteratorState.IDLE) {
                    iterator.fetchNext()
                }
            }

            SourcePopularCarousel(
                fetchIterator = iterator,
                onBookClick = onBookClick,
                modifier = Modifier.fillMaxWidth(),
                fetchCoverUrl = { bookUrl ->
                    (catalogItem.catalog.getBookCoverImageUrl(bookUrl) as? my.novelreader.core.Response.Success)?.data
                }
            )
        }
    }
}

@PreviewThemes
@Composable
private fun PreviewView() {
    val catalogItemsList = fixturesCatalogList().mapIndexed { index, it ->
        CatalogItem(
            catalog = it,
            pinned = index % 2 == 0,
        )
    }
    val scope = rememberCoroutineScope()

    InternalTheme {
        CatalogList(
            innerPadding = PaddingValues(),
            databasesList = fixturesDatabaseList(),
            sourcesList = catalogItemsList,
            onDatabaseClick = {},
            onSourceClick = {},
            onSourceSetPinned = { _, _ -> },
            getOrCreatePopularBooksIterator = { _ ->
                my.novelreader.coreui.states.PagedListIteratorState(
                    scope
                ) { throw NotImplementedError() }
            },
            onBookClick = {},
        )
    }
}