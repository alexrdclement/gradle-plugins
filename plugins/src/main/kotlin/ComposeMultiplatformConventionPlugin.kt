import com.alexrdclement.gradle.plugin.Libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ComposeMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        afterEvaluate {
            configureCompose()
        }
    }
}

fun Project.configureCompose() {
    composeCompiler {
        includeSourceInformation.set(true)
    }

    val compose = extensions.getByType<ComposeExtension>()
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.apply {
            getByName("commonMain").dependencies {
                implementation(compose.dependencies.components.uiToolingPreview)
                implementation(compose.dependencies.runtime)
                implementation(compose.dependencies.ui)
                implementation(compose.dependencies.foundation)
            }

            findByName("androidMain")?.apply {
                dependencies {
                    implementation(compose.dependencies.preview)

                    // TODO: revisit in later versions of Otter. See https://issuetracker.google.com/issues/422373442
                    implementation(compose.dependencies.uiTooling)
                    implementation(Libs.androidxActivityCompose)
                    implementation(Libs.androidxCustomview)
                    implementation(Libs.androidxEmoji2)
                    implementation(Libs.composeUiTestManifest)
                }
            }

            findByName("jvmMain")?.apply {
                dependencies {
                    implementation(compose.dependencies.desktop.currentOs)
                }
            }
        }
    }
}

fun Project.composeCompiler(block: ComposeCompilerGradlePluginExtension.() -> Unit) {
    extensions.configure<ComposeCompilerGradlePluginExtension>(block)
}
