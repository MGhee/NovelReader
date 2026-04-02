package my.novelreader.coreui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

fun generateBookColorScheme(seedColor: Int, baseTheme: Themes): ColorScheme {
    val bookColor = Color(seedColor)
    val baseColorScheme = when (baseTheme) {
        Themes.LIGHT -> light_colorScheme
        Themes.DARK -> dark_colorScheme
        Themes.BLACK -> black_colorScheme
        Themes.DARK_TEAL -> darkTeal_colorScheme
        Themes.SEPIA -> sepia_colorScheme
    }

    // Blend the seed color into accent-related roles and subtly tint structural colors.
    // mix(bookColor, f) = base*f + book*(1-f), so f = 1 - desired_book_pct
    return baseColorScheme.copy(
        primary = baseColorScheme.primary.mix(bookColor, 0.80f),           // 20% book
        onPrimary = baseColorScheme.onPrimary,
        primaryContainer = baseColorScheme.primaryContainer.mix(bookColor, 0.70f), // 30% book
        onPrimaryContainer = baseColorScheme.onPrimaryContainer,
        secondary = bookColor,
        onSecondary = when (baseTheme.isLight) {
            true -> Color.Black
            false -> Color.White
        },
        secondaryContainer = baseColorScheme.secondaryContainer.mix(bookColor, 0.50f), // 50% book
        onSecondaryContainer = baseColorScheme.onSecondaryContainer,
        tertiary = bookColor.mix(baseColorScheme.tertiary, 0.40f),         // 40% book (receiver is bookColor)
        onTertiary = baseColorScheme.onTertiary,
        tertiaryContainer = baseColorScheme.tertiaryContainer.mix(bookColor, 0.70f), // 30% book
        onTertiaryContainer = baseColorScheme.onTertiaryContainer,
        surface = baseColorScheme.surface.mix(bookColor, 0.88f),           // 12% book
        onSurface = baseColorScheme.onSurface,
        surfaceVariant = baseColorScheme.surfaceVariant.mix(bookColor, 0.82f), // 18% book
        onSurfaceVariant = baseColorScheme.onSurfaceVariant,
        background = baseColorScheme.background.mix(bookColor, 0.92f),     // 8% book — subtle canvas tint
        onBackground = baseColorScheme.onBackground,
        surfaceContainerLowest = baseColorScheme.surface.mix(bookColor, 0.96f),  // 4% book
        surfaceContainerLow = baseColorScheme.surface.mix(bookColor, 0.92f),     // 8% book
        surfaceContainer = baseColorScheme.surface.mix(bookColor, 0.85f),        // 15% book
        surfaceContainerHigh = baseColorScheme.surface.mix(bookColor, 0.78f),    // 22% book
        surfaceContainerHighest = baseColorScheme.surface.mix(bookColor, 0.72f), // 28% book
        outline = baseColorScheme.outline.mix(bookColor, 0.85f),           // 15% book
        outlineVariant = baseColorScheme.outlineVariant.mix(bookColor, 0.88f), // 12% book
        scrim = baseColorScheme.scrim.mix(bookColor, 0.90f),               // 10% book
        surfaceTint = bookColor,
    )
}

fun generateBookAppColor(seedColor: Int, baseTheme: Themes): AppColor {
    val bookColor = Color(seedColor)
    val baseAppColor = when (baseTheme) {
        Themes.LIGHT -> light_appColor
        Themes.DARK -> dark_appColor
        Themes.BLACK -> black_appColor
        Themes.DARK_TEAL -> darkTeal_appColor
        Themes.SEPIA -> sepia_appColor
    }

    val baseColorForBlend = when (baseTheme) {
        Themes.LIGHT -> Grey25
        Themes.DARK -> Grey900
        Themes.BLACK -> Grey1000
        Themes.DARK_TEAL -> TealDark900
        Themes.SEPIA -> SepiaLight
    }

    // Generate 4 calendar heat intensities from book color (not blended with green base)
    val heat1 = baseColorForBlend.mix(bookColor, 0.78f)  // 22% book (lightest)
    val heat2 = baseColorForBlend.mix(bookColor, 0.55f)  // 45% book
    val heat3 = baseColorForBlend.mix(bookColor, 0.35f)  // 65% book
    val heat4 = baseColorForBlend.mix(bookColor, 0.18f)  // 82% book (strongest)

    return AppColor(
        tabSurface = baseAppColor.tabSurface.mix(bookColor, 0.82f),         // 18% book
        bookSurface = baseAppColor.bookSurface.mix(bookColor, 0.78f),       // 22% book
        checkboxPositive = baseAppColor.checkboxPositive,
        checkboxNegative = baseAppColor.checkboxNegative,
        checkboxNeutral = baseAppColor.checkboxNeutral,
        tintedSurface = baseColorForBlend.mix(bookColor, 0.35f),            // 65% book — strong reader chrome color
        tintedSelectedSurface = baseColorForBlend.mix(bookColor, 0.25f),    // 75% book
        accent = bookColor.mix(baseAppColor.accent, 0.5f),
        accentVariant = baseColorForBlend.mix(bookColor, 0.55f),            // 45% book
        calendarHeat1 = heat1,
        calendarHeat2 = heat2,
        calendarHeat3 = heat3,
        calendarHeat4 = heat4,
        navBarSurface = baseAppColor.navBarSurface.mix(bookColor, 0.70f),   // 30% book — distinct from background
    )
}
