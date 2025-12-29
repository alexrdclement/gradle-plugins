import com.alexrdclement.gradle.plugin.AndroidLibraryTargetConfiguration
import com.alexrdclement.gradle.plugin.IosFrameworkConfiguration
import com.alexrdclement.gradle.plugin.IosLibraryTargetConfiguration
import com.alexrdclement.gradle.plugin.configure
import com.alexrdclement.gradle.plugin.configureKotlin
import com.alexrdclement.gradle.plugin.configureKotlinMultiplatformAndroidLibrary
import com.android.build.api.dsl.androidLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KotlinMultiplatformLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform")
            apply("com.android.kotlin.multiplatform.library")
        }

        configureKotlin()
    }
}

fun Project.libraryTargets(
    androidNamespace: String,
    iosFrameworkBaseName: String,
    iosFrameworkIsStatic: Boolean = true
) {
    libraryTargets(
        androidConfiguration = AndroidLibraryTargetConfiguration(
            namespace = androidNamespace
        ),
        iosConfiguration = IosLibraryTargetConfiguration(
            framework = IosFrameworkConfiguration(
                baseName = iosFrameworkBaseName,
                isStatic = iosFrameworkIsStatic
            )
        )
    )
}

fun Project.libraryTargets(
    androidConfiguration: AndroidLibraryTargetConfiguration,
    iosConfiguration: IosLibraryTargetConfiguration
) {
    extensions.configure<KotlinMultiplatformExtension> {
        applyDefaultHierarchyTemplate()

        androidLibrary {
            configureKotlinMultiplatformAndroidLibrary(this, androidConfiguration)
        }

        jvm()

        val iosFrameworkConfiguration = iosConfiguration.framework
        iosArm64 {
            binaries.framework {
                configure(iosFrameworkConfiguration)
            }
        }
        iosSimulatorArm64 {
            binaries.framework {
                configure(iosFrameworkConfiguration)
            }
        }

        @OptIn(ExperimentalWasmDsl::class)
        wasmJs {
            browser()
            binaries.executable()
        }

        sourceSets {
            commonTest {
                dependencies {
                    implementation(kotlin("test"))
                }
            }
        }
    }
}
