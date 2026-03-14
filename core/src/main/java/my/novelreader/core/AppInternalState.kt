package my.novelreader.core

interface AppInternalState {
    val isDebugMode: Boolean
    val versionCode: Int
    val versionName: String
}