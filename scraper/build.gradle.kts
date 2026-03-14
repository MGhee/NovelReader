plugins {
    alias(libs.plugins.novelreader.android.library)
    alias(libs.plugins.novelreader.android.compose)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "my.novelreader.scraper"
}

dependencies {
    implementation(projects.strings)
    implementation(projects.core)
    implementation(projects.networking)

    implementation(libs.androidx.core.ktx)
    implementation(libs.jsoup)
    implementation(libs.gson)
    implementation(libs.okhttp)
    androidTestImplementation(libs.test.androidx.espresso.core)
}