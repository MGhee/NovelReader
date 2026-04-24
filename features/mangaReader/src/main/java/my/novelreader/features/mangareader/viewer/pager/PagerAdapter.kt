package my.novelreader.features.mangareader.viewer.pager

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import my.novelreader.features.mangareader.model.MangaReaderPage
import my.novelreader.features.mangareader.setting.ViewerConfig
import my.novelreader.features.mangareader.viewer.ViewerNavigation
import my.novelreader.features.mangareader.widget.ReaderPageImageView

/**
 * Adapter for ViewPager2-based pager viewer (page-by-page navigation).
 */
class PagerAdapter(
    private val pages: List<MangaReaderPage>,
    private val onTapAction: (ViewerNavigation.NavigationAction) -> Unit,
    private val onLongPress: (MangaReaderPage) -> Unit,
    private val navigation: ViewerNavigation,
    private val config: ViewerConfig? = null,
) : RecyclerView.Adapter<PagerAdapter.PageViewHolder>() {

    class PageViewHolder(
        private val container: FrameLayout,
    ) : RecyclerView.ViewHolder(container) {

        private val imageView = ReaderPageImageView(container.context).apply {
            fitToWidth = false
            disableTouch = true
        }
        private lateinit var frame: PagerPageFrame

        init {
            container.removeAllViews()
            frame = PagerPageFrame(container.context)
            frame.addView(
                imageView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            )
            container.addView(
                frame,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            )
        }

        fun bind(
            page: MangaReaderPage,
            navigation: ViewerNavigation,
            onTapAction: (ViewerNavigation.NavigationAction) -> Unit,
            onLongPress: (MangaReaderPage) -> Unit,
            config: ViewerConfig? = null,
        ) {
            // Apply config settings to the image view
            config?.let {
                imageView.doubleTapZoomEnabled = it.doubleTapZoom
                imageView.cropBorders = it.cropBorders
            }
            imageView.loadImage(page.imageUrl)
            frame.setNavigation(navigation)
            frame.setOnTap { action ->
                onTapAction(action)
            }
            frame.setOnLongPress {
                onLongPress(page)
            }
        }

        fun recycle() {
            imageView.clear()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        return PageViewHolder(
            FrameLayout(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        )
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = pages[position]
        holder.bind(page, navigation, onTapAction, onLongPress, config)
    }

    override fun getItemCount(): Int = pages.size

    override fun onViewRecycled(holder: PageViewHolder) {
        super.onViewRecycled(holder)
        holder.recycle()
    }
}
