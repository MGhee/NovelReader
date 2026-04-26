package my.novelreader.features.mangareader

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import my.novelreader.coreui.BaseActivity
import my.novelreader.coreui.composableActions.SetSystemBarTransparent
import my.novelreader.coreui.theme.Theme
import my.novelreader.core.utils.Extra_String
import my.novelreader.features.mangareader.presentation.MangaReaderRoot
import my.novelreader.features.mangareader.setting.ReaderOrientation

@AndroidEntryPoint
class MangaReaderActivity : BaseActivity() {

    class IntentData : Intent, MangaReaderStateBundle {
        override var bookUrl by Extra_String()
        override var chapterUrl by Extra_String()
        override var chapterSource by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(
            ctx: Context,
            bookUrl: String,
            chapterUrl: String,
            chapterSource: String? = null,
        ) : super(ctx, MangaReaderActivity::class.java) {
            this.bookUrl = bookUrl
            this.chapterUrl = chapterUrl
            this.chapterSource = chapterSource.orEmpty()
        }
    }

    private val viewModel by viewModels<MangaReaderViewModel>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Observe orientation preference and apply immediately
        lifecycleScope.launch {
            viewModel.readerPrefs.orientation.collect { orientation ->
                requestedOrientation = orientation.toActivityInfoFlag()
            }
        }

        // Back button closes the activity
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        setContent {
            Theme(themeProvider) {
                SetSystemBarTransparent()

                MangaReaderRoot(
                    viewModel = viewModel,
                    onBack = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        viewModel.cleanup()
        super.onDestroy()
    }
}

private fun ReaderOrientation.toActivityInfoFlag(): Int {
    return when (this) {
        ReaderOrientation.Free -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        ReaderOrientation.Portrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        ReaderOrientation.ReversePortrait -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        ReaderOrientation.Landscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        ReaderOrientation.ReverseLandscape -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
    }
}
