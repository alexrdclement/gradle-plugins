import com.alexrdclement.gradle.plugin.Libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidComposeTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                add("debugImplementation", Libs.composeUiTestManifest)
                add("androidTestImplementation", Libs.composeUiTestJunit4)
            }
        }
    }
}
