import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class AndroidInstrumentedTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.firebase.testlab")

            val extension = extensions.create<InstrumentedTestExtension>("instrumentedTest")

            afterEvaluate {
                tasks.register("runInstrumentedTests") {
                    description = "Runs instrumented tests on Firebase Test Lab"
                    group = "verification"
                    dependsOn("${extension.deviceName}DebugAndroidTest")
                }
            }
        }
    }
}

open class InstrumentedTestExtension {
    var deviceName: String = "mediumPhoneApi30Ftl"
    var deviceType: String = "MediumPhone.arm"
    var apiLevel: Int = 31
}
