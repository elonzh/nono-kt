pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri(providers.gradleProperty("localMavenRepository").get())
        }
        mavenCentral()
    }
}

rootProject.name = "nono-kt-publication-test"
