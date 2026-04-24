package my.novelreader.features.mangareader.viewer.webtoon

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView for the webtoon viewer.
 *
 * Detects single-tap and long-press at the RecyclerView level (Komikku-style),
 * because per-item gesture detectors fight with SSIV touch consumption.
 */
class WebtoonRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr) {

    var tapListener: ((x: Float, y: Float, width: Int, height: Int) -> Unit)? = null
    var longTapListener: (() -> Unit)? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            tapListener?.invoke(e.x, e.y, width, height)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            longTapListener?.invoke()
        }
    })

    override fun onTouchEvent(e: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(e)
        return super.onTouchEvent(e)
    }
}
