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

    // Tests
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
