plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "com.shioya86"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jsoup:jsoup:1.16.1")
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

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    configurations["runtimeClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
}