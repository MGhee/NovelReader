package my.novelreader.features.reader.tools

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import my.novelreader.reader.R

internal class FontsLoader(private val context: Context? = null) {
    companion object {
        val availableFonts = listOf(
            "Arial",
            "Open Dyslexic",
            "Segoe UI",
            "Tahoma",
            "Times New Roman",
            "Verdana",
            "monospace",
            "sans-serif",
            "serif"
        )

        // Map display names to bundled font resource IDs
        private val fontNameToResId = mapOf(
            "Arial" to R.font.arial,
            "Open Dyslexic" to R.font.opendyslexic,
            "Segoe UI" to R.font.segoeui,
            "Tahoma" to R.font.tahoma,
            "Times New Roman" to R.font.times,
            "Verdana" to R.font.verdana,
            "monospace" to R.font.monospace
        )

        // Fallback to system fonts if bundled font fails to load
        private val fontNameToSystemFont = mapOf(
            "Arial" to "sans-serif",
            "Open Dyslexic" to "sans-serif",
            "Segoe UI" to "sans-serif",
            "Tahoma" to "sans-serif",
            "Times New Roman" to "serif",
            "Verdana" to "sans-serif",
            "monospace" to "monospace"
        )
    }

    private val typeFaceNORMALCache = mutableMapOf<String, Typeface>()
    private val typeFaceBOLDCache = mutableMapOf<String, Typeface>()
    private val fontFamilyCache = mutableMapOf<String, FontFamily>()

    fun getTypeFaceNORMAL(name: String) = typeFaceNORMALCache.getOrPut(name) {
        // Try bundled font first if context is available
        if (context != null) {
            val resId = fontNameToResId[name]
            if (resId != null) {
                ResourcesCompat.getFont(context, resId)?.let { return@getOrPut it }
            }
        }
        // Fallback to system font
        Typeface.create(fontNameToSystemFont[name] ?: name, Typeface.NORMAL)
    }

    fun getTypeFaceBOLD(name: String) = typeFaceBOLDCache.getOrPut(name) {
        // Try bundled font first if context is available
        if (context != null) {
            val resId = fontNameToResId[name]
            if (resId != null) {
                ResourcesCompat.getFont(context, resId)?.let { return@getOrPut it }
            }
        }
        // Fallback to system font
        Typeface.create(fontNameToSystemFont[name] ?: name, Typeface.BOLD)
    }

    fun getFontFamily(name: String) = fontFamilyCache.getOrPut(name) {
        FontFamily(getTypeFaceNORMAL(name))
    }
}