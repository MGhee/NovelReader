package my.novelreader.coreui.theme

import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import my.novelreader.coreui.R

enum class Themes(
    val isLight: Boolean,
    @StringRes val nameId: Int,
    @StyleRes val themeId: Int,
) {
    LIGHT(
        isLight = true,
        nameId = R.string.theme_name_light,
        themeId = R.style.AppTheme_Light,
    ),
    DARK(
        isLight = false,
        nameId = R.string.theme_name_dark,
        themeId = R.style.AppTheme_BaseDark_Dark,
    ),
    BLACK(
        isLight = false,
        nameId = R.string.theme_name_black,
        themeId = R.style.AppTheme_BaseDark_Black,
    ),
    DARK_TEAL(
        isLight = false,
        nameId = R.string.theme_name_dark_teal,
        themeId = R.style.AppTheme_BaseDark_DarkTeal,
    ),
    SEPIA(
        isLight = true,
        nameId = R.string.theme_name_sepia,
        themeId = R.style.AppTheme_Sepia,
    );
}