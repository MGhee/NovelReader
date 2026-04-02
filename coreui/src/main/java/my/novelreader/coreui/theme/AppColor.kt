package my.novelreader.coreui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColor(
    val tabSurface: Color,
    val bookSurface: Color,
    val checkboxPositive: Color,
    val checkboxNegative: Color,
    val checkboxNeutral: Color,
    val tintedSurface: Color,
    val tintedSelectedSurface: Color,
    val accent: Color,
    val accentVariant: Color,
    val calendarHeat1: Color,
    val calendarHeat2: Color,
    val calendarHeat3: Color,
    val calendarHeat4: Color,
    val navBarSurface: Color,
)

val light_appColor = AppColor(
    tabSurface = Grey75,
    bookSurface = Grey75,
    checkboxPositive = Success500,
    checkboxNegative = Error500,
    checkboxNeutral = Grey900,
    tintedSurface = Grey25.mix(AccentLight, 0.65f),
    tintedSelectedSurface = Grey25.mix(AccentLight, 0.75f),
    accent = AccentLight,
    accentVariant = AccentLight.mix(Grey25, 0.4f),
    calendarHeat1 = Grey25.mix(AccentLight, 0.75f),  // 25% accent
    calendarHeat2 = Grey25.mix(AccentLight, 0.50f),  // 50% accent
    calendarHeat3 = Grey25.mix(AccentLight, 0.30f),  // 70% accent
    calendarHeat4 = AccentLight,                      // 100% accent
    navBarSurface = NavSurfaceLight,
)

val dark_appColor = AppColor(
    tabSurface = Grey800,
    bookSurface = Grey800,
    checkboxPositive = Success500,
    checkboxNegative = Error500,
    checkboxNeutral = Grey900,
    tintedSurface = Grey900.mix(AccentDark, 0.65f),
    tintedSelectedSurface = Grey900.mix(AccentDark, 0.75f),
    accent = AccentDark,
    accentVariant = AccentDark.mix(Grey900, 0.4f),
    calendarHeat1 = Grey900.mix(AccentDark, 0.75f),  // 25% accent
    calendarHeat2 = Grey900.mix(AccentDark, 0.50f),  // 50% accent
    calendarHeat3 = Grey900.mix(AccentDark, 0.30f),  // 70% accent
    calendarHeat4 = AccentDark,                       // 100% accent
    navBarSurface = NavSurfaceDark,
)

val black_appColor = AppColor(
    tabSurface = Grey900,
    bookSurface = Grey900,
    checkboxPositive = Success500,
    checkboxNegative = Error500,
    checkboxNeutral = Grey900,
    tintedSurface = Grey1000.mix(AccentBlack, 0.65f),
    tintedSelectedSurface = Grey1000.mix(AccentBlack, 0.75f),
    accent = AccentBlack,
    accentVariant = AccentBlack.mix(Grey1000, 0.4f),
    calendarHeat1 = Grey1000.mix(AccentBlack, 0.75f),  // 25% accent
    calendarHeat2 = Grey1000.mix(AccentBlack, 0.50f),  // 50% accent
    calendarHeat3 = Grey1000.mix(AccentBlack, 0.30f),  // 70% accent
    calendarHeat4 = AccentBlack,                        // 100% accent
    navBarSurface = NavSurfaceBlack,
)

val darkTeal_appColor = AppColor(
    tabSurface = TealDark800,
    bookSurface = TealDark800,
    checkboxPositive = Success500,
    checkboxNegative = Error500,
    checkboxNeutral = TealDark900,
    tintedSurface = TealDark900.mix(TealLight300, 0.65f),
    tintedSelectedSurface = TealDark900.mix(TealLight300, 0.75f),
    accent = TealLight300,
    accentVariant = TealLight300.mix(TealDark900, 0.4f),
    calendarHeat1 = TealDark900.mix(TealLight300, 0.75f),  // 25% accent
    calendarHeat2 = TealDark900.mix(TealLight300, 0.50f),  // 50% accent
    calendarHeat3 = TealDark900.mix(TealLight300, 0.30f),  // 70% accent
    calendarHeat4 = TealLight300,                           // 100% accent
    navBarSurface = TealDark800,
)

val sepia_appColor = AppColor(
    tabSurface = SepiaLight50,
    bookSurface = SepiaLight50,
    checkboxPositive = Success500,
    checkboxNegative = Error500,
    checkboxNeutral = SepiaDark,
    tintedSurface = SepiaLight.mix(AccentSepia, 0.65f),
    tintedSelectedSurface = SepiaLight.mix(AccentSepia, 0.75f),
    accent = AccentSepia,
    accentVariant = AccentSepia.mix(SepiaLight, 0.4f),
    calendarHeat1 = SepiaLight.mix(AccentSepia, 0.75f),  // 25% accent
    calendarHeat2 = SepiaLight.mix(AccentSepia, 0.50f),  // 50% accent
    calendarHeat3 = SepiaLight.mix(AccentSepia, 0.30f),  // 70% accent
    calendarHeat4 = AccentSepia,                         // 100% accent
    navBarSurface = NavSurfaceSepia,
)

val LocalAppColor = compositionLocalOf { light_appColor }

@Suppress("UnusedReceiverParameter")
val MaterialTheme.colorApp: AppColor
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColor.current