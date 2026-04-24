plugins {
    alias(libs.plugins.novelreader.android.library)
    alias(libs.plugins.novelreader.android.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "my.novelreader.features.mangareader"

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(projects.core)
    implementation(projects.coreui)
    implementation(projects.data)
    implementation(projects.navigation)
    implementation(projects.scraper)
    implementation(projects.networking)
    implementation(projects.tooling.localDatabase)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Image viewing with subsampling/tiling for large manga pages
    implementation("com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0")

    // ViewPager2 for pager-mode viewer
    implementation(libs.androidx.viewpager2)

    // Material & Compose
    implementation(libs.material)
    implementation(libs.compose.material3.android)
    implementation(libs.compose.coil)
    implementation(libs.compose.androidx.material.icons.extended)

    // Activity & Lifecycle
    implementation(libs.compose.androidx.activity)
    implementation(libs.compose.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Android core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    testImplementation(libs.test.junit)
}
