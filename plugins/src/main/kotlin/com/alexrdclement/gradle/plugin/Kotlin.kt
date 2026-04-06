package com.alexrdclement.gradle.plugin

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = AndroidCompileSdk

        defaultConfig {
            minSdk = AndroidMinSdk

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    configureKotlin<KotlinAndroidProjectExtension>()
}

internal fun Project.configureKotlinMultiplatformAndroidLibrary(
    androidLibraryExtension: KotlinMultiplatformAndroidLibraryExtension,
    configuration: AndroidLibraryTargetConfiguration,
) {
    androidLibraryExtension.apply {
        compileSdk = AndroidCompileSdk
        minSdk = AndroidMinSdk

        namespace = configuration.namespace
        configuration.hostTestConfiguration?.let(::withHostTest)
        configuration.instrumentedTestConfiguration?.let {
            withDeviceTest {
                instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                execution = "ANDROIDX_TEST_ORCHESTRATOR"
                it()
            }
        }
    }
}

internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    configureKotlin<KotlinJvmProjectExtension>()
}

private inline fun <reified T : KotlinBaseExtension> Project.configureKotlin() = configure<T> {
    // Treat all Kotlin warnings as errors (disabled by default)
    // Override by setting warningsAsErrors=true in your ~/.gradle/gradle.properties
    val warningsAsErrors: String? by project
    when (this) {
        is KotlinAndroidProjectExtension -> compilerOptions
        is KotlinJvmProjectExtension -> compilerOptions
        else -> throw IllegalArgumentException("Unsupported project extension $this ${T::class}")
    }.apply {
        jvmTarget = JvmTarget.JVM_17
        allWarningsAsErrors = warningsAsErrors.toBoolean()
        // Enable detailed names in test-parameter-injector tests
        javaParameters = true
        freeCompilerArgs.add("-Xcontext-parameters")
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

fun Project.configureKotlin(enableAllWarningsAsErrors: Boolean = false) {
    // Treat all Kotlin warnings as errors (disabled by default)
    // Override by setting warningsAsErrors=true in your ~/.gradle/gradle.properties
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            // Blocked by https://youtrack.jetbrains.com/issue/KT-69701/
            if (enableAllWarningsAsErrors) {
                allWarningsAsErrors = true
            }

            freeCompilerArgs.add("-Xcontext-parameters")

            if (this is KotlinJvmCompilerOptions) {
                // Target JVM 11 bytecode
                jvmTarget = JvmTarget.JVM_17
                // Enable detailed names in test-parameter-injector tests
                javaParameters = true
            }
        }
    }
}
