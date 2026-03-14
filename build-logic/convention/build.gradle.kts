import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "my.novelreader.convention.plugin"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "novelreader.android.application"
            implementationClass =
                "NovelreaderAndroidApplicationBestPracticesConventionPlugin" // :)
        }
        register("androidLibrary") {
            id = "novelreader.android.library"
            implementationClass =
                "NovelreaderAndroidLibraryBestPracticesConventionPlugin" // ;)
        }
        register("androidCompose") {
            id = "novelreader.android.compose"
            implementationClass =
                "NovelreaderAndroidComposeBestPracticesConventionPlugin" // :)
        }
    }
}
