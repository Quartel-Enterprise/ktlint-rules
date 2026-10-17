import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.vanniktech.maven.publish)
}

group = "software.quare"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.ktlint.rule.engine.core)
    compileOnly(libs.ktlint.cli.ruleset.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.ktlint.rule.engine.core)
    testImplementation(libs.ktlint.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.slf4j.nop)
}

// The ktlint CLI runs the ruleset on whatever JDK the consumer has, so target the oldest one we support.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral()

    coordinates(
        groupId = group.toString(),
        artifactId = "ktlint-rules",
        version = version.toString(),
    )

    pom {
        name = "Quare ktlint rules"
        description = "Custom ktlint rule set shared by the Quare Kotlin projects."
        inceptionYear = "2026"
        url = "https://github.com/Quartel-Enterprise/ktlint-rules"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }

        developers {
            developer {
                id = "Quartel-Enterprise"
                name = "Quartel Enterprise"
                email = "quare.software@gmail.com"
            }
        }

        scm {
            connection = "scm:git:github.com/Quartel-Enterprise/ktlint-rules.git"
            developerConnection = "scm:git:ssh://github.com/Quartel-Enterprise/ktlint-rules.git"
            url = "https://github.com/Quartel-Enterprise/ktlint-rules/tree/main"
        }
    }
}
