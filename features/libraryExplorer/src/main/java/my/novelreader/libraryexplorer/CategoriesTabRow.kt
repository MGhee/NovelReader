package my.novelreader.libraryexplorer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import my.novelreader.feature.local_database.tables.Category

/**
 * Categories tab row with horizontal pager for library books by category
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun CategoriesTabRow(
    categories: List<Category>,
    selectedCategoryId: Int,
    onCategorySelected: (Int) -> Unit,
    onManageCategoriesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = categories.indexOfFirst { it.id == selectedCategoryId }.takeIf { it >= 0 } ?: 0,
        pageCount = { categories.size }
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedCategoryId) {
        val index = categories.indexOfFirst { it.id == selectedCategoryId }
        if (index >= 0 && index != pagerState.currentPage) {
            pagerState.animateScrollToPage(index)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < categories.size) {
            onCategorySelected(categories[pagerState.currentPage].id)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            edgePadding = 16.dp,
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    text = {
                        Text(
                            text = category.name,
                            maxLines = 1,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // Add category button in tab row
            Tab(
                selected = false,
                onClick = onManageCategoriesClick,
                modifier = Modifier.padding(horizontal = 4.dp),
                text = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add category",
                        modifier = Modifier.height(24.dp)
                    )
                }
            )
        }
    }
}
