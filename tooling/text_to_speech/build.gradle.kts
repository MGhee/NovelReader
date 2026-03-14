plugins {
    alias(libs.plugins.novelreader.android.library)
    alias(libs.plugins.novelreader.android.compose)
}

android {
    namespace = "my.novelreader.tooling.texttospeech"
}

dependencies {
    implementation(projects.tooling.algorithms)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.test.junit)
}