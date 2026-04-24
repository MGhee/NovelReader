package my.novelreader.features.mangareader.viewer.webtoon

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import my.novelreader.features.mangareader.model.MangaReaderPage
import my.novelreader.features.mangareader.setting.ViewerConfig

/**
 * Adapter for Webtoon RecyclerView.
 */
class WebtoonAdapter(
    private val pages: List<MangaReaderPage>,
    private val config: ViewerConfig? = null,
) : RecyclerView.Adapter<WebtoonPageHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebtoonPageHolder {
        return WebtoonPageHolder.create(parent)
    }

    override fun onBindViewHolder(holder: WebtoonPageHolder, position: Int) {
        holder.bind(pages[position], config)
    }

    override fun onViewRecycled(holder: WebtoonPageHolder) {
        super.onViewRecycled(holder)
        holder.recycle()
    }

    override fun getItemCount(): Int = pages.size
}
