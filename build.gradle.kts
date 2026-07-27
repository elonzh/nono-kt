import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.maven.publish)
}

group = "com.elonzh.nono"
version = providers.gradleProperty("VERSION_NAME").orElse("0.1.0-SNAPSHOT").get()

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test.junit5)
}

tasks.test {
    useJUnitPlatform()
}

configure<MavenPublishBaseExtension> {
    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("nono-kt")
        description.set("Kotlin and Java client for running commands through the nono CLI.")
        inceptionYear.set("2026")
        url.set("https://github.com/elonzh/nono-kt")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("elonzh")
                name.set("elonzh")
                email.set("elonzh@users.noreply.github.com")
                url.set("https://github.com/elonzh")
            }
        }

        scm {
            url.set("https://github.com/elonzh/nono-kt")
            connection.set("scm:git:https://github.com/elonzh/nono-kt.git")
            developerConnection.set("scm:git:ssh://git@github.com/elonzh/nono-kt.git")
        }
    }
}
