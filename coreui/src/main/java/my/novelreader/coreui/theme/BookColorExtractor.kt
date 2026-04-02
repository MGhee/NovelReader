package my.novelreader.coreui.theme

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import my.novelreader.core.AppFileResolver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookColorExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appFileResolver: AppFileResolver,
) {

    suspend fun extractSeedColor(bookUrl: String, coverImageUrl: String): Int? {
        return try {
            val imageModel = appFileResolver.resolvedBookImagePath(bookUrl, coverImageUrl)
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageModel)
                .allowHardware(false) // Palette requires software bitmap
                .build()

            val result = loader.execute(request)
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return null

            val palette = Palette.from(bitmap).generate()
            palette.vibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb
        } catch (e: Exception) {
            null
        }
    }
}
