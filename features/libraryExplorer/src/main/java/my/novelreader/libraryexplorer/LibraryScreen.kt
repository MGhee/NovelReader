package my.novelreader.libraryexplorer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import my.novelreader.coreui.theme.ColorNotice
import my.novelreader.navigation.NavigationRouteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navigationRouteViewModel: NavigationRouteViewModel = viewModel()
) {
    val libraryModel: LibraryViewModel = viewModel()
    var confirmRemoveBookFor by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val context by rememberUpdatedState(LocalContext.current)
    var showDropDown by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        snapAnimationSpec = null,
        flingAnimationSpec = null
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { libraryModel.showBottomSheet = !libraryModel.showBottomSheet }
                        ) {
                            Icon(
                                Icons.Filled.FilterList,
                                stringResource(R.string.filter),
                                tint = ColorNotice
                            )
                        }
                        IconButton(
                            onClick = { showDropDown = !showDropDown }
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                stringResource(R.string.options_panel)
                            )
                            LibraryDropDown(
                                expanded = showDropDown,
                                onDismiss = { showDropDown = false }
                            )
                        }
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        },
        content = { innerPadding ->
            LibraryScreenBody(
                innerPadding = innerPadding,
                topAppBarState = scrollBehavior.state,
                onBookClick = { book ->
                    coroutineScope.launch {
                        libraryModel.updateLastSeenChaptersCount(book.book.url, book.chaptersCount)

                        val chapterUrl = libraryModel.getBookOpenChapterUrl(book.book.url)
                            ?: return@launch

                        navigationRouteViewModel.reader(
                            context = context,
                            bookUrl = book.book.url,
                            chapterUrl = chapterUrl
                        ).let(context::startActivity)
                    }
                },
                onBookMenuClick = {
                    libraryModel.bookOptionsSheetBook = it
                }
            )
        }
    )

    // Book options sheet
    val sheetBook = libraryModel.bookOptionsSheetBook
    if (sheetBook != null) {
        LibraryBookOptionsSheet(
            book = sheetBook,
            onDismiss = { libraryModel.bookOptionsSheetBook = null },
            onDownloadChapters = {
                libraryModel.bookOptionsSheetBook = null
                libraryModel.downloadAllBookChapters(sheetBook.book.url)
            },
            onReDownloadChapters = {
                libraryModel.bookOptionsSheetBook = null
                libraryModel.reDownloadAllBookChapters(sheetBook.book.url)
            },
            onDeleteDownloadedChapters = {
                libraryModel.bookOptionsSheetBook = null
                libraryModel.deleteDownloadedChapters(sheetBook.book.url)
            },
            onDeleteBook = {
                libraryModel.bookOptionsSheetBook = null
                confirmRemoveBookFor = sheetBook.book.url
            }
        )
    }

    LibraryBottomSheet(
        visible = libraryModel.showBottomSheet,
        onDismiss = { libraryModel.showBottomSheet = false }
    )

    

    if (confirmRemoveBookFor != null) {
        AlertDialog(
            onDismissRequest = { confirmRemoveBookFor = null },
            title = { Text(text = stringResource(R.string.removed_from_library)) },
            text = { Text(text = "Remove this book and all its downloaded files?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        libraryModel.removeBook(confirmRemoveBookFor!!)
                        confirmRemoveBookFor = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(text = stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmRemoveBookFor = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) { Text(text = stringResource(R.string.cancel)) }
            }
        )
    }
}
