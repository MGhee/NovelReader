package my.novelreader.core

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface Toasty {
    fun show(text: String, shortDuration: Boolean = true)
    fun show(@StringRes id: Int, shortDuration: Boolean = true)
}

@Singleton
class ToastyToast @Inject constructor(
    @ApplicationContext private val context: Context
) : Toasty {

    private val scope = MainScope()

    override fun show(text: String, shortDuration: Boolean) {
        scope.launch {
            Toast.makeText(context, text, durationMapper(shortDuration))
                .show()
        }
    }

    override fun show(@StringRes id: Int, shortDuration: Boolean) {
        scope.launch {
            Toast.makeText(context, id, durationMapper(shortDuration))
                .show()
        }
    }

    private fun durationMapper(shortDuration: Boolean) = when (shortDuration) {
        true -> Toast.LENGTH_SHORT
        false -> Toast.LENGTH_LONG
    }
}

