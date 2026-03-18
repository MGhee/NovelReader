package my.novelreader.core.appPreferences

enum class ReaderMarginLevel {
    Small, Medium, Large;

    val dpValue: Int
        get() = when (this) {
            Small -> 8
            Medium -> 16
            Large -> 32
        }
}
