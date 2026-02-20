plugins {
    id("org.springframework.boot") version "3.5.10"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.github.ben-manes.versions") version "0.53.0"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.spring") version "2.3.10"
}

group = "no.novari"
version = "0.0.1-SNAPSHOT"

kotlin {
    jvmToolchain(25)
}

tasks.jar {
    isEnabled = false
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.fintlabs.no/releases")
    }
    mavenLocal()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.security:spring-security-config")
    compileOnly("org.springframework.security:spring-security-web")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.google.guava:guava:33.5.0-jre")
    implementation("com.azure:azure-storage-blob:12.33.2")

    implementation("no.novari:flyt-web-resource-server:2.0.0")
    implementation("no.novari:flyt-kafka:4.0.0")
    implementation("no.novari:flyt-cache:2.0.0")

    implementation("org.apache.commons:commons-text:1.15.0")
    implementation("org.apache.commons:commons-lang3:3.20.0")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:7.4")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.2.3")
}

tasks.test {
    useJUnitPlatform()
}

ktlint {
    version.set("1.8.0")
}

tasks.named("check") {
    dependsOn("ktlintCheck")
}
