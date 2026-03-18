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
)

val light_appColor = AppColor(
    tabSurface = Grey75,
    bookSurface = Grey75,
    checkboxPositive = Success500,
    checkboxNegative = Error500,
    checkboxNeutral = Grey900,
    tintedSurface = Grey25.mix(ColorAccent, 0.65f),
    tintedSelectedSurface = Grey25.mix(ColorAccent, 0.75f),
)

val dark_appColor = AppColor(
    tabSurface = Grey800,
    bookSurface = Grey800,
    checkboxPositive = Success500,
    checkboxNegative = Error500,
    checkboxNeutral = Grey900,
    tintedSurface = Grey900.mix(ColorAccent, 0.65f),
    tintedSelectedSurface = Grey900.mix(ColorAccent, 0.75f),
)

val black_appColor = AppColor(
    tabSurface = Grey900,
    bookSurface = Grey900,
    checkboxPositive = Success500,
    checkboxNegative = Error500,
    checkboxNeutral = Grey900,
    tintedSurface = Grey1000.mix(ColorAccent, 0.65f),
    tintedSelectedSurface = Grey1000.mix(ColorAccent, 0.75f),
)

val darkTeal_appColor = AppColor(
    tabSurface = TealDark800,
    bookSurface = TealDark800,
    checkboxPositive = Success500,
    checkboxNegative = Error500,
    checkboxNeutral = TealDark900,
    tintedSurface = TealDark900.mix(TealMid, 0.65f),
    tintedSelectedSurface = TealDark900.mix(TealMid, 0.75f),
)

val sepia_appColor = AppColor(
    tabSurface = SepiaLight50,
    bookSurface = SepiaLight50,
    checkboxPositive = Success500,
    checkboxNegative = Error500,
    checkboxNeutral = SepiaDark,
    tintedSurface = SepiaLight.mix(ColorAccent, 0.65f),
    tintedSelectedSurface = SepiaLight.mix(ColorAccent, 0.75f),
)

val LocalAppColor = compositionLocalOf { light_appColor }

@Suppress("UnusedReceiverParameter")
val MaterialTheme.colorApp: AppColor
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColor.current