plugins {
    kotlin("jvm") version "2.1.20"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("plugin.serialization") version "1.9.0"
}

group = "org.dust2dust"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.web3j:core:4.9.5")
    implementation("io.ktor:ktor-client-core:2.3.11")
    implementation("io.ktor:ktor-client-cio:2.3.11") // For CIO engine (or choose another like Apache, OkHttp)
    implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation("com.sun.mail:jakarta.mail:2.0.1")
}

application {
    mainClass.set("SpreadSniperKt") // Match your entrypoint filename
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
        manifest {
            attributes["Main-Class"] = "SpreadSniperKt"
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
kotlin {
    jvmToolchain(17)
}