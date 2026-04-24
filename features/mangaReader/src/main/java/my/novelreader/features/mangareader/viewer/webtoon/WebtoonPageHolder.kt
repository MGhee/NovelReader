package my.novelreader.features.mangareader.viewer.webtoon

import android.content.res.Resources
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import my.novelreader.features.mangareader.model.MangaReaderPage
import my.novelreader.features.mangareader.setting.ViewerConfig
import my.novelreader.features.mangareader.widget.ReaderPageImageView

/**
 * ViewHolder for a single webtoon page.
 *
 * Each item has a screen-height placeholder that guarantees a measurable
 * item size even before the image is loaded — without this, items collapse
 * to zero height and the RecyclerView has nothing to scroll.
 *
 * Touch events are handled at the WebtoonRecyclerView level, so the image
 * view disables its own SSIV touch handling.
 */
class WebtoonPageHolder(
    private val container: FrameLayout,
    parentHeight: Int,
) : RecyclerView.ViewHolder(container) {

    private val imageView = ReaderPageImageView(container.context).apply {
        disableTouch = true
        fitToWidth = true
    }
    private val progressContainer: FrameLayout

    init {
        container.removeAllViews()

        // Placeholder shows loading state. Since images are pre-cached, this is usually not visible,
        // but kept as fallback. Height is set to actual image height when available (via imageView).
        progressContainer = FrameLayout(container.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        container.addView(progressContainer)
        container.addView(
            imageView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        imageView.onImageReady = { onImageReady() }
    }

    fun bind(page: MangaReaderPage, config: ViewerConfig? = null) {
        // Apply config settings to the image view
        config?.let {
            imageView.doubleTapZoomEnabled = it.doubleTapZoom
            imageView.cropBorders = it.cropBorders
        }
        imageView.loadImage(page.imageUrl)
    }

    fun recycle() {
        progressContainer.isVisible = true
        imageView.clear()
    }

    private fun onImageReady() {
        progressContainer.isVisible = false
        container.requestLayout()
    }

    companion object {
        fun create(parent: ViewGroup): WebtoonPageHolder {
            val recyclerHeight = (parent as? RecyclerView)?.height?.takeIf { it > 0 }
            val parentHeight = recyclerHeight ?: Resources.getSystem().displayMetrics.heightPixels

            val container = FrameLayout(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }
            return WebtoonPageHolder(container, parentHeight)
        }
    }
}
