package my.novelreader.features.reader.domain

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import my.novelreader.core.AppFileResolver
import my.novelreader.features.reader.features.TextSynthesis
import my.novelreader.features.reader.view.ReaderViewHolder
import my.novelreader.reader.R
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
import my.novelreader.reader.databinding.ActivityReaderListItemChapterEndSpacerBinding
import my.novelreader.text_to_speech.Utterance

internal class ReaderItemBinder(
    private val ctx: Context,
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
) {
    private val appFileResolver = AppFileResolver(ctx)
    private val inflater = LayoutInflater.from(ctx)

    private fun dpToPx(dp: Int): Int = (dp * ctx.resources.displayMetrics.density).toInt()

    fun inflateAndBindViewHolder(parent: ViewGroup, viewType: Int, item: ReaderItem): ReaderViewHolder {
        val holder = when (viewType) {
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
            12 -> ReaderViewHolder.ChapterEndSpacerHolder(
                ActivityReaderListItemChapterEndSpacerBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }

        bindViewHolder(holder, item)
        return holder
    }

    fun bindViewHolder(holder: ReaderViewHolder, item: ReaderItem) {
        when (holder) {
            is ReaderViewHolder.BodyHolder -> bindBody(holder, item as ReaderItem.Body)
            is ReaderViewHolder.TitleHolder -> bindTitle(holder, item as ReaderItem.Title)
            is ReaderViewHolder.ImageHolder -> bindImage(holder, item as ReaderItem.Image)
            is ReaderViewHolder.DividerHolder -> {} // No binding needed
            is ReaderViewHolder.PaddingHolder -> bindPadding(holder, item as ReaderItem.Padding)
            is ReaderViewHolder.ProgressbarHolder -> {} // No binding needed
            is ReaderViewHolder.ErrorHolder -> bindError(holder, item as ReaderItem.Error)
            is ReaderViewHolder.BookEndHolder -> bindBookEnd(holder)
            is ReaderViewHolder.BookStartHolder -> bindBookStart(holder)
            is ReaderViewHolder.TranslatingHolder -> bindTranslating(holder, item as ReaderItem.Translating)
            is ReaderViewHolder.TranslateAttributionHolder -> bindTranslateAttribution(holder, item as ReaderItem.TranslateAttribution)
            is ReaderViewHolder.GoogleTranslateAttributionHolder -> {} // No binding needed
            is ReaderViewHolder.ChapterEndSpacerHolder -> bindChapterEndSpacer(holder)
        }
    }

    private fun bindBody(holder: ReaderViewHolder.BodyHolder, item: ReaderItem.Body) {
        val bind = holder.binding
        bind.body.updateTextSelectability()
        bind.root.background = getItemReadingStateBackground(item)

        val marginPx = dpToPx(currentMarginDp())
        val spacingDp = maxOf(0, (currentLineBreakHeight() - 10) * 2 - 5)
        val spacingPx = dpToPx(spacingDp)

        bind.body.setPadding(marginPx, bind.body.paddingTop, marginPx, spacingPx)

        val paragraph = item.textToDisplay
        if (currentTextIndent()) {
            val spanned = android.text.SpannableString(paragraph)
            val indentPx = dpToPx(16)
            spanned.setSpan(LeadingMarginSpan.Standard(indentPx, 0), 0, spanned.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            bind.body.text = spanned
        } else {
            bind.body.text = paragraph
        }

        bind.body.textSize = currentFontSize()
        bind.body.typeface = currentTypeface()
        bind.body.setLineSpacing(0f, currentLineSpacingMultiplier())

        when (item.location) {
            ReaderItem.Location.FIRST -> onChapterStartVisible(item.chapterUrl)
            ReaderItem.Location.LAST -> onChapterEndVisible(item.chapterUrl)
            else -> {}
        }
    }

    private fun bindTitle(holder: ReaderViewHolder.TitleHolder, item: ReaderItem.Title) {
        val bind = holder.binding
        bind.title.updateTextSelectability()
        bind.root.background = getItemReadingStateBackground(item)
        bind.title.text = item.textToDisplay
        bind.title.typeface = currentTypefaceBold()

        val marginPx = dpToPx(currentMarginDp())
        bind.title.setPadding(marginPx, bind.title.paddingTop, marginPx, bind.title.paddingBottom)
    }

    private fun bindImage(holder: ReaderViewHolder.ImageHolder, item: ReaderItem.Image) {
        val bind = holder.binding
        bind.image.updateLayoutParams<ConstraintLayout.LayoutParams> {
            dimensionRatio = "1:${item.image.yrel}"
        }

        val imageModel = appFileResolver.resolvedBookImagePath(bookUrl = bookUrl, imagePath = item.image.path)
        bind.imageContainer.doOnNextLayout {
            Glide.with(ctx)
                .load(imageModel)
                .fitCenter()
                .error(R.drawable.ic_baseline_error_outline_24)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(bind.image)
        }

        when (item.location) {
            ReaderItem.Location.FIRST -> onChapterStartVisible(item.chapterUrl)
            ReaderItem.Location.LAST -> onChapterEndVisible(item.chapterUrl)
            else -> {}
        }
    }

    private fun bindPadding(holder: ReaderViewHolder.PaddingHolder, item: ReaderItem.Padding) {
        val bind = holder.binding
        val spacingDp = maxOf(0, (currentLineBreakHeight() - 10) * 2 - 5)
        val lineBreakHeightPx = dpToPx(spacingDp)

        if (lineBreakHeightPx <= 0) {
            bind.root.visibility = View.GONE
        } else {
            bind.root.visibility = View.VISIBLE
            bind.root.updateLayoutParams {
                height = lineBreakHeightPx
            }
        }
        bind.root.setPadding(0, 0, 0, 0)
    }

    private fun bindError(holder: ReaderViewHolder.ErrorHolder, item: ReaderItem.Error) {
        val bind = holder.binding
        bind.error.updateTextSelectability()
        bind.reloadButton.setOnClickListener { onReloadReader() }
        bind.error.text = item.text
    }

    private fun bindBookEnd(holder: ReaderViewHolder.BookEndHolder) {
        val bind = holder.binding
        bind.specialTitle.updateTextSelectability()
        bind.specialTitle.text = ctx.getString(R.string.reader_no_more_chapters)
        bind.specialTitle.typeface = currentTypefaceBold()
    }

    private fun bindBookStart(holder: ReaderViewHolder.BookStartHolder) {
        val bind = holder.binding
        bind.specialTitle.updateTextSelectability()
        bind.specialTitle.text = ctx.getString(R.string.reader_first_chapter)
        bind.specialTitle.typeface = currentTypefaceBold()
    }

    private fun bindChapterEndSpacer(holder: ReaderViewHolder.ChapterEndSpacerHolder) {
        val screenHeight = ctx.resources.displayMetrics.heightPixels
        holder.binding.root.updateLayoutParams {
            height = screenHeight / 2
        }
    }

    private fun bindTranslating(holder: ReaderViewHolder.TranslatingHolder, item: ReaderItem.Translating) {
        val bind = holder.binding
        bind.text.text = ctx.getString(
            R.string.translating_from_lang_a_to_lang_b,
            item.sourceLang,
            item.targetLang
        )
    }

    private fun bindTranslateAttribution(holder: ReaderViewHolder.TranslateAttributionHolder, item: ReaderItem.TranslateAttribution) {
        val bind = holder.binding
        bind.attributionText.text = when (item.provider) {
            "gemini" -> "Powered by Gemini"
            else -> "Powered by Google Translate"
        }
    }

    private val currentReadingAloudDrawable by lazy {
        AppCompatResources.getDrawable(ctx, R.drawable.translucent_current_reading_text_background)
    }

    private val currentReadingAloudLoadingDrawable by lazy {
        AppCompatResources.getDrawable(ctx, R.drawable.translucent_current_reading_loading_text_background)
    }

    private fun TextView.updateTextSelectability() {
        val selectableText = currentTextSelectability()
        setTextIsSelectable(selectableText)
        if (selectableText) {
            setTextSelectionAwareClick { onClick() }
        } else {
            setOnClickListener { onClick() }
        }
    }

    private fun getItemReadingStateBackground(item: ReaderItem): Drawable? {
        val textSynthesis = currentSpeakerActiveItem()
        val isReadingItem = item is ReaderItem.Position &&
                textSynthesis.itemPos.chapterIndex == item.chapterIndex &&
                textSynthesis.itemPos.chapterItemPosition == item.chapterItemPosition

        if (!isReadingItem) return null

        return when (textSynthesis.playState) {
            Utterance.PlayState.PLAYING -> currentReadingAloudDrawable
            Utterance.PlayState.LOADING -> currentReadingAloudLoadingDrawable
            Utterance.PlayState.FINISHED -> null
        }
    }

    private fun View.setTextSelectionAwareClick(action: () -> Unit) {
        setOnClickListener { action() }
        setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP && !this.isFocused) {
                performClick()
            }
            false
        }
    }
}
