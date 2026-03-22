package my.novelreader.features.reader.view

import android.view.View
import androidx.recyclerview.widget.RecyclerView
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

sealed class ReaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    data class BodyHolder(val binding: ActivityReaderListItemBodyBinding) : ReaderViewHolder(binding.root)
    data class TitleHolder(val binding: ActivityReaderListItemTitleBinding) : ReaderViewHolder(binding.root)
    data class ImageHolder(val binding: ActivityReaderListItemImageBinding) : ReaderViewHolder(binding.root)
    data class DividerHolder(val binding: ActivityReaderListItemDividerBinding) : ReaderViewHolder(binding.root)
    data class PaddingHolder(val binding: ActivityReaderListItemPaddingBinding) : ReaderViewHolder(binding.root)
    data class ProgressbarHolder(val binding: ActivityReaderListItemProgressBarBinding) : ReaderViewHolder(binding.root)
    data class ErrorHolder(val binding: ActivityReaderListItemErrorBinding) : ReaderViewHolder(binding.root)
    data class BookEndHolder(val binding: ActivityReaderListItemSpecialTitleBinding) : ReaderViewHolder(binding.root)
    data class BookStartHolder(val binding: ActivityReaderListItemSpecialTitleBinding) : ReaderViewHolder(binding.root)
    data class TranslatingHolder(val binding: ActivityReaderListItemTranslatingBinding) : ReaderViewHolder(binding.root)
    data class TranslateAttributionHolder(val binding: ActivityReaderListItemTranslateAttributionBinding) : ReaderViewHolder(binding.root)
    data class GoogleTranslateAttributionHolder(val binding: ActivityReaderListItemGoogleTranslateAttributionBinding) : ReaderViewHolder(binding.root)
    data class ChapterEndSpacerHolder(val binding: ActivityReaderListItemChapterEndSpacerBinding) : ReaderViewHolder(binding.root)
}
