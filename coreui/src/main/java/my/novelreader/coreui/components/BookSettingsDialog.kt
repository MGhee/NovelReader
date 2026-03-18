package my.novelreader.coreui.components

import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
// Button removed; using OutlinedButton only
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.parcelize.Parcelize
import my.novelreader.coreui.R
import my.novelreader.coreui.theme.ImageBorderShape
import my.novelreader.coreui.theme.colorApp
import my.novelreader.core.rememberResolvedBookImagePath
import my.novelreader.feature.local_database.tables.Book


sealed interface BookSettingsDialogState : Parcelable {
    @Parcelize
    data object Hide : BookSettingsDialogState

    @Parcelize
    data class Show(val book: Book) :
        BookSettingsDialogState
}

@Composable
fun BookSettingsDialog(
    book: Book,
    onDismiss: () -> Unit,
    onToggleCompleted: () -> Unit,
    onRemove: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            ImageView(
                imageModel = rememberResolvedBookImagePath(
                    bookUrl = book.url,
                    imagePath = book.coverImageUrl
                ),
                error = R.drawable.default_book_cover,
                modifier = Modifier
                    .width(96.dp)
                    .aspectRatio(1 / 1.45f)
                    .clip(ImageBorderShape)
            )
        },
        title = {
            Text(text = book.title)
        },
        confirmButton = {},
        text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Checkbox(
                            checked = book.completed,
                            onCheckedChange = { onToggleCompleted() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorApp.checkboxPositive,
                                checkmarkColor = MaterialTheme.colorScheme.inverseOnSurface
                            )
                        )
                        Text(
                            text = stringResource(R.string.completed),
                        )
                    }

                Spacer(modifier = Modifier.size(8.dp))

                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = "Remove from library")
                }
                }
            }
    )
}