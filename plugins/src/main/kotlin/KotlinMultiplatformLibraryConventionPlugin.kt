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

enum class KotlinTarget {
    ANDROID,
    JVM,
    IOS,
    WASM,
    ;
}

/**
 * Configure a Kotlin multiplatform library project.
 *
 * Example usage:
 * ```kotlin
 * kotlin {
 *     libraryTargets(
 *         androidNamespace = "com.alexrdclement.library",
 *         iosFrameworkBaseName = "MyLibrary",
 *     )
 * ...
 * }
 * ```
 *
 * Optionally configure enabled targets via `libraryTargets`:
 * ```kotlin
 * kotlin {
 *     libraryTargets(
 *         androidNamespace = "com.alexrdclement.library",
 *         iosFrameworkBaseName = "MyLibrary",
 *         targets = setOf(KotlinTarget.ANDROID, KotlinTarget.IOS),
 *     )
 * ...
 * ```
 *
 * or local.properties:
 * ```
 * kotlin.target.android=true
 * kotlin.target.ios=false
 * ```
 *
 * Defaults to the full set of targets if not specified.
 */
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
    iosFrameworkIsStatic: Boolean = true,
    targets: Set<KotlinTarget>? = null
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
        ),
        targets = targets
    )
}

fun Project.libraryTargets(
    androidConfiguration: AndroidLibraryTargetConfiguration,
    iosConfiguration: IosLibraryTargetConfiguration,
    targets: Set<KotlinTarget>? = null
) {
    val enabledTargets = targets ?: loadTargetsFromProperties()

    extensions.configure<KotlinMultiplatformExtension> {
        applyDefaultHierarchyTemplate()

        if (KotlinTarget.ANDROID in enabledTargets) {
            androidLibrary {
                configureKotlinMultiplatformAndroidLibrary(this, androidConfiguration)
            }
        }

        if (KotlinTarget.JVM in enabledTargets) {
            jvm()
        }

        if (KotlinTarget.IOS in enabledTargets) {
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
        }

        if (KotlinTarget.WASM in enabledTargets) {
            @OptIn(ExperimentalWasmDsl::class)
            wasmJs {
                browser()
                binaries.executable()
            }
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

private fun Project.loadTargetsFromProperties(): Set<KotlinTarget> {
    val localPropsFile = rootProject.file("local.properties")
    if (!localPropsFile.exists()) {
        return KotlinTarget.entries.toSet() // All targets enabled by default
    }

    val localProps = java.util.Properties()
    localPropsFile.inputStream().use { localProps.load(it) }

    // Start with all targets, remove explicitly disabled ones
    return KotlinTarget.entries.toMutableSet().apply {
        if (localProps.getProperty("kotlin.target.android") == "false") remove(KotlinTarget.ANDROID)
        if (localProps.getProperty("kotlin.target.jvm") == "false") remove(KotlinTarget.JVM)
        if (localProps.getProperty("kotlin.target.ios") == "false") remove(KotlinTarget.IOS)
        if (localProps.getProperty("kotlin.target.wasm") == "false") remove(KotlinTarget.WASM)
    }
}
