plugins {
    alias(libs.plugins.novelreader.android.library)
    alias(libs.plugins.novelreader.android.compose)
}

android {
    namespace = "my.novelreader.networking"
}

dependencies {

    implementation(projects.core)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.okhttp.interceptor.brotli)
    implementation(libs.okhttp.interceptor.logging)
    implementation(libs.okhttp.glideIntegration)

    // Logging
    implementation(libs.timber)

    implementation(libs.jsoup)
    implementation(libs.gson)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
}