package my.novelreader.personal.sections

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.novelreader.coreui.components.Section
import my.novelreader.coreui.theme.ColorAccent
import my.novelreader.coreui.theme.colorApp
import my.novelreader.personal.TopNovelUiItem

@Composable
fun TopNovelsSection(
    topNovels: List<TopNovelUiItem>,
) {
    if (topNovels.isEmpty()) return

    Section(
        title = "Top Novels",
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorApp.tintedSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            topNovels.forEach { novel ->
                TopNovelItem(novel = novel)
            }
        }
    }
}

@Composable
private fun TopNovelItem(
    novel: TopNovelUiItem,
) {
    val progress = novel.chaptersRead.toFloat() / maxOf(novel.totalChapters, 1).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "novel_progress"
    )

    val abbreviation = abbreviateTitle(novel.title)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Title abbreviation (bold)
        Text(
            text = abbreviation,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Chapter count
        Text(
            text = "${novel.chaptersRead} / ${novel.totalChapters} chapters",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ColorAccent)
            )
        }
    }
}

private fun abbreviateTitle(title: String): String {
    if (title.length <= 20) return title

    val words = title.split(" ")
    if (words.size >= 3) {
        return words.mapNotNull { word ->
            word.firstOrNull()?.uppercaseChar()
        }.joinToString("")
    }

    return title
}
