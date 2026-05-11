plugins {
    alias(libs.plugins.kotlin.jvm)
}

allprojects {
    group = "no.nav.helse.spiskammerset"

    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        testImplementation(platform("org.junit:junit-bom:6.0.3"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation(kotlin("test"))
    }
}

subprojects {
    kotlin {
        jvmToolchain(21)
    }
    tasks {
        named<Test>("test") {
            useJUnitPlatform()
            testLogging {
                events("skipped", "failed")
                showStackTraces = true
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
    }
}
