plugins {
    application
    // Selects the JavaFX artifacts for the OS and CPU running the build.
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

// Tests tagged "known-bug" assert the correct behavior for bugs that are not fixed yet, so they
// fail today. They are left out of the normal build and run on their own with knownBugTest.
val knownBugTag = "known-bug"

tasks.withType<Test>().configureEach {
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.test {
    useJUnitPlatform {
        excludeTags(knownBugTag)
    }
}

val knownBugTest by tasks.registering(Test::class) {
    description = "Runs the tests for known bugs. They fail until the bugs are fixed."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags(knownBugTag)
    }
}
