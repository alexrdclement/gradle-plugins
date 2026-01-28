import com.alexrdclement.gradle.plugin.Libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
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
                api(multiplatformExtension.foundation)
                api(multiplatformExtension.runtime)
                api(multiplatformExtension.ui)
                api(multiplatformExtension.uiToolingPreview)
            }

            findByName("jvmMain")?.apply {
                dependencies {
                    api(multiplatformExtension.desktopCurrentOs)
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

    val uiTooling: String = Libs.composeMultiplatformUiTooling
    val uiToolingPreview: String = Libs.composeMultiplatformUiToolingPreview

    val desktopCurrentOs: String = Libs.composeMultiplatformDesktop
}
