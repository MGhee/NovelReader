package my.novelreader.libraryexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.novelreader.data.LibraryDisplayMode

/**
 * Bottom sheet for library settings (display mode, sorting, filters)
 */
@Composable
internal fun LibrarySettingsSheet(
    currentDisplayMode: LibraryDisplayMode,
    onDisplayModeSelected: (LibraryDisplayMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Display Mode Section
        Text(
            text = stringResource(R.string.library_display_mode),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
        ) {
            LibraryDisplayMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDisplayModeSelected(mode) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = mode == currentDisplayMode,
                        onClick = { onDisplayModeSelected(mode) },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mode.displayName(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = mode.description(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun LibraryDisplayMode.displayName(): String = when (this) {
    LibraryDisplayMode.CompactGrid -> stringResource(R.string.library_display_mode_compact)
    LibraryDisplayMode.ComfortableGrid -> stringResource(R.string.library_display_mode_comfortable)
    LibraryDisplayMode.List -> stringResource(R.string.library_display_mode_list)
}

@Composable
private fun LibraryDisplayMode.description(): String = when (this) {
    LibraryDisplayMode.CompactGrid -> stringResource(R.string.library_display_mode_compact_desc)
    LibraryDisplayMode.ComfortableGrid -> stringResource(R.string.library_display_mode_comfortable_desc)
    LibraryDisplayMode.List -> stringResource(R.string.library_display_mode_list_desc)
}
