package my.novelreader.tooling.epub_importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun onDoExportEPUB(): (bookUrl: String, bookTitle: String) -> Unit {
    val context = LocalContext.current
    var pendingBookUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val fileCreator = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/epub+zip"),
        onResult = { uri ->
            val bookUrl = pendingBookUrl
            pendingBookUrl = null
            if (uri != null && bookUrl != null)
                EpubExportService.start(ctx = context, uri = uri, bookUrl = bookUrl)
        }
    )
    return { bookUrl, bookTitle ->
        pendingBookUrl = bookUrl
        val fileName = bookTitle.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
        fileCreator.launch("$fileName.epub")
    }
}
