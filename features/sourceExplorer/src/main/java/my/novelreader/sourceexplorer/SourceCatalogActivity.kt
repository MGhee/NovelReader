package my.novelreader.sourceexplorer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import my.novelreader.coreui.BaseActivity
import my.novelreader.coreui.theme.Theme
import my.novelreader.core.utils.Extra_String
import my.novelreader.navigation.NavigationRoutes
import javax.inject.Inject


@AndroidEntryPoint
class SourceCatalogActivity : BaseActivity() {
    class IntentData : Intent, SourceCatalogStateBundle {
        override var sourceBaseUrl by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, sourceBaseUrl: String) : super(
            ctx,
            SourceCatalogActivity::class.java
        ) {
            this.sourceBaseUrl = sourceBaseUrl
        }
    }

    private val viewModel by viewModels<SourceCatalogViewModel>()

    @Inject
    internal lateinit var navigationRoutes: NavigationRoutes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Theme(themeProvider = themeProvider) {
                SourceCatalogScreen(
                    state = viewModel.state,
                    onSearchTextInputChange = viewModel::onSearchTextChange,
                    onSearchTextInputSubmit = viewModel::onSearchText,
                    onSearchCatalogSubmit = viewModel::onSearchCatalog,
                    onListLayoutModeChange = viewModel.state.listLayoutMode::value::set,
                    onToolbarModeChange = viewModel.state.toolbarMode::value::set,
                    onOpenSourceWebPage = {
                        navigationRoutes.webView(this, viewModel.sourceBaseUrl).let(::startActivity)
                    },
                    onBookClicked = { navigationRoutes.chapters(this, it).let(::startActivity) },
                    onBookLongClicked = viewModel::addToLibraryToggle,
                    onPressBack = ::onBackPressed
                )
            }
        }
    }
}
