package my.novelreader.features.mangareader.viewer.webtoon

import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import my.novelreader.features.mangareader.viewer.ViewerNavigation

/**
 * Custom frame that detects taps and passes them to navigation handler.
 * This frame wraps the image and detects tap actions WITHOUT blocking scroll gestures.
 */
class WebtoonFrame @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private var navigation: ViewerNavigation? = null
    private var onTap: ((ViewerNavigation.NavigationAction) -> Unit)? = null
    private var onLongPress: (() -> Unit)? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val action = navigation?.getActionForTap(
                e.x, e.y, width, height
            )
            if (action != null) {
                onTap?.invoke(action)
                return true
            }
            return false
        }

        override fun onLongPress(e: MotionEvent) {
            onLongPress?.invoke()
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    fun setNavigation(nav: ViewerNavigation) {
        this.navigation = nav
    }

    fun setOnTap(callback: (ViewerNavigation.NavigationAction) -> Unit) {
        this.onTap = callback
    }

    fun setOnLongPress(callback: () -> Unit) {
        this.onLongPress = callback
    }
}
