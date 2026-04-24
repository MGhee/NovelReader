plugins {
    alias(libs.plugins.novelreader.android.library)
    alias(libs.plugins.novelreader.android.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "my.novelreader.tooling.backup_restore"
}

dependencies {
    implementation(projects.core)
    implementation(projects.coreui)
    implementation(projects.strings)
    implementation(projects.data)
    implementation(projects.tooling.localDatabase)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
    implementation(libs.compose.androidx.activity)
    implementation(libs.compose.material3.android)
    implementation(libs.compose.androidx.material.icons.extended)
    implementation(libs.compose.landscapist.glide)
    implementation(libs.compose.coil)
}