# This project is deprecated, use https://github.com/GradleUp/librarian instead

# SJMP (Simple Java Maven Publish)

A small opinionated Gradle plugin to automate publishing of a simple JVM only project.

This plugin assumes using GitHub and Sonatype OSSRH.

## Minimal Configuration

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm").version(kotlinVersion)
    id("net.mbonnin.sjmp").version("0.1")
}

sjmp {
    jvmProject {
        publication {
            groupId = "net.mbonnin.xoxo"
            version = "0.2-SNAPSHOT"
            simplePom {
                githubRepository = "martinbonnin/xoxo"
                githubLicensePath = "LICENSE"
                license = "MIT License"
            }
        }
    }
}

```

## Environment variables

```
OSSRH_USER
OSSRH_PASSWORD

# Signing will be skipped if these environment variables are not set
# This is ok for snapshots and maven local but not maven central
GPG_PRIVATE_KEY
GPG_PRIVATE_KEY_PASSWORD
```
