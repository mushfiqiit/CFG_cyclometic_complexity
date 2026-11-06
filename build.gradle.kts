plugins {
    application
    // Selects the JavaFX artifacts for the OS and CPU running the build.
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

javafx {
    // JavaFX 19 crashes on macOS 26 when a window opens; 21.0.5 does not.
    version = "21.0.5"
    modules("javafx.controls")
}

application {
    mainClass = "cfg.Main"
}

tasks.named<JavaExec>("run") {
    // The program reads its menu choice from the keyboard.
    standardInput = System.`in`
}
