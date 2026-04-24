package my.novelreader.features.mangareader.viewer.pager

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import my.novelreader.features.mangareader.viewer.ViewerNavigation

/**
 * Frame that detects taps and long-presses on pager pages.
 * Similar to WebtoonFrame but for page-by-page navigation.
 */
class PagerPageFrame(context: Context) : FrameLayout(context) {
    private var onTap: ((ViewerNavigation.NavigationAction) -> Unit)? = null
    private var onLongPress: (() -> Unit)? = null
    private var navigation: ViewerNavigation? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val nav = navigation ?: return false
            val action = nav.getActionForTap(e.x, e.y, width, height)
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

    fun setNavigation(navigation: ViewerNavigation) {
        this.navigation = navigation
    }

    fun setOnTap(callback: (ViewerNavigation.NavigationAction) -> Unit) {
        this.onTap = callback
    }

    fun setOnLongPress(callback: () -> Unit) {
        this.onLongPress = callback
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        // Return true to keep receiving events for gesture detection,
        // ViewPager2 will intercept on ACTION_MOVE for swipe navigation
        return true
    }
}
