plugins {
    alias(libs.plugins.novelreader.android.library)
}

android {
    namespace = "my.novelreader.algorithms"
}

dependencies {
    implementation(libs.test.junit)
}
