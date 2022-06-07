plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-gradle-plugin")
    id("com.gradleup.gr8.external").version("0.5") // only for removeGradleApiFromApi
    id("org.jetbrains.kotlin.plugin.sam.with.receiver")
    id("net.mbonnin.sjmp")
}

repositories {
    mavenCentral()
}

dependencies {
    // keep in sync with MIN_GRADLE_VERSION
    compileOnly("dev.gradleplugins:gradle-api:7.1")
}

gr8 {
    removeGradleApiFromApi()
}

group = "net.mbonnin.sjmp"
version = "0.1"

gradlePlugin {
    plugins {
        create("net.mbonnin.sjmp") {
            this.id = "net.mbonnin.sjmp"
            this.implementationClass = "net.mbonnin.sjmp.SjmpPlugin"
            this.description = "Simple Jvm Maven Publish"
            this.displayName = "Sjmp"
        }
    }
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

sjmp {
    configureAllPublications {
        pomGithubRepository = "martinbonnin/sjmp"
        pomLicense = "MIT License"
        pomGithubLicensePath = "blob/main/LICENSE"
    }
}

tasks.withType(JavaCompile::class.java).configureEach {
    options.release.set(8)
}
