import com.alexrdclement.gradle.plugin.Libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ComposeMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val composeMultiplatform = extensions.create("composeMultiplatform", ComposeMultiplatformExtension::class.java)

        afterEvaluate {
            configureCompose(composeMultiplatform)
        }
    }
}

fun Project.configureCompose(multiplatformExtension: ComposeMultiplatformExtension) {
    composeCompiler {
        includeSourceInformation.set(true)
    }

    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.apply {
            getByName("commonMain").dependencies {
                implementation(multiplatformExtension.uiToolingPreview)
                implementation(multiplatformExtension.runtime)
                implementation(multiplatformExtension.ui)
                implementation(multiplatformExtension.foundation)
            }

            findByName("androidMain")?.apply {
                dependencies {
                    implementation(multiplatformExtension.preview)

                    // TODO: revisit in later versions of Otter. See https://issuetracker.google.com/issues/422373442
                    implementation(multiplatformExtension.uiTooling)
                    implementation(ComposeMultiplatformExtension.Android.activityCompose)
                    implementation(ComposeMultiplatformExtension.Android.customview)
                    implementation(ComposeMultiplatformExtension.Android.emoji2)
                    implementation(ComposeMultiplatformExtension.Android.uiTestManifest)
                }
            }

            findByName("jvmMain")?.apply {
                dependencies {
                    implementation(multiplatformExtension.desktopCurrentOs)
                }
            }
        }
    }
}

fun Project.composeCompiler(block: ComposeCompilerGradlePluginExtension.() -> Unit) {
    extensions.configure<ComposeCompilerGradlePluginExtension>(block)
}

open class ComposeMultiplatformExtension {
    val ui: String = Libs.composeMultiplatformUi
    val foundation: String = Libs.composeMultiplatformFoundation
    val runtime: String = Libs.composeMultiplatformRuntime

    val preview: String = Libs.composeMultiplatformUiToolingPreview
    val uiTooling: String = Libs.composeMultiplatformUiTooling
    val uiToolingPreview: String = Libs.composeMultiplatformUiToolingPreview

    val desktopCurrentOs: String = Libs.composeMultiplatformDesktop

    object Android {
        val activityCompose: String = Libs.androidxActivityCompose
        val customview: String = Libs.androidxCustomview
        val emoji2: String = Libs.androidxEmoji2
        val uiTestManifest: String = Libs.composeUiTestManifest
    }
}
