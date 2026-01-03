import com.android.build.api.dsl.TestExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * Convention plugin for baseline profile generator test modules using Firebase Test Lab.
 *
 * This plugin:
 * 1. Applies necessary plugins (android.test, androidx.baselineprofile, firebase.testlab)
 * 2. Enables test orchestrator for better test isolation
 * 3. Creates a task to copy profiles from FTL results to expected location
 * 4. Wires up task dependencies for automatic collection
 *
 * Note: Firebase Test Lab configuration (device setup, credentials, directoriesToPull) must still
 * be done manually in the module's build.gradle.kts file, as the Firebase Test Lab plugin doesn't
 * expose a public API for configuration from convention plugins.
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
 *
 * Note: The following are automatically configured by the plugin:
 * - baselineProfile { useConnectedDevices, managedDevices }
 * - firebaseTestLab { testOptions { results { directoriesToPull } } }
 */
class AndroidBaselineProfileGeneratorConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Apply required plugins
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

                // Automatically configure baseline profile to use the managed device from extension
                extensions.findByName("baselineProfile")?.let { bpExtension ->
                    try {
                        bpExtension::class.java.getMethod("setUseConnectedDevices", Boolean::class.javaPrimitiveType).invoke(bpExtension, false)
                        @Suppress("UNCHECKED_CAST")
                        val managedDevices = bpExtension::class.java.getMethod("getManagedDevices").invoke(bpExtension) as MutableList<String>
                        managedDevices.add(extension.deviceName)
                    } catch (e: Exception) {
                        logger.warn("Could not configure baseline profile automatically: ${e.message}")
                    }
                }

                // Automatically configure Firebase Test Lab directoriesToPull
                extensions.findByName("firebaseTestLab")?.let { ftlExtension ->
                    try {
                        // Get testOptions
                        val getTestOptionsMethod = ftlExtension::class.java.getMethod("getTestOptions")
                        val testOptions = getTestOptionsMethod.invoke(ftlExtension)

                        // Get results
                        val getResultsMethod = testOptions::class.java.getMethod("getResults")
                        val results = getResultsMethod.invoke(testOptions)

                        // Get directoriesToPull (it's a ListProperty, not a List)
                        val directoriesToPull = results::class.java.getMethod("getDirectoriesToPull").invoke(results)

                        // Add to the property using add() method
                        directoriesToPull::class.java.getMethod("add", Any::class.java).invoke(
                            directoriesToPull,
                            "/storage/emulated/0/Android/media/$namespace"
                        )
                    } catch (e: Exception) {
                        logger.warn("Could not configure Firebase Test Lab directoriesToPull automatically: ${e.message}")
                        logger.warn("Please add directoriesToPull manually in firebaseTestLab { testOptions { results { ... } } }")
                    }
                }

                // Create task to copy baseline profiles from FTL results to intermediate location
                tasks.register<Copy>("copyBaselineProfilesFromFtl") {
                    description = "Copies baseline profiles from Firebase Test Lab results to build output"
                    group = "baseline profile"

                    val testResultsDir = layout.buildDirectory.dir(
                        "outputs/androidTest-results/managedDevice/nonminifiedrelease/${extension.deviceName}/results"
                    )
                    val outputDir = layout.buildDirectory.dir(
                        "outputs/managed_device_android_test_additional_output/${extension.deviceName}"
                    )

                    from(testResultsDir) {
                        include("**/artifacts/storage/emulated/0/Android/media/$namespace/*.txt")
                        eachFile {
                            // Flatten the directory structure
                            path = name
                        }
                        includeEmptyDirs = false
                    }

                    into(outputDir)

                    doFirst {
                        outputDir.get().asFile.mkdirs()
                    }
                }

                // Wire up the FTL test task to copy results after completion
                tasks.named("${extension.deviceName}NonMinifiedReleaseAndroidTest") {
                    finalizedBy("copyBaselineProfilesFromFtl")
                }

                // Create simplified mergeBaselineProfiles task that bypasses the problematic collect task
                tasks.register<Copy>("mergeBaselineProfiles") {
                    description = "Merges baseline profiles from FTL output (simplified, bypasses collect task)"
                    group = "baseline profile"

                    // Source: FTL output directory
                    val ftlOutputDir = layout.buildDirectory.dir(
                        "outputs/managed_device_android_test_additional_output/${extension.deviceName}"
                    )

                    // Determine destination based on target project
                    val targetProjectPath = androidExtension.targetProjectPath
                    val dest = when {
                        // Library module - copy to library's src directory
                        extension.copyToLibrary != null -> {
                            val libProject = project(extension.copyToLibrary!!)
                            libProject.layout.projectDirectory.dir("src/androidMain/generated/baselineProfiles")
                        }
                        // App module - copy to app's release directory
                        targetProjectPath != null -> {
                            val appProject = project(targetProjectPath)
                            appProject.layout.projectDirectory.dir("src/release/generated/baselineProfiles")
                        }
                        else -> {
                            throw IllegalStateException("Either targetProjectPath or copyToLibrary must be set")
                        }
                    }

                    from(ftlOutputDir) {
                        include("*.txt")
                        // Exclude timestamped files (format: *-YYYY-MM-DD-HH-MM-SS.txt)
                        // Only use the canonical non-timestamped versions
                        exclude("*-????-??-??-??-??-??.txt")
                        // Rename -startup-prof.txt to startup-prof.txt (remove test name prefix)
                        rename { filename ->
                            when {
                                filename.contains("-startup-prof.txt") -> "startup-prof.txt"
                                filename.contains("-baseline-prof.txt") -> "baseline-prof.txt"
                                else -> filename
                            }
                        }
                    }

                    into(dest)

                    dependsOn("copyBaselineProfilesFromFtl")

                    doFirst {
                        dest.asFile.mkdirs()
                    }
                }

                // Create a device-agnostic task for convenience - skips the problematic collect task
                tasks.register("generateBaselineProfile") {
                    description = "Generates baseline profiles using Firebase Test Lab (simplified workflow)"
                    group = "baseline profile"

                    dependsOn("${extension.deviceName}NonMinifiedReleaseAndroidTest")
                    finalizedBy("mergeBaselineProfiles")
                }
            }
        }
    }
}

/**
 * Extension for configuring the baseline profile generator.
 * This is the single source of truth for device configuration.
 */
open class BaselineProfileGeneratorExtension {
    /**
     * The managed device name (e.g., "mediumPhoneApi31Ftl").
     * Used for task names, paths, and device configuration.
     */
    var deviceName: String = "mediumPhoneApi31Ftl"

    /**
     * The Firebase Test Lab device type (e.g., "MediumPhone.arm", "Pixel6.arm").
     */
    var deviceType: String = "MediumPhone.arm"

    /**
     * The Android API level for the device.
     */
    var apiLevel: Int = 31

    /**
     * Optional: The library project to copy baseline profiles to (e.g., ":components").
     * If set, the plugin will automatically wire up the collection task to trigger
     * the library's copyBaselineProfile task.
     * Leave null for app baseline profile generators that don't need to copy to a library.
     */
    var copyToLibrary: String? = null
}
