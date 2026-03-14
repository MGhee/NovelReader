plugins {
    alias(libs.plugins.novelreader.android.library)
}

android {
    namespace = "my.novelreader.navigation"
}

dependencies {
    implementation(projects.core)
    implementation(projects.tooling.localDatabase)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
}