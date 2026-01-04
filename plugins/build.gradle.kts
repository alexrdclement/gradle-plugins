import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    alias(alexrdclementPluginLibs.plugins.maven.publish)
    id("generate-libs")
}

group = "com.alexrdclement.gradle.plugin"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(alexrdclementPluginLibs.android.gradle.plugin)
    implementation(alexrdclementPluginLibs.android.kotlin.multiplatform.library.plugin)
    implementation(alexrdclementPluginLibs.android.tools.common)
    implementation(alexrdclementPluginLibs.androidx.baselineprofile.plugin)
    implementation(alexrdclementPluginLibs.jetbrains.compose)
    implementation(alexrdclementPluginLibs.compose.compiler.plugin)
    implementation(alexrdclementPluginLibs.kotlin.gradle.plugin)
    implementation(alexrdclementPluginLibs.kotlin.multiplatform.plugin)
    implementation(alexrdclementPluginLibs.kotlin.serialization.plugin)
    implementation(alexrdclementPluginLibs.hilt.gradle.plugin)
    implementation(alexrdclementPluginLibs.ksp.gradle.plugin)
    implementation(alexrdclementPluginLibs.room.gradle.plugin)
    implementation(alexrdclementPluginLibs.shipkit.autoversion.plugin)
    implementation(alexrdclementPluginLibs.shipkit.changelog.plugin)
    implementation(alexrdclementPluginLibs.maven.publish.plugin)
    implementation(alexrdclementPluginLibs.firebase.testlab.plugin)

    testImplementation(alexrdclementPluginLibs.junit.jupiter)
    testImplementation(alexrdclementPluginLibs.junit.platform.launcher)
    testImplementation(gradleTestKit())
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }

    test {
        useJUnitPlatform()
    }
}

gradlePlugin {
    plugins {
        register("androidApplicationCompose") {
            id = "com.alexrdclement.gradle.plugin.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidApplication") {
            id = "com.alexrdclement.gradle.plugin.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidBaselineProfileConsumer") {
            id = "com.alexrdclement.gradle.plugin.android.baselineprofile.consumer"
            implementationClass = "AndroidBaselineProfileConsumerConventionPlugin"
        }
        register("androidBaselineProfileGenerator") {
            id = "com.alexrdclement.gradle.plugin.android.baselineprofile.generator"
            implementationClass = "AndroidBaselineProfileGeneratorConventionPlugin"
        }
        register("androidBenchmark") {
            id = "com.alexrdclement.gradle.plugin.android.benchmark"
            implementationClass = "AndroidBenchmarkConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "com.alexrdclement.gradle.plugin.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "com.alexrdclement.gradle.plugin.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryTestFixtures") {
            id = "com.alexrdclement.gradle.plugin.android.library.test.fixtures"
            implementationClass = "AndroidLibraryTestFixturesConventionPlugin"
        }
        register("androidTest") {
            id = "com.alexrdclement.gradle.plugin.android.test"
            implementationClass = "AndroidTestConventionPlugin"
        }
        register("androidHilt") {
            id = "com.alexrdclement.gradle.plugin.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidHiltTestFixtures") {
            id = "com.alexrdclement.gradle.plugin.android.hilt.test.fixtures"
            implementationClass = "AndroidHiltTestFixturesConventionPlugin"
        }
        register("androidRoom") {
            id = "com.alexrdclement.gradle.plugin.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("composeMultiplatform") {
            id = "com.alexrdclement.gradle.plugin.compose.multiplatform"
            implementationClass = "ComposeMultiplatformConventionPlugin"
        }
        register("desktopApplication") {
            id = "com.alexrdclement.gradle.plugin.desktop.application"
            implementationClass = "DesktopApplicationConventionPlugin"
        }
        register("githubRelease") {
            id = "com.alexrdclement.gradle.plugin.github.release"
            implementationClass = "GithubReleaseConventionPlugin"
        }
        register("jvmLibrary") {
            id = "com.alexrdclement.gradle.plugin.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("kotlinMultiplatformLibrary") {
            id = "com.alexrdclement.gradle.plugin.kotlin.multiplatform.library"
            implementationClass = "KotlinMultiplatformLibraryConventionPlugin"
        }
        register("kotlinSerialization") {
            id = "com.alexrdclement.gradle.plugin.kotlin.serialization"
            implementationClass = "KotlinSerializationConventionPlugin"
        }
        register("mavenPublish") {
            id = "com.alexrdclement.gradle.plugin.maven.publish"
            implementationClass = "MavenPublishConventionPlugin"
        }
        register("moduleUtils") {
            id = "com.alexrdclement.gradle.plugin.module.utils"
            implementationClass = "ModuleUtilsPlugin"
        }
        register("webApplication") {
            id = "com.alexrdclement.gradle.plugin.web.application"
            implementationClass = "WebApplicationConventionPlugin"
        }
    }
}
