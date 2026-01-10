import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.shipkit.changelog.GenerateChangelogTask
import org.shipkit.github.release.GithubReleaseTask

open class GithubReleaseExtension {
    var githubToken: String? = null
    var repository: String? = null
    var newTagRevision: String? = null
    var enabled: Boolean = true
}

/**
 * Configures automatic versioning and GitHub releases using shipkit plugins.
 *
 * The plugin applies and configures:
 * - `org.shipkit.shipkit-auto-version` for automatic semantic versioning from git tags
 * - `org.shipkit.shipkit-changelog` for generating changelogs from commits
 * - `org.shipkit.shipkit-github-release` for creating GitHub releases
 *
 * ```kotlin
 * plugins {
 *     id("com.alexrdclement.gradle.plugin.github.release")
 * }
 *
 * githubRelease {
 *     githubToken = System.getenv("GITHUB_TOKEN")
 *     repository = "username/repo"
 *     enabled = !version.toString().endsWith("SNAPSHOT")
 *     newTagRevision = System.getenv("GITHUB_SHA")
 * }
 * ```
 *
 * The plugin also registers a custom task `updateReadmeVersion` that updates `{{VERSION}}`
 * placeholders in `README.md` with the current project version.
 *
 * ```bash
 * ./gradlew updateReadmeVersion
 * ```
 */
class GithubReleaseConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("org.shipkit.shipkit-auto-version")
        project.pluginManager.apply("org.shipkit.shipkit-changelog")
        project.pluginManager.apply("org.shipkit.shipkit-github-release")

        val extension = project.extensions.create("githubRelease", GithubReleaseExtension::class.java)

        project.tasks.register("updateReadmeVersion", UpdateReadmeVersionTask::class.java)

        // Configure tasks after evaluation when extension values are available
        project.afterEvaluate {
            val previousRevision = project.extraProperties["shipkit-auto-version.previous-tag"] as String

            // Configure generateChangelog task using typed API
            val changelogTask = project.tasks.named("generateChangelog", GenerateChangelogTask::class.java).get()
            changelogTask.previousRevision = previousRevision
            extension.githubToken?.let { token -> changelogTask.githubToken = token }
            extension.repository?.let { repo -> changelogTask.repository = repo }

            // Configure githubRelease task using typed API
            val githubReleaseTask = project.tasks.named("githubRelease", GithubReleaseTask::class.java).get()
            githubReleaseTask.dependsOn(changelogTask)
            githubReleaseTask.enabled = extension.enabled
            extension.repository?.let { repo -> githubReleaseTask.repository = repo }
            // Wire changelog output from generateChangelog to githubRelease
            // Use setProperty for changelog as it may not have a typed accessor
            githubReleaseTask.setProperty("changelog", changelogTask.outputFile)
            extension.githubToken?.let { token -> githubReleaseTask.githubToken = token }
            extension.newTagRevision?.let { revision -> githubReleaseTask.newTagRevision = revision }

            val updateReadmeTask = project.tasks.named("updateReadmeVersion", UpdateReadmeVersionTask::class.java).get()
            updateReadmeTask.group = "documentation"
            updateReadmeTask.description = "Updates `{{VERSION}}` placeholders in README.md with the current project version"
            updateReadmeTask.projectVersion.set(project.version.toString())
            updateReadmeTask.readmeFile.set(project.layout.projectDirectory.file("README.md"))
        }
    }
}

/**
 * Updates `{{VERSION}}` placeholders in README.md with the current project version.
 *
 * @property projectVersion The current project version to insert into the README
 * @property readmeFile The README.md file to update
 */
abstract class UpdateReadmeVersionTask : DefaultTask() {
    @get:Input
    abstract val projectVersion: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val readmeFile: RegularFileProperty

    @TaskAction
    fun updateVersion() {
        val file = readmeFile.get().asFile
        val currentVersion = projectVersion.get()
        val content = file.readText()
        val updatedContent = content.replace("{{VERSION}}", currentVersion)
        file.writeText(updatedContent)
        println("Updated README.md with version $currentVersion")
    }
}
