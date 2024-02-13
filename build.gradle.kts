plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "org.christos"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // API
    implementation("io.vertx:vertx-lang-kotlin:4.5.3")
    implementation("io.vertx:vertx-core:4.5.3")
    implementation("io.vertx:vertx-web:4.5.3")
    implementation("io.vertx:vertx-config:4.5.3")
    implementation("io.vertx:vertx-auth-jwt:4.5.3")

    // Serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")

    // Tests
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation(kotlin("test"))

    // Env variables
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}
