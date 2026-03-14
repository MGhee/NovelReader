plugins {
    alias(libs.plugins.novelreader.android.library)
    alias(libs.plugins.novelreader.android.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "my.novelreader.core"
}

dependencies {
    implementation(projects.strings)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.timber)
    implementation(libs.compose.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.preference.ktx)
}