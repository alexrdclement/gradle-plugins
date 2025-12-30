import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.shipkit.changelog.GenerateChangelogTask
import org.shipkit.github.release.GithubReleaseTask

open class GithubReleaseExtension {
    var githubToken: String? = null
    var repository: String? = null
    var newTagRevision: String? = null
    var enabled: Boolean = true
}

class GithubReleaseConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("org.shipkit.shipkit-auto-version")
        project.pluginManager.apply("org.shipkit.shipkit-changelog")
        project.pluginManager.apply("org.shipkit.shipkit-github-release")

        val extension = project.extensions.create("githubRelease", GithubReleaseExtension::class.java)

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
        }
    }
}
