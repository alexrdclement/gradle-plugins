import com.alexrdclement.gradle.plugin.AndroidMinSdk
import com.alexrdclement.gradle.plugin.Libs
import com.alexrdclement.gradle.plugin.configureKotlinAndroid
import com.alexrdclement.gradle.plugin.disableUnnecessaryAndroidTests
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                defaultConfig.minSdk = AndroidMinSdk
                testOptions {
                    unitTests {
                        isReturnDefaultValues = true
                    }
                }
            }
            extensions.configure<LibraryAndroidComponentsExtension> {
                disableUnnecessaryAndroidTests(target)
            }
            dependencies {
                add("testImplementation", kotlin("test"))

                add("androidTestImplementation", kotlin("test"))
                add("androidTestImplementation", Libs.androidxTestCore)
                add("androidTestImplementation", Libs.androidxTestRunner)
                add("androidTestImplementation", Libs.androidxTestExtJunit)
            }
        }
    }
}
