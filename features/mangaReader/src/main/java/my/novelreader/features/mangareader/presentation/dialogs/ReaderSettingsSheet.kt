package my.novelreader.features.mangareader.presentation.dialogs

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import my.novelreader.features.mangareader.MangaReaderViewModel

/**
 * TODO: Full settings sheet with tabs:
 * - Reading Mode
 * - General Settings (brightness, background color, page indicator, keep screen on)
 * - Color Filter
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    viewModel: MangaReaderViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Text("Settings - Coming Soon")
    }
}
