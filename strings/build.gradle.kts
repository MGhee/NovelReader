plugins {
    alias(libs.plugins.novelreader.android.library)
}

android {
    namespace = "my.novelreader.strings"
    androidResources {
        resourcePrefix = ""
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}