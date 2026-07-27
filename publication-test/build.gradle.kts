plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

dependencies {
    implementation(
        "com.elonzh.nono:nono-kt:${providers.gradleProperty("nonoVersion").get()}",
    )
}
