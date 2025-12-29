import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * Convention plugin for consuming baseline profiles in Kotlin Multiplatform libraries.
 *
 * This plugin works around the limitation that the androidx.baselineprofile plugin
 * expects the old com.android.library plugin's LibraryExtension, but KMP libraries
 * use com.android.kotlin.multiplatform.library which doesn't expose that extension.
 *
 * The plugin:
 * 1. Creates a "baselineProfile" configuration manually
 * 2. Creates a "copyBaselineProfile" task that copies generated profiles to src/androidMain/generated/baselineProfiles/baseline-prof.txt
 *
 * Usage in consumer build.gradle.kts:
 * ```kotlin
 * plugins {
 *     id("com.alexrdclement.gradle.plugin.android.baselineprofile.consumer")
 * }
 *
 * dependencies {
 *     baselineProfile(projects.moduleName.baselineProfile)
 * }
 * ```
 *
 * Usage in generator module:
 * - `tasks.configureEach { if (name == "collectNonMinifiedReleaseBaselineProfile") finalizedBy(":library:copyBaselineProfile") }`
 * - Or run manually: `./gradlew :library:copyBaselineProfile`
 */
class AndroidBaselineProfileConsumerConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Create the baselineProfile configuration with appropriate attributes
            val baselineProfileConfiguration = configurations.create("baselineProfile") {
                isCanBeConsumed = false
                isCanBeResolved = true

                // Request baseline profile artifacts
                attributes {
                    attribute(
                        org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE,
                        project.objects.named(org.gradle.api.attributes.Usage::class.java, "baselineProfile")
                    )
                }
            }

            // Create task to copy baseline profiles (do this early, not in afterEvaluate)
            tasks.register<Copy>("copyBaselineProfile") {
                description = "Copies baseline profile from generator module to src/androidMain/generated/baselineProfiles/baseline-prof.txt"
                group = "baseline profile"

                from(baselineProfileConfiguration) {
                    include("*.txt")
                    rename { "baseline-prof.txt" }
                }

                // Copy to the src directory for the Android target so it's committed to source control
                val outputDir = File(project.projectDir, "src/androidMain/generated/baselineProfiles")
                into(outputDir)

                doFirst {
                    outputDir.mkdirs()
                    // Clean any existing profile files before copying
                    outputDir.listFiles()?.forEach { it.delete() }
                }
            }

            // Note: The copyBaselineProfile task is not automatically wired up here because
            // the collectNonMinifiedReleaseBaselineProfile task exists in the *generator*
            // module (e.g., :components:baseline-profile), not in this consumer module.
            // Wire it up manually in the generator module's build.gradle.kts instead.
        }
    }
}
