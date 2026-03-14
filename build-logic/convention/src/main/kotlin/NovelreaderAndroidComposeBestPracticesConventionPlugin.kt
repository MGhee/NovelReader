
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import my.novelreader.convention.plugin.implementation
import my.novelreader.convention.plugin.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType

class NovelreaderAndroidComposeBestPracticesConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val extension = extensions.findByType<ApplicationExtension>()
                ?: extensions.getByType<LibraryExtension>()

            extension.apply {
                buildFeatures {
                    compose = true
                }

                composeOptions {
                    kotlinCompilerExtensionVersion =
                        libs.findVersion("kotlin-compose-compilerVersion").get().toString()
                }

                dependencies {
                    implementation(libs.findLibrary("compose-androidx-ui").get())
                    implementation(libs.findLibrary("compose-androidx-ui-tooling").get())
                }

                testOptions {
                    unitTests {
                        // For Robolectric
                        isIncludeAndroidResources = true
                    }
                }
            }

        }
    }
}
