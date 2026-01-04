import com.android.build.api.dsl.TestExtension
import com.google.firebase.testlab.gradle.TestLabGradlePluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/**
 * Convention plugin for benchmark test modules using Firebase Test Lab.
 *
 * This plugin:
 * 1. Applies necessary plugins (android.test, firebase.testlab)
 * 2. Configures Firebase Test Lab with physical devices
 * 3. Configures Firebase Test Lab directoriesToPull for benchmark results
 * 4. Creates tasks to copy benchmark JSON from FTL results to source control
 * 5. Wires up task dependencies for automatic collection
 *
 * Usage in benchmark build.gradle.kts:
 * ```kotlin
 * plugins {
 *     id("com.alexrdclement.gradle.plugin.android.benchmark")
 * }
 *
 * android {
 *     namespace = "com.example.app.benchmark"
 *     targetProjectPath = ":app"
 *
 *     buildTypes {
 *         create("benchmark") {
 *             signingConfig = signingConfigs.getByName("debug")
 *             matchingFallbacks += listOf("release")
 *         }
 *     }
 *
 *     experimentalProperties["android.experimental.self-instrumenting"] = true
 * }
 *
 * // Optional: customize device configuration (these are the defaults)
 * benchmark {
 *     buildType = "benchmarkRelease"  // Must match androidApp build type
 *     deviceName = "pixel5Api34Ftl"
 *     deviceType = "akita"  // Physical device
 *     apiLevel = 34
 *     resultsDir = "results"  // Relative to benchmark module
 * }
 *
 * // Configure Firebase Test Lab (references extension values to avoid duplication)
 * firebaseTestLab {
 *     managedDevices {
 *         create(benchmark.deviceName) {
 *             device = benchmark.deviceType
 *             apiLevel = benchmark.apiLevel
 *         }
 *     }
 *     val serviceAccountJson = System.getenv("FIREBASE_TEST_LAB_SERVICE_ACCOUNT")
 *     if (serviceAccountJson != null) {
 *         serviceAccountCredentials.set(file(serviceAccountJson))
 *     }
 *     testOptions {
 *         results.cloudStorageBucket = "your-cloud-storage-bucket"
 *     }
 * }
 *
 * androidComponents {
 *     beforeVariants(selector().all()) {
 *         it.enable = it.buildType == "benchmark"
 *     }
 * }
 * ```
 */
class AndroidBenchmarkConventionPlugin : Plugin<Project> {
    private companion object {
        private const val DIRECTORY_TO_PULL_ROOT = "/storage/emulated/0/Android/media"
    }

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.alexrdclement.gradle.plugin.android.test")
                apply("com.google.firebase.testlab")
            }

            val extension = extensions.create("benchmark", BenchmarkExtension::class.java)

            afterEvaluate {
                val androidExtension = extensions.getByName("android") as TestExtension
                val namespace = androidExtension.namespace

                extensions.configure<TestLabGradlePluginExtension> {
                    testOptions {
                        results {
                            directoriesToPull.add("$DIRECTORY_TO_PULL_ROOT/$namespace")
                        }
                    }
                }

                tasks.register<Copy>("copyBenchmarkResults") {
                    description = "Copies benchmark JSON from Firebase Test Lab results"
                    group = "benchmark"

                    val testResultsDir = layout.buildDirectory.dir(
                        "outputs/androidTest-results/managedDevice/${extension.buildType.lowercase()}/${extension.deviceName}/results"
                    )

                    from(testResultsDir) {
                        include("**/artifacts$DIRECTORY_TO_PULL_ROOT/$namespace/$namespace-benchmarkData.json")

                        eachFile {
                            // Flatten directory structure, keep original filename
                            path = name
                        }
                        includeEmptyDirs = false
                    }

                    val resultsDir = layout.projectDirectory.dir(extension.resultsDir)
                    into(resultsDir)

                    doFirst {
                        resultsDir.asFile.mkdirs()
                    }
                }

                // Find the benchmark Android test task and wire it up
                val capitalizedBuildType = extension.buildType.replaceFirstChar { it.uppercase() }
                tasks.named("${extension.deviceName}${capitalizedBuildType}AndroidTest") {
                    finalizedBy("copyBenchmarkResults")
                }

                tasks.register("runBenchmarks") {
                    description = "Runs benchmarks on Firebase Test Lab"
                    group = "benchmark"

                    dependsOn("${extension.deviceName}${capitalizedBuildType}AndroidTest")
                    finalizedBy("copyBenchmarkResults")
                }
            }
        }
    }
}

open class BenchmarkExtension {
    var buildType: String = "benchmarkRelease"
    var deviceName: String = "pixel8aApi34Ftl"
    var deviceType: String = "akita"
    var apiLevel: Int = 34
    var resultsDir: String = "results"
}
