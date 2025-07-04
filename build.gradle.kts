plugins {
    kotlin("jvm") version "2.1.20"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.cflowers"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.web3j:core:4.9.5")
    implementation("io.ktor:ktor-client-core:2.3.3")
    implementation("io.ktor:ktor-client-cio:2.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
}

application {
    mainClass.set("SpreadSniperkt") // Match your entrypoint filename
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    // Creates an uber/fat jar for easy deployment
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName.set("spreadsniper")
        archiveClassifier.set("")
        archiveVersion.set("")
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }
}
kotlin {
    jvmToolchain(23)
}