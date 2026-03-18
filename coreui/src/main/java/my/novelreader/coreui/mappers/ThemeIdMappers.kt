package my.novelreader.coreui.mappers

import my.novelreader.coreui.theme.Themes
import my.novelreader.core.appPreferences.PreferenceThemes

val PreferenceThemes.toTheme
    get() = when (this) {
        PreferenceThemes.Light -> Themes.LIGHT
        PreferenceThemes.Dark -> Themes.DARK
        PreferenceThemes.Black -> Themes.BLACK
        PreferenceThemes.DarkTeal -> Themes.DARK_TEAL
        PreferenceThemes.Sepia -> Themes.SEPIA
    }

val Themes.toPreferenceTheme
    get() = when (this) {
        Themes.LIGHT -> PreferenceThemes.Light
        Themes.DARK -> PreferenceThemes.Dark
        Themes.BLACK -> PreferenceThemes.Black
        Themes.DARK_TEAL -> PreferenceThemes.DarkTeal
        Themes.SEPIA -> PreferenceThemes.Sepia
    }