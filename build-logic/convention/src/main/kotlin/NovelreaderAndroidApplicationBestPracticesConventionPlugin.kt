import com.android.build.api.dsl.ApplicationExtension
import my.novelreader.convention.plugin.appConfig
import my.novelreader.convention.plugin.applyHilt
import my.novelreader.convention.plugin.configureAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class NovelreaderAndroidApplicationBestPracticesConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }
            applyHilt()

            extensions.configure<ApplicationExtension> {
                configureAndroid(this)
                defaultConfig.targetSdk = appConfig.TARGET_SDK
            }
        }
    }

}
