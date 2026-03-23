package my.novelreader.catalogexplorer

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CatalogExplorerScreen(
    navigationRouteViewModel: NavigationRouteViewModel = viewModel()
) {
    val viewModel: CatalogExplorerViewModel = viewModel()

    val context by rememberUpdatedState(newValue = LocalContext.current)
    var languagesOptionsExpanded by rememberSaveable { mutableStateOf(false) }
    var toolbarMode by rememberSaveable { mutableStateOf(ToolbarMode.MAIN) }
    val focusRequester = FocusRequester()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        snapAnimationSpec = null,
        flingAnimationSpec = null
    )

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
                                    viewModel.searchMode = false
                                    viewModel.searchTextInput = ""
                                    viewModel.searchResults.clear()
                                },
                                placeholderText = stringResource(R.string.search_by_title),
                                scrollBehavior = scrollBehavior,
                            )
                        }
                    }
                }
                CollapsibleDivider(scrollBehavior.state)
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
                    databasesList = viewModel.databaseList,
                    sourcesList = viewModel.sourcesList,
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
