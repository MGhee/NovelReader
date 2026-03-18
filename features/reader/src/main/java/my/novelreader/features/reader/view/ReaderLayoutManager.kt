package my.novelreader.features.reader.view

import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

class ReaderLayoutManager(context: Context) : LinearLayoutManager(context) {

    companion object {
        private const val MILLISECONDS_PER_INCH = 15f
        private const val MAX_SCROLL_ON_FLING_DURATION = 100
    }

    private var recyclerView: RecyclerView? = null

    fun attachToRecyclerView(recyclerView: RecyclerView?) {
        this.recyclerView = recyclerView
        if (recyclerView != null) {
            recyclerView.layoutManager = this
        }
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: Exception) {
            android.util.Log.e("ReaderLayoutManager", "Error in onLayoutChildren", e)
        }
    }

    fun smoothScrollToPositionWithOffset(position: Int, offset: Int, duration: Int = 500) {
        val recyclerView = this.recyclerView ?: return

        val scroller = object : LinearSmoothScroller(recyclerView.context) {
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return MILLISECONDS_PER_INCH / displayMetrics.densityDpi
            }

            override fun calculateTimeForScrolling(dx: Int): Int {
                return kotlin.math.min(MAX_SCROLL_ON_FLING_DURATION, super.calculateTimeForScrolling(dx))
            }

            override fun calculateDyToMakeVisible(view: View, snapPreference: Int): Int {
                val result = super.calculateDyToMakeVisible(view, snapPreference)
                return result + offset
            }
        }

        scroller.targetPosition = position
        startSmoothScroll(scroller)
    }

    override fun supportsPredictiveItemAnimations(): Boolean = false
}
