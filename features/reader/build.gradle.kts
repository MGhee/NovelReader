plugins {
    alias(libs.plugins.novelreader.android.library)
    alias(libs.plugins.novelreader.android.compose)
}

android {
    namespace = "my.novelreader.reader"

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(projects.core)
    implementation(projects.coreui)
    implementation(projects.data)
    implementation(projects.navigation)
    implementation(projects.tooling.localDatabase)
    implementation(projects.tooling.textToSpeech)
    implementation(projects.tooling.textTranslator.domain)
    implementation(projects.tooling.algorithms)

    implementation(libs.material)
    implementation(libs.androidx.media)
    implementation(libs.compose.landscapist.glide)
    implementation(libs.compose.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.compose.androidx.activity)
    implementation(libs.compose.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.viewpager2)
    implementation(libs.compose.material3.android)
    
    // Kotlin reflection for dynamic batch translator lookup
    implementation(kotlin("reflect"))

    testImplementation(libs.test.junit)
}