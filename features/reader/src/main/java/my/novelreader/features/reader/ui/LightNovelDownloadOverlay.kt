package my.novelreader.features.reader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import my.novelreader.core.ChapterDownloadProgress

/**
 * Full-screen overlay shown while a light-novel volume's body is being fetched. Backed by
 * [ChapterDownloadProgress], which the scraper updates as bytes arrive. Hides itself when
 * the URL leaves the map (the scraper calls `clear` on success or failure).
 *
 * The overlay swallows touches so the reader behind it can't be scrolled until the volume
 * is ready.
 */
@Composable
internal fun LightNovelDownloadOverlay(
    chapterUrl: String,
    chapterTitle: String,
    bookTitle: String,
    modifier: Modifier = Modifier,
) {
    val progressMap by ChapterDownloadProgress.flow.collectAsState()
    val fraction = progressMap[chapterUrl]
    val visible = fraction != null && fraction < 1f

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
                .pointerInput(Unit) { /* swallow touches */ },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            ) {
                if (bookTitle.isNotBlank()) {
                    Text(
                        text = bookTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    text = chapterTitle.ifBlank { "Loading volume…" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                val safeFraction = (fraction ?: 0f).coerceIn(0f, 1f)
                val indicatorColor = MaterialTheme.colorScheme.onBackground
                val trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.24f)
                LinearProgressIndicator(
                    progress = { safeFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = indicatorColor,
                    trackColor = trackColor,
                )
                Text(
                    text = "${(safeFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = indicatorColor.copy(alpha = 0.85f),
                )
            }
        }
    }
}

