package my.novelreader.features.mangareader.widget

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ConnectionPool
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Image view for displaying manga pages with support for:
 * - Subsampling/tiling for very large images (prevents OOM)
 * - Pinch zoom and pan
 * - Double-tap zoom
 * - Fit-to-screen modes
 */
class ReaderPageImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val subsamplingView = SubsamplingScaleImageView(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
        isPanEnabled = true
        isZoomEnabled = true
        setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
    }

    private var currentImageUrl: String = ""
    private var isZoomed = false
    private var loadJob: Job? = null
    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val pageCacheDir = File(context.cacheDir, "manga_pages_cache").apply { mkdirs() }

    // Callback when image finishes loading
    var onImageReady: (() -> Unit)? = null

    // When true, set explicit height from image dimensions (webtoon mode).
    // When false, keep MATCH_PARENT layout (pager modes).
    var fitToWidth: Boolean = true
        set(value) {
            field = value
            updateSubsamplingLayout()
        }

    // Toggle double-tap zoom behavior
    var doubleTapZoomEnabled: Boolean = true
        set(value) {
            field = value
            subsamplingView.isQuickScaleEnabled = value
        }

    // Crop white borders from pages
    var cropBorders: Boolean = false

    init {
        addView(subsamplingView)
        updateSubsamplingLayout()
    }

    private fun updateSubsamplingLayout() {
        subsamplingView.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            if (fitToWidth) LayoutParams.WRAP_CONTENT else LayoutParams.MATCH_PARENT
        )
    }

    companion object {
        private var sharedClient: OkHttpClient? = null

        fun setSharedClient(client: OkHttpClient) {
            sharedClient = client
        }

        private val fallbackClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        val imageClient: OkHttpClient
            get() = sharedClient ?: fallbackClient
    }

    fun loadImage(url: String) {
        if (currentImageUrl == url && subsamplingView.hasImage()) {
            return
        }
        currentImageUrl = url
        loadJob?.cancel()

        loadJob = scope.launch {
            val requestedUrl = url
            val localFile = withContext(Dispatchers.IO) {
                cacheImageLocally(requestedUrl)
            }

            if (requestedUrl != currentImageUrl || localFile == null) {
                return@launch
            }

            // In fitToWidth mode (webtoon), decode and set explicit height from image dimensions.
            // In pager modes, keep MATCH_PARENT layout.
            if (fitToWidth) {
                val imageDimensions = withContext(Dispatchers.IO) {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(localFile.absolutePath, opts)
                    if (opts.outWidth > 0 && opts.outHeight > 0) {
                        Pair(opts.outWidth, opts.outHeight)
                    } else null
                }

                if (imageDimensions != null) {
                    val (imageWidth, imageHeight) = imageDimensions
                    val screenWidth = Resources.getSystem().displayMetrics.widthPixels
                    val scaledHeight = (screenWidth.toFloat() / imageWidth * imageHeight).toInt()
                    layoutParams = layoutParams?.apply { height = scaledHeight }
                        ?: FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, scaledHeight)
                }
            }

            val cropRegion = if (cropBorders) {
                withContext(Dispatchers.IO) { detectCropRegion(localFile) }
            } else null

            if (requestedUrl != currentImageUrl) return@launch

            runCatching {
                val imageSource = ImageSource.uri(Uri.fromFile(localFile)).let { src ->
                    if (cropRegion != null) src.region(cropRegion) else src
                }
                subsamplingView.setImage(imageSource)
                // Trigger callback when image is ready
                onImageReady?.invoke()
                // Request layout to ensure RecyclerView remeasures this item
                requestLayout()
            }
        }
    }

    /**
     * SubsamplingScaleImageView cannot reliably decode remote http(s) URIs via ContentResolver.
     * Download first and decode from a local file URI instead.
     */
    private fun cacheImageLocally(imageUrl: String): File? {
        val extension = imageUrl.fileExtensionOrDefault()
        val cachedFile = File(pageCacheDir, "${imageUrl.sha256()}.$extension")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return cachedFile
        }

        val tempFile = File.createTempFile("page_", ".tmp", pageCacheDir)
        return runCatching {
            // Derive referer from image URL origin for CDN compatibility
            val referer = try {
                val uri = java.net.URI(imageUrl)
                "${uri.scheme}://${uri.host}"
            } catch (_: Exception) {
                ""
            }

            val request = okhttp3.Request.Builder()
                .url(imageUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "image/*")
                .header("Referer", referer)
                .build()

            imageClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    tempFile.delete()
                    return null
                }

                response.body?.byteStream()?.use { input ->
                    tempFile.outputStream().buffered().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            if (tempFile.length() == 0L) {
                tempFile.delete()
                return null
            }

            if (!tempFile.renameTo(cachedFile)) {
                tempFile.copyTo(cachedFile, overwrite = true)
                tempFile.delete()
            }

            cachedFile
        }.getOrElse {
            tempFile.delete()
            null
        }
    }

    private fun String.fileExtensionOrDefault(): String {
        val clean = substringBefore('?').substringBefore('#')
        val ext = clean.substringAfterLast('.', "img").lowercase()
        return if (ext.length in 2..5 && ext.all { it.isLetterOrDigit() }) ext else "img"
    }

    private fun String.sha256(): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isCurrentlyZoomed(): Boolean {
        val scale = subsamplingView.scale
        return scale != null && scale > 1f
    }

    fun resetZoom() {
        subsamplingView.resetScaleAndCenter()
    }

    /**
     * When true, all touches are intercepted and not forwarded to SSIV, and
     * onTouchEvent returns false so the parent (WebtoonRecyclerView) handles
     * scroll + tap detection. Used in webtoon mode.
     */
    var disableTouch: Boolean = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (disableTouch) return true
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (disableTouch) return false
        isZoomed = isCurrentlyZoomed()
        if (isZoomed) {
            return subsamplingView.onTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        loadJob?.cancel()
        scope.cancel()
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    fun clear() {
        loadJob?.cancel()
        currentImageUrl = ""
        subsamplingView.recycle()
    }

    /**
     * Detect near-white borders by decoding a single downscaled copy of the image
     * (target ~200px on the long edge) and scanning rows/columns on it. Caller must
     * invoke this off the main thread.
     */
    private fun detectCropRegion(file: File): android.graphics.Rect? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val srcW = bounds.outWidth
            val srcH = bounds.outHeight
            if (srcW <= 0 || srcH <= 0) return null

            val target = 200
            var sample = 1
            while (maxOf(srcW, srcH) / (sample * 2) >= target) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
            val w = bmp.width
            val h = bmp.height
            val threshold = 240

            fun rowIsBorder(y: Int): Boolean {
                var bright = 0
                for (x in 0 until w) {
                    val p = bmp.getPixel(x, y)
                    val lum = (android.graphics.Color.red(p) + android.graphics.Color.green(p) + android.graphics.Color.blue(p)) / 3
                    if (lum > threshold) bright++
                }
                return bright * 100 / w >= 95
            }

            fun colIsBorder(x: Int): Boolean {
                var bright = 0
                for (y in 0 until h) {
                    val p = bmp.getPixel(x, y)
                    val lum = (android.graphics.Color.red(p) + android.graphics.Color.green(p) + android.graphics.Color.blue(p)) / 3
                    if (lum > threshold) bright++
                }
                return bright * 100 / h >= 95
            }

            var top = 0
            while (top < h && rowIsBorder(top)) top++
            var bottom = h - 1
            while (bottom > top && rowIsBorder(bottom)) bottom--
            var left = 0
            while (left < w && colIsBorder(left)) left++
            var right = w - 1
            while (right > left && colIsBorder(right)) right--

            bmp.recycle()

            if (top == 0 && left == 0 && bottom == h - 1 && right == w - 1) return null
            if ((right - left) < w * 0.5f || (bottom - top) < h * 0.5f) return null

            // Scale back up to source coordinates
            val srcLeft = left * sample
            val srcTop = top * sample
            val srcRight = ((right + 1) * sample).coerceAtMost(srcW)
            val srcBottom = ((bottom + 1) * sample).coerceAtMost(srcH)
            android.graphics.Rect(srcLeft, srcTop, srcRight, srcBottom)
        } catch (e: Exception) {
            null
        }
    }
}
