import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.maven.publish)
}

group = "com.elonzh.nono"
version = providers.gradleProperty("VERSION_NAME").orElse("0.2.0-SNAPSHOT").get()

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

val integrationTestSourceSet =
    sourceSets.create("integrationTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }

configurations[integrationTestSourceSet.implementationConfigurationName].extendsFrom(
    configurations.testImplementation.get(),
)
configurations[integrationTestSourceSet.runtimeOnlyConfigurationName].extendsFrom(
    configurations.testRuntimeOnly.get(),
)

tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Runs tests against an installed supported nono CLI."
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
    outputs.upToDateWhen { false }

    doFirst {
        require(System.getenv("NONO_INTEGRATION") == "1") {
            "Set NONO_INTEGRATION=1 to run real nono integration tests"
        }
    }
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}

tasks.check {
    dependsOn("docsCheck")
    dependsOn(":examples:kotlin:compileKotlin")
    dependsOn(":examples:java:compileJava")
}

val documentationPairs =
    listOf(
        "README.md" to "README.zh-Hans.md",
        "docs/getting-started.md" to "docs/zh-Hans/getting-started.md",
        "docs/run-api.md" to "docs/zh-Hans/run-api.md",
        "docs/profiles-and-diagnostics.md" to "docs/zh-Hans/profiles-and-diagnostics.md",
        "docs/compatibility.md" to "docs/zh-Hans/compatibility.md",
    )

tasks.register("docsCheck") {
    group = "verification"
    description = "Checks bilingual documentation, local links, and examples."
    dependsOn(":examples:kotlin:compileKotlin")
    dependsOn(":examples:java:compileJava")

    doLast {
        documentationPairs.forEach { (english, chinese) ->
            val englishFile = file(english)
            val chineseFile = file(chinese)
            require(englishFile.isFile) { "Missing English document: $english" }
            require(chineseFile.isFile) { "Missing Chinese document: $chinese" }
        }

        val markdownFiles =
            files(
                fileTree(projectDir) {
                    include("*.md")
                    include("docs/**/*.md")
                },
            )
        val linkPattern = Regex("""\[[^]]*]\(([^)]+)\)""")
        val uriScheme = Regex("""^[A-Za-z][A-Za-z0-9+.-]*:""")
        markdownFiles.forEach { source ->
            linkPattern.findAll(source.readText()).forEach { match ->
                val target = match.groupValues[1].substringBefore('#')
                if (target.isNotEmpty() && !target.startsWith("#") && !uriScheme.containsMatchIn(target)) {
                    val resolved = source.parentFile.resolve(target).normalize()
                    require(resolved.exists()) {
                        "Broken local Markdown link in ${source.relativeTo(projectDir)}: $target"
                    }
                }
            }
        }
    }
}

configure<MavenPublishBaseExtension> {
    publishToMavenCentral(automaticRelease = true)
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

gradle.taskGraph.whenReady {
    extensions.configure<SigningExtension> {
        isRequired = allTasks.any { task -> task.name.contains("MavenCentral") }
    }
}
