package my.novelreader.features.reader.view

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

interface ReaderListener {
    fun onPageChanged(page: Int)
}

class ReaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private val readerLayoutManager = ReaderLayoutManager(context)
    private var currentPage = 0
    private var listener: ReaderListener? = null

    init {
        readerLayoutManager.attachToRecyclerView(this)
        itemAnimator = null
    }

    private fun getAdapterItemCount(): Int = adapter?.itemCount ?: 0

    fun getCurrentPage(): Int {
        val adapterItemCount = getAdapterItemCount()
        val firstCompletelyVisible = readerLayoutManager.findFirstCompletelyVisibleItemPosition()
        return when {
            firstCompletelyVisible >= adapterItemCount - 1 -> adapterItemCount - 2
            firstCompletelyVisible <= 0 -> 1
            else -> firstCompletelyVisible
        }
    }

    fun scrollToPage(page: Int, smooth: Boolean = true) {
        var boundedPage = page
        val adapterItemCount = getAdapterItemCount()
        if (boundedPage < 0) boundedPage = 0
        if (boundedPage >= adapterItemCount) boundedPage = adapterItemCount - 1

        currentPage = boundedPage
        if (smooth) {
            smoothScrollToPosition(boundedPage)
        } else {
            scrollToPosition(boundedPage)
        }
    }

    fun scrollToNextPage() {
        currentPage++
        scrollToPage(currentPage, false)
    }

    fun scrollToPrevPage() {
        currentPage--
        scrollToPage(currentPage, false)
    }

    fun setListener(readerListener: ReaderListener?) {
        this.listener = readerListener
    }

    fun setOrientation(orientation: Int) {
        readerLayoutManager.setOrientation(orientation)
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        if (state != SCROLL_STATE_IDLE) return

        val firstCompletelyVisible = readerLayoutManager.findFirstCompletelyVisibleItemPosition()
        if (firstCompletelyVisible > -1) {
            currentPage = firstCompletelyVisible
        }
        listener?.onPageChanged(currentPage)
    }
}
