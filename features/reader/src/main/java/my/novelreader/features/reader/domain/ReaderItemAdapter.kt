package my.novelreader.features.reader.domain

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import my.novelreader.features.reader.features.TextSynthesis
import my.novelreader.features.reader.view.ReaderViewHolder
import my.novelreader.reader.databinding.ActivityReaderListItemBodyBinding
import my.novelreader.reader.databinding.ActivityReaderListItemDividerBinding
import my.novelreader.reader.databinding.ActivityReaderListItemErrorBinding
import my.novelreader.reader.databinding.ActivityReaderListItemGoogleTranslateAttributionBinding
import my.novelreader.reader.databinding.ActivityReaderListItemImageBinding
import my.novelreader.reader.databinding.ActivityReaderListItemPaddingBinding
import my.novelreader.reader.databinding.ActivityReaderListItemProgressBarBinding
import my.novelreader.reader.databinding.ActivityReaderListItemSpecialTitleBinding
import my.novelreader.reader.databinding.ActivityReaderListItemTitleBinding
import my.novelreader.reader.databinding.ActivityReaderListItemTranslateAttributionBinding
import my.novelreader.reader.databinding.ActivityReaderListItemTranslatingBinding

internal class ReaderItemAdapter(
    private val ctx: Context,
    private val items: List<ReaderItem>,
    private val bookUrl: String,
    private val currentSpeakerActiveItem: () -> TextSynthesis,
    private val currentTextSelectability: () -> Boolean,
    private val currentFontSize: () -> Float,
    private val currentTypeface: () -> Typeface,
    private val currentTypefaceBold: () -> Typeface,
    private val currentMarginDp: () -> Int = { 8 },
    private val currentLineSpacingMultiplier: () -> Float = { 1.0f },
    private val currentLineBreakHeight: () -> Int = { 0 },
    private val currentTextIndent: () -> Boolean = { true },
    private val onChapterStartVisible: (chapterUrl: String) -> Unit,
    private val onChapterEndVisible: (chapterUrl: String) -> Unit,
    private val onReloadReader: () -> Unit,
    private val onClick: () -> Unit,
) : RecyclerView.Adapter<ReaderViewHolder>() {

    private val binder = ReaderItemBinder(
        ctx = ctx,
        bookUrl = bookUrl,
        currentSpeakerActiveItem = currentSpeakerActiveItem,
        currentTextSelectability = currentTextSelectability,
        currentFontSize = currentFontSize,
        currentTypeface = currentTypeface,
        currentTypefaceBold = currentTypefaceBold,
        currentMarginDp = currentMarginDp,
        currentLineSpacingMultiplier = currentLineSpacingMultiplier,
        currentLineBreakHeight = currentLineBreakHeight,
        currentTextIndent = currentTextIndent,
        onChapterStartVisible = onChapterStartVisible,
        onChapterEndVisible = onChapterEndVisible,
        onReloadReader = onReloadReader,
        onClick = onClick,
    )

    private val inflater = LayoutInflater.from(ctx)

    override fun getItemCount() = items.size + 2  // top and bottom padding

    fun getItem(position: Int): ReaderItem = when (position) {
        0 -> topPadding
        count - 1 -> bottomPadding
        else -> items[position - 1]
    }

    val count get() = itemCount

    // Helpers for position mapping
    fun getFirstVisibleItemIndexGivenPosition(firstVisiblePosition: Int): Int =
        when (firstVisiblePosition) {
            in 1 until (count - 1) -> firstVisiblePosition - 1
            0 -> 0
            count - 1 -> count - 1
            else -> -1
        }

    fun fromPositionToIndex(position: Int): Int = when (position) {
        in 1 until (count - 1) -> position - 1
        else -> -1
    }

    fun fromIndexToPosition(index: Int): Int = when (index) {
        in 0 until items.size -> index + 1
        else -> -1
    }

    private val topPadding = ReaderItem.Padding(chapterIndex = Int.MIN_VALUE)
    private val bottomPadding = ReaderItem.Padding(chapterIndex = Int.MAX_VALUE)

    override fun getItemViewType(position: Int) = when (getItem(position)) {
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReaderViewHolder {
        return when (viewType) {
            0 -> ReaderViewHolder.BodyHolder(
                ActivityReaderListItemBodyBinding.inflate(inflater, parent, false)
            )
            1 -> ReaderViewHolder.ImageHolder(
                ActivityReaderListItemImageBinding.inflate(inflater, parent, false)
            )
            2 -> ReaderViewHolder.BookEndHolder(
                ActivityReaderListItemSpecialTitleBinding.inflate(inflater, parent, false)
            )
            3 -> ReaderViewHolder.BookStartHolder(
                ActivityReaderListItemSpecialTitleBinding.inflate(inflater, parent, false)
            )
            4 -> ReaderViewHolder.DividerHolder(
                ActivityReaderListItemDividerBinding.inflate(inflater, parent, false)
            )
            5 -> ReaderViewHolder.ErrorHolder(
                ActivityReaderListItemErrorBinding.inflate(inflater, parent, false)
            )
            6 -> ReaderViewHolder.PaddingHolder(
                ActivityReaderListItemPaddingBinding.inflate(inflater, parent, false)
            )
            7 -> ReaderViewHolder.ProgressbarHolder(
                ActivityReaderListItemProgressBarBinding.inflate(inflater, parent, false)
            )
            8 -> ReaderViewHolder.TitleHolder(
                ActivityReaderListItemTitleBinding.inflate(inflater, parent, false)
            )
            9 -> ReaderViewHolder.TranslatingHolder(
                ActivityReaderListItemTranslatingBinding.inflate(inflater, parent, false)
            )
            10 -> ReaderViewHolder.GoogleTranslateAttributionHolder(
                ActivityReaderListItemGoogleTranslateAttributionBinding.inflate(inflater, parent, false)
            )
            11 -> ReaderViewHolder.TranslateAttributionHolder(
                ActivityReaderListItemTranslateAttributionBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: ReaderViewHolder, position: Int) {
        val item = getItem(position)
        binder.bindViewHolder(holder, item)
    }
}
