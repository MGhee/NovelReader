plugins {
    alias(libs.plugins.novelreader.android.library)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "my.novelreader.tooling.epub_parser"
}

dependencies {
    implementation(projects.core)

    implementation(libs.jsoup)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}