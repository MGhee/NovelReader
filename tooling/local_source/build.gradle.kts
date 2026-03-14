plugins {
    alias(libs.plugins.novelreader.android.library)
    alias(libs.plugins.novelreader.android.compose)
}

android {
    namespace = "my.novelreader.tooling.local_source"
}

dependencies {
    implementation(projects.core)
    implementation(projects.coreui)
    implementation(projects.strings)
    implementation(projects.data)
    implementation(projects.scraper)
    implementation(projects.networking)
    implementation(projects.tooling.epubParser)
    implementation(projects.tooling.localDatabase)

    implementation(libs.timber)
    implementation(libs.androidx.documentfile)
    implementation(libs.compose.androidx.activity)
    implementation(libs.compose.material3.android)
    implementation(libs.compose.androidx.material.icons.extended)
}