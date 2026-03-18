package my.novelreader.libraryexplorer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import my.novelreader.feature.local_database.BookWithContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
internal fun LibraryScreenBody(
    innerPadding: PaddingValues,
    topAppBarState: TopAppBarState,
    onBookClick: (BookWithContext) -> Unit,
    onBookMenuClick: (BookWithContext) -> Unit,
    viewModel: LibraryPageViewModel = viewModel()
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = viewModel.isPullRefreshing,
        onRefresh = {
            viewModel.onLibraryRefresh()
        }
    )

    Box(
        modifier = Modifier
            .pullRefresh(state = pullRefreshState)
            .padding(innerPadding),
    ) {
        LibraryPageBody(
            list = viewModel.list,
            onClick = onBookClick,
            onMenuClick = onBookMenuClick
        )
        PullRefreshIndicator(
            refreshing = viewModel.isPullRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
