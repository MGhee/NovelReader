package my.novelreader.features.reader.domain

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import my.novelreader.reader.R

/**
 * ViewPager2 adapter for horizontal (page-based) reading mode.
 * Each page is a full-screen vertical LinearLayout containing multiple ReaderItems.
 */
internal class ReaderPageAdapter(
    private val ctx: Context,
    private val pages: List<PageData>,
    private val binder: ReaderItemBinder,
    private val onClick: () -> Unit,
) : RecyclerView.Adapter<ReaderPageAdapter.PageViewHolder>() {

    inner class PageViewHolder(private val pageContainer: LinearLayout) : RecyclerView.ViewHolder(pageContainer) {
        fun bind(page: PageData) {
            pageContainer.removeAllViews()

            for (item in page.items) {
                val viewType = getItemViewType(item)
                val holder = binder.inflateAndBindViewHolder(pageContainer, viewType, item)
                pageContainer.addView(holder.itemView)
            }

            // Click listener for showing/hiding reader info
            pageContainer.setOnClickListener { onClick() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val pageContainer = LayoutInflater.from(ctx)
            .inflate(R.layout.activity_reader_page_item, parent, false) as LinearLayout
        return PageViewHolder(pageContainer)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = pages.getOrNull(position) ?: return
        holder.bind(page)
    }

    override fun getItemCount(): Int = pages.size

    /**
     * Returns the view type for a given item (matches ReaderItemAdapter logic).
     */
    private fun getItemViewType(item: ReaderItem): Int = when (item) {
        is ReaderItem.Body -> 0
        is ReaderItem.Image -> 1
        is ReaderItem.BookEnd -> 2
        is ReaderItem.BookStart -> 3
        is ReaderItem.Divider -> 4
        is ReaderItem.Error -> 5
        is ReaderItem.Padding -> 6
        is ReaderItem.Progressbar -> 7
        is ReaderItem.Title -> 8
        is ReaderItem.Translating -> 9
        is ReaderItem.GoogleTranslateAttribution -> 10
        is ReaderItem.TranslateAttribution -> 11
        is ReaderItem.ChapterEndSpacer -> 12
    }
}
