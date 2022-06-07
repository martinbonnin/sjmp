package net.mbonnin.sjmp

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.gradle.util.GradleVersion

class SjmpPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(GradleVersion.current() >= GradleVersion.version(MIN_GRADLE_VERSION)) {
            "apollo-android requires Gradle version $MIN_GRADLE_VERSION or greater"
        }

        target.apply(mapOf("plugin" to "maven-publish"))
        target.apply(mapOf("plugin" to "signing"))

        val javaPluginExtension = target.extensions.findByType(JavaPluginExtension::class.java)
            ?: error("No 'java' extension found. Did you apply the Java or Kotlin plugin?")

        javaPluginExtension.withSourcesJar()
        target.withEmptyJavadocJar()

        target.extensions.create("sjmp", SjmpExtension::class.java, target)
    }

    companion object {
        const val MIN_GRADLE_VERSION = "7.1"
    }
}

private fun Project.withEmptyJavadocJar() = tasks.register("emptyJavadocJar", Jar::class.java) {
    archiveClassifier.set("javadoc")
}

interface DefaultPublicationConfig {
    /**
     * The name of the repo like "$user/name"
     *
     * Mandatory
     */
    var pomGithubRepository: String?

    /**
     * The path to the GitHub license path like "blob/main/LICENSE"
     *
     * Mandatory
     */
    var pomGithubLicensePath: String?

    /**
     * The name of the license like "MIT License"
     *
     * Mandatory
     */
    var pomLicense: String?

    /**
     * project.name if null
     */
    var artifactId: String?

    /**
     * project.group if null
     */
    var groupId: String?

    /**
     * project.group if null
     */
    var version: String?

    /**
     * artifactId if null
     */
    var pomName: String?

    /**
     * artifactId if null
     */
    var pomDescription: String?

    /**
     * "pomName authors" if null
     */
    var pomDevelopers: String?

    /**
     * Configures the pom manually, overrides all the properties that start with "pom"
     */
    fun pom(action: Action<MavenPom>)
}

private class DefaultPublicationConfigImpl : DefaultPublicationConfig {
    override var pomGithubRepository: String? = null
    override var pomGithubLicensePath: String? = null
    override var pomLicense: String? = null
    override var artifactId: String? = null
    override var groupId: String? = null
    override var version: String? = null
    override var pomName: String? = null
    override var pomDescription: String? = null
    override var pomDevelopers: String? = null

    var pomAction: Action<MavenPom>? = null

    override fun pom(action: Action<MavenPom>) {
        pomAction = action
    }
}

open class SjmpExtension(val project: Project) {
    private fun configurePublication(mavenPublication: MavenPublication, config: DefaultPublicationConfigImpl) {
        with(mavenPublication) {
            val lArtifactId = config.artifactId ?: project.name
            val lGroupId = config.groupId ?: project.group.toString().takeIf { it.isNotBlank() }
            ?: error("group is required")
            val lVersion = config.version ?: project.version.toString()
            val lPomName = config.pomName ?: lArtifactId
            val lPomDescription = config.pomDescription ?: lArtifactId
            val lPomAuthors = config.pomDevelopers ?: "$lPomName authors"
            val lGithubRepository = config.pomGithubRepository ?: error("githubRepository is required")
            val lGithubLicensePath = config.pomGithubLicensePath ?: error("githubLicensePath is required")

            artifactId = lArtifactId
            groupId = lGroupId
            version = lVersion

            if (config.pomAction != null) {
                pom(config.pomAction!!)
            } else {
                pom {
                    name.set(lPomName)
                    description.set(lPomDescription)

                    val githubUrl = "https://github.com/$lGithubRepository"

                    url.set(githubUrl)

                    scm {
                        url.set(githubUrl)
                        connection.set(githubUrl)
                        developerConnection.set(githubUrl)
                    }

                    licenses {
                        license {
                            name.set(config.pomLicense)
                            url.set("$githubUrl/$lGithubLicensePath")
                        }
                    }

                    developers {
                        developer {
                            id.set(lPomAuthors)
                            name.set(lPomAuthors)
                        }
                    }
                }
            }
        }
    }

    /**
     * @param sonatypeSubDomain The sonatype subdomain to use. Use "s01" for the new host or use null for the default
     */
    fun configureRepositories(sonatypeSubDomain: String? = null) {
        project.publishing.apply {
            repositories {
                val extra = sonatypeSubDomain?.let { "$it." } ?: ""

                maven {
                    name = "ossSnapshots"
                    url = project.uri("https://${extra}oss.sonatype.org/content/repositories/snapshots/")
                    credentials {
                        username = System.getenv("OSSRH_USER")
                        password = System.getenv("OSSRH_PASSWORD")
                    }
                }
                maven {
                    name = "ossStaging"
                    url = project.uri("https://${extra}oss.sonatype.org/service/local/staging/deploy/maven2/")
                    credentials {
                        username = System.getenv("OSSRH_USER")
                        password = System.getenv("OSSRH_PASSWORD")
                    }
                }
            }
        }
    }

    fun configureSigning() {
        project.extensions.getByType(SigningExtension::class.java).apply {
            useInMemoryPgpKeys(System.getenv("GPG_PRIVATE_KEY"), System.getenv("GPG_PRIVATE_KEY_PASSWORD"))
            sign(project.publishing.publications)
        }

        project.tasks.withType(Sign::class.java).configureEach {
            isEnabled = !System.getenv("GPG_PRIVATE_KEY").isNullOrBlank()
        }
    }

    fun configureSjmpTask(snapshotsBranch: String? = null) {
        project.sjmpTask().configure {
            if (shouldPublishSnapshots(snapshotsBranch ?: "main")) {
                dependsOn(project.tasks.named("publishAllPublicationsToOssSnapshotsRepository"))
            }
            if (isTag()) {
                dependsOn(project.tasks.named("publishAllPublicationsToOssStagingRepository"))
            }
        }
    }

    private val Project.publishing: PublishingExtension
        get() = extensions.getByType(PublishingExtension::class.java)

    private fun Project.sjmpTask(): TaskProvider<out Task> {
        val rootTask = try {
            rootProject.tasks.named("sjmpPublishIfNeeded")
        } catch (e: Exception) {
            null
        }

        if (rootTask != null) {
            return rootTask
        }

        return rootProject.tasks.register("sjmpPublishIfNeeded", DefaultTask::class.java)
    }

    private fun shouldPublishSnapshots(snapshotsBranch: String): Boolean {
        val eventName = System.getenv("GITHUB_EVENT_NAME")
        val ref = System.getenv("GITHUB_REF")

        return eventName == "push" && (ref == "refs/heads/$snapshotsBranch")
    }

    private fun isTag(): Boolean {
        val ref = System.getenv("GITHUB_REF")

        return ref?.startsWith("refs/tags/") == true
    }

    fun createPublication(action: Action<DefaultPublicationConfig>) {
        val config = DefaultPublicationConfigImpl()
        action.execute(config)

        project.publishing.apply {
            publications {
                create("default", MavenPublication::class.java) {
                    from(project.components.findByName("java"))

                    artifact(project.tasks.named("emptyJavadocJar"))
                    artifact(project.tasks.named("sourcesJar"))

                    configurePublication(this, config)
                }
            }
        }
    }

    fun configurePublication(name: String, action: Action<DefaultPublicationConfig>) {
        val config = DefaultPublicationConfigImpl()
        action.execute(config)

        project.publishing.apply {
            publications.getByName(name) {
                this as MavenPublication
                configurePublication(this, config)
            }
        }
    }

    fun configureAllPublications(action: Action<DefaultPublicationConfig>) {
        val config = DefaultPublicationConfigImpl()
        action.execute(config)

        project.publishing.apply {
            publications.configureEach {
                this as MavenPublication
                configurePublication(this, config)
            }
        }
    }
}

