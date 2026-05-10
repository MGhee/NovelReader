package my.novelreader.catalogexplorer

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import my.novelreader.coreui.components.AnimatedTransition
import my.novelreader.coreui.components.CollapsibleDivider
import my.novelreader.coreui.components.TopAppBarSearch
import my.novelreader.coreui.components.ToolbarMode
import my.novelreader.navigation.NavigationRouteViewModel
import my.novelreader.scraper.LightNovelSourceInterface
import my.novelreader.scraper.MangaSourceInterface
import my.novelreader.strings.R as StringsR

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CatalogExplorerScreen(
    navigationRouteViewModel: NavigationRouteViewModel = viewModel()
) {
    val viewModel: CatalogExplorerViewModel = viewModel()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSearch()
        }
    }

    val context by rememberUpdatedState(newValue = LocalContext.current)
    var languagesOptionsExpanded by rememberSaveable { mutableStateOf(false) }
    var toolbarMode by rememberSaveable { mutableStateOf(ToolbarMode.MAIN) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(viewModel.searchMode) {
        if (!viewModel.searchMode && toolbarMode != ToolbarMode.MAIN) {
            toolbarMode = ToolbarMode.MAIN
        }
    }

    val focusRequester = FocusRequester()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        snapAnimationSpec = null,
        flingAnimationSpec = null
    )

    val tabTitles = listOf(
        stringResource(StringsR.string.finder_tab_novels),
        stringResource(StringsR.string.finder_tab_light_novels),
        stringResource(StringsR.string.finder_tab_manga),
    )

    // Filter sources by tab selection
    val filteredSources = viewModel.sourcesList.filter { item ->
        when (selectedTab) {
            0 -> item.catalog !is MangaSourceInterface && item.catalog !is LightNovelSourceInterface
            1 -> item.catalog is LightNovelSourceInterface
            2 -> item.catalog is MangaSourceInterface
            else -> true
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                AnimatedTransition(targetState = toolbarMode) { mode ->
                    when (mode) {
                        ToolbarMode.MAIN -> {
                            TopAppBar(
                                scrollBehavior = scrollBehavior,
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                                ),
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.title_finder),
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                },
                                actions = {
                                    IconButton(onClick = {
                                        toolbarMode = ToolbarMode.SEARCH
                                        viewModel.searchMode = true
                                    }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_baseline_search_24),
                                            contentDescription = stringResource(R.string.search_for_title)
                                        )
                                    }
                                    IconButton(onClick = {
                                        languagesOptionsExpanded = !languagesOptionsExpanded
                                    }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_baseline_languages_24),
                                            contentDescription = stringResource(R.string.open_for_more_options)
                                        )
                                        LanguagesDropDown(
                                            expanded = languagesOptionsExpanded,
                                            languageItemList = viewModel.languagesList,
                                            onDismiss = { languagesOptionsExpanded = false },
                                            onSourceLanguageItemToggle = { viewModel.toggleSourceLanguage(it.language) }
                                        )
                                    }
                                }
                            )
                        }

                        ToolbarMode.SEARCH -> {
                            TopAppBarSearch(
                                focusRequester = focusRequester,
                                searchTextInput = viewModel.searchTextInput,
                                onSearchTextChange = { viewModel.onSearchTextChange(it) },
                                onTextDone = { },
                                onClose = {
                                    toolbarMode = ToolbarMode.MAIN
                                    viewModel.clearSearch()
                                },
                                placeholderText = stringResource(R.string.search_by_title),
                                scrollBehavior = scrollBehavior,
                            )
                        }
                    }
                }
                CollapsibleDivider(scrollBehavior.state)
                if (toolbarMode == ToolbarMode.MAIN) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                androidx.compose.material3.TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                },
                                selectedContentColor = MaterialTheme.colorScheme.onSurface,
                                unselectedContentColor = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }
        },
        content = { innerPadding ->
            if (viewModel.searchMode) {
                SearchSuggestionsContent(
                    searchResults = viewModel.searchResults,
                    onBookClick = { book ->
                        navigationRouteViewModel.chapters(context, book).let(context::startActivity)
                    },
                    modifier = Modifier
                )
            } else {
                CatalogList(
                    innerPadding = innerPadding,
                    databasesList = if (selectedTab == 0) viewModel.databaseList else emptyList(),
                    sourcesList = filteredSources,
                    onDatabaseClick = {
                        navigationRouteViewModel.databaseSearch(
                            context,
                            databaseBaseUrl = it.baseUrl
                        ).let(context::startActivity)
                    },
                    onSourceClick = {
                        navigationRouteViewModel.sourceCatalog(
                            context,
                            sourceBaseUrl = it.baseUrl
                        ).let(context::startActivity)
                    },
                    onSourceSetPinned = viewModel::onSourceSetPinned,
                    getOrCreatePopularBooksIterator = viewModel::getOrCreatePopularBooksIterator,
                    onBookClick = { book ->
                        navigationRouteViewModel.chapters(context, book).let(context::startActivity)
                    }
                )
            }
        }
    )
}
