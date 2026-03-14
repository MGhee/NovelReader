package my.novelreader.databaseexplorer.databaseBookInfo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import my.novelreader.coreui.BaseActivity
import my.novelreader.coreui.composableActions.SetSystemBarTransparent
import my.novelreader.coreui.theme.Theme
import my.novelreader.databaseexplorer.databaseSearch.DatabaseSearchActivity
import my.novelreader.databaseexplorer.databaseSearch.DatabaseSearchExtras
import my.novelreader.core.utils.Extra_String
import my.novelreader.navigation.NavigationRoutes
import my.novelreader.scraper.SearchGenre
import my.novelreader.feature.local_database.BookMetadata
import javax.inject.Inject

@AndroidEntryPoint
class DatabaseBookInfoActivity : BaseActivity() {
    class IntentData : Intent, DatabaseBookInfoStateBundle {
        override var databaseUrlBase by Extra_String()
        override var bookUrl by Extra_String()
        override var bookTitle by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, databaseUrlBase: String, bookMetadata: BookMetadata) : super(
            ctx,
            DatabaseBookInfoActivity::class.java
        ) {
            this.databaseUrlBase = databaseUrlBase
            this.bookUrl = bookMetadata.url
            this.bookTitle = bookMetadata.title
        }
    }

    @Inject
    internal lateinit var navigationRoutes: NavigationRoutes

    private val viewModel by viewModels<DatabaseBookInfoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            Theme(themeProvider = themeProvider) {
                SetSystemBarTransparent()
                DatabaseBookInfoScreen(
                    state = viewModel.state,
                    onSourcesClick = ::openGlobalSearchPage,
                    onGenresClick = ::openSearchPageByGenres,
                    onBookClick = ::openBookInfo,
                    onOpenInWeb = { navigationRoutes.webView(this, viewModel.bookUrl).let(::startActivity) },
                    onPressBack = ::onBackPressed
                )
            }
        }
    }

    private fun openGlobalSearchPage() = navigationRoutes.globalSearch(
        context = this,
        text = viewModel.state.book.value.title
    ).let(::startActivity)

    private fun openSearchPageByGenres(
        genres: List<SearchGenre>
    ) = DatabaseSearchActivity.IntentData(
        ctx = this,
        extras = DatabaseSearchExtras.Genres(
            databaseBaseUrl = viewModel.database.baseUrl,
            includedGenresIds = genres.map { it.id },
            excludedGenresIds = emptyList()
        )
    ).let(::startActivity)

    private fun openBookInfo(book: BookMetadata) = IntentData(
        ctx = this,
        bookMetadata = book,
        databaseUrlBase = viewModel.database.baseUrl
    ).let(::startActivity)
}
