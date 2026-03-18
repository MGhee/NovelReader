package my.novelreader.core.appPreferences

enum class ReaderLineSpacingLevel {
    Small, Medium, Large;

    val multiplier: Float
        get() = when (this) {
            Small -> 1.0f
            Medium -> 1.3f
            Large -> 1.6f
        }
}
