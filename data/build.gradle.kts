plugins {
    alias(libs.plugins.novelreader.android.library)
    alias(libs.plugins.novelreader.android.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "my.novelreader.data"
}

dependencies {
    implementation(projects.core)
    implementation(projects.networking)
    implementation(projects.scraper)
    implementation(projects.tooling.localDatabase)
    implementation(projects.tooling.epubParser)

    implementation(libs.jsoup)
    implementation(libs.readability4j)
    implementation(libs.gson)
    implementation(libs.moshi.kotlin)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.timber)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    testImplementation(libs.test.junit)
}