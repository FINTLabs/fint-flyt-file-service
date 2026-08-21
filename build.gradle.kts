plugins {
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.github.ben-manes.versions") version "0.61.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.spring") version "2.4.10"
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

extra["jackson-bom.version"] = "2.22.2"
extra["log4j2.version"] = "2.26.1"
extra["netty.version"] = "4.2.17.Final"

dependencies {
    constraints {
        implementation("at.yawk.lz4:lz4-java:1.11.2") {
            because("Fixes CVE-2026-59949 in the kafka-clients transitive dependency")
        }
    }

    implementation(platform("tools.jackson:jackson-bom:3.2.2"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.security:spring-security-config")
    compileOnly("org.springframework.security:spring-security-web")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.4")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.google.guava:guava:33.7.1-jre")
    implementation("com.azure:azure-storage-blob:12.35.0")

    implementation("no.novari:flyt-cache:3.0.0")
    implementation("no.novari:flyt-web-resource-server:4.0.0")
    implementation("no.novari:flyt-kafka:7.2.0")

    implementation("org.apache.commons:commons-text:1.15.0")
    implementation("org.apache.commons:commons-lang3:3.20.0")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:9.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
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
