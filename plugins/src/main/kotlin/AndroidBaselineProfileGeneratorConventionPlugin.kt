import androidx.baselineprofile.gradle.producer.BaselineProfileProducerExtension
import com.android.build.api.dsl.TestExtension
import com.google.firebase.testlab.gradle.TestLabGradlePluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * Convention plugin for baseline profile generator test modules using Firebase Test Lab.
 *
 * This plugin:
 * 1. Applies necessary plugins (android.test, androidx.baselineprofile, firebase.testlab)
 * 2. Enables test orchestrator
 * 3. Configures baseline profile to use managed devices
 * 4. Configures Firebase Test Lab directoriesToPull
 * 5. Creates tasks to copy and merge profiles from FTL results to expected locations
 * 6. Wires up task dependencies for automatic collection
 *
 * Usage in generator build.gradle.kts:
 * ```kotlin
 * plugins {
 *     id("com.alexrdclement.gradle.plugin.android.baselineprofile.generator")
 * }
 *
 * android {
 *     namespace = "com.example.app.baselineprofile"
 *     targetProjectPath = ":app"
 * }
 *
 * // Optional: customize device configuration (these are the defaults)
 * baselineProfileGenerator {
 *     deviceName = "mediumPhoneApi31Ftl"
 *     deviceType = "MediumPhone.arm"
 *     apiLevel = 31
 *
 *     // For library baseline profile generators: automatically copy to library's src directory
 *     copyToLibrary = ":components"  // omit for app baseline profile generators
 * }
 *
 * // Configure Firebase Test Lab (references extension values to avoid duplication)
 * firebaseTestLab {
 *     managedDevices {
 *         create(baselineProfileGenerator.deviceName) {
 *             device = baselineProfileGenerator.deviceType
 *             apiLevel = baselineProfileGenerator.apiLevel
 *         }
 *     }
 *     val serviceAccountJson = System.getenv("FIREBASE_TEST_LAB_SERVICE_ACCOUNT")
 *     if (serviceAccountJson != null) {
 *         serviceAccountCredentials.set(file(serviceAccountJson))
 *     }
 *     // directoriesToPull is automatically configured by the plugin
 * }
 * ```
 */
class AndroidBaselineProfileGeneratorConventionPlugin : Plugin<Project> {
    private companion object {
        private const val DIRECTORY_TO_PULL_ROOT = "/storage/emulated/0/Android/media"
    }

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.alexrdclement.gradle.plugin.android.test")
                apply("androidx.baselineprofile")
                apply("com.google.firebase.testlab")
            }

            val extension = extensions.create("baselineProfileGenerator", BaselineProfileGeneratorExtension::class.java)

            extensions.configure<TestExtension> {
                defaultConfig {
                    testInstrumentationRunnerArguments["clearPackageData"] = "true"
                }

                testOptions {
                    execution = "ANDROIDX_TEST_ORCHESTRATOR"
                }
            }

            afterEvaluate {
                val androidExtension = extensions.getByName("android") as TestExtension
                val namespace = androidExtension.namespace

                extensions.configure<BaselineProfileProducerExtension> {
                    useConnectedDevices = false
                    managedDevices.add(extension.deviceName)
                }

                extensions.configure<TestLabGradlePluginExtension> {
                    testOptions {
                        results {
                            directoriesToPull.add("$DIRECTORY_TO_PULL_ROOT/$namespace")
                        }
                    }
                }

                tasks.register<Copy>("mergeBaselineProfiles") {
                    description = "Copies and merges baseline profiles from Firebase Test Lab results"
                    group = "baseline profile"

                    val testResultsDir = layout.buildDirectory.dir(
                        "outputs/androidTest-results/managedDevice/nonminifiedrelease/${extension.deviceName}/results"
                    )

                    from(testResultsDir) {
                        include("**/artifacts$DIRECTORY_TO_PULL_ROOT/$namespace/*.txt")

                        eachFile {
                            // Exclude timestamped files (format: *-YYYY-MM-DD-HH-MM-SS.txt)
                            if (name.matches(Regex(".*-\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}\\.txt$"))) {
                                exclude()
                            } else {
                                // Flatten directory structure
                                path = name
                            }
                        }
                        includeEmptyDirs = false

                        configureBaselineProfileFiltering()
                    }

                    val dest = determineBaselineProfileDestination(extension, androidExtension)

                    into(dest)

                    doFirst {
                        dest.mkdirs()
                    }
                }

                tasks.named("${extension.deviceName}NonMinifiedReleaseAndroidTest") {
                    finalizedBy("mergeBaselineProfiles")
                }

                tasks.register("generateBaselineProfile") {
                    description = "Generates baseline profiles using Firebase Test Lab"
                    group = "baseline profile"

                    dependsOn("${extension.deviceName}NonMinifiedReleaseAndroidTest")
                    finalizedBy("mergeBaselineProfiles")
                }
            }
        }
    }

    private fun Project.determineBaselineProfileDestination(
        extension: BaselineProfileGeneratorExtension,
        androidExtension: TestExtension
    ): File {
        return when {
            extension.copyToLibrary != null -> {
                val libProject = project(extension.copyToLibrary!!)
                libProject.layout.projectDirectory.dir("src/androidMain/generated/baselineProfiles").asFile
            }
            androidExtension.targetProjectPath != null -> {
                val appProject = project(androidExtension.targetProjectPath!!)
                appProject.layout.projectDirectory.dir("src/release/generated/baselineProfiles").asFile
            }
            else -> {
                throw IllegalStateException("Either targetProjectPath or copyToLibrary must be set")
            }
        }
    }

    /**
     * Configures file filtering and renaming for baseline profiles.
     * - Excludes timestamped files (format: *-YYYY-MM-DD-HH-MM-SS.txt)
     * - Renames test-prefixed files to canonical names (e.g., -startup-prof.txt -> startup-prof.txt)
     *
     * Note: This assumes files have already been included via the parent CopySpec.
     */
    private fun CopySpec.configureBaselineProfileFiltering() {
        // Exclude timestamped files (format: *-YYYY-MM-DD-HH-MM-SS.txt)
        exclude("*-????-??-??-??-??-??.txt")
        rename { filename ->
            when {
                filename.contains("-startup-prof.txt") -> "startup-prof.txt"
                filename.contains("-baseline-prof.txt") -> "baseline-prof.txt"
                else -> filename
            }
        }
    }
}

/**
 * Extension for configuring the baseline profile generator.
 */
open class BaselineProfileGeneratorExtension {
    var deviceName: String = "mediumPhoneApi31Ftl"

    var deviceType: String = "MediumPhone.arm"

    var apiLevel: Int = 31

    /**
     * Optional: The library project to copy baseline profiles to (e.g., ":components", ":modifiers").
     *
     * When set:
     * - Profiles are copied to `{library}/src/androidMain/generated/baselineProfiles/`
     *
     * When null:
     * - Profiles are copied to `{app}/src/release/generated/baselineProfiles/`
     * - Requires `android.targetProjectPath` to be set
     */
    var copyToLibrary: String? = null
}
