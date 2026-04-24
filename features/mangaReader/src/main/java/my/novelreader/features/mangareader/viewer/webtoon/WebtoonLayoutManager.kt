package my.novelreader.features.mangareader.viewer.webtoon

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Custom LayoutManager for Webtoon viewer.
 * - Vertical linear layout
 * - Aggressive prefetch to keep pages ready
 */
class WebtoonLayoutManager(context: Context) : LinearLayoutManager(context, VERTICAL, false) {
    init {
        // Prefetch pages aggressively for smooth scrolling
        initialPrefetchItemCount = 5
    }

    override fun supportsPredictiveItemAnimations(): Boolean = false
}
