package my.novelreader.features.reader.view

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

class ReaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private val readerLayoutManager = ReaderLayoutManager(context)

    init {
        readerLayoutManager.attachToRecyclerView(this)
        itemAnimator = null
    }
}
