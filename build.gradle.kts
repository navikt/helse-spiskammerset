import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

val junitJupiterVersion = "5.12.1"
val tbdLibsVersion = "2026.01.22-09.16-1d3f6039"
val rapidsAndRiversVersion = "2026011411051768385145.e8ebad1177b4"
val jacksonVersion = "2.18.3"
val hikariCPVersion = "6.3.0"
val jsonAssertVersion = "1.5.3"
val kotliqueryVersion = "1.9.0"
val postgresqlVersion = "42.7.7"

plugins {
    kotlin("jvm") version "2.3.0" apply false
}

allprojects {
    repositories {
        val githubPassword: String? by project
        mavenCentral()
        /* ihht. https://github.com/navikt/utvikling/blob/main/docs/teknisk/Konsumere%20biblioteker%20fra%20Github%20Package%20Registry.md
            så plasseres github-maven-repo (med autentisering) før nav-mirror slik at github actions kan anvende førstnevnte.
            Det er fordi nav-mirroret kjører i Google Cloud og da ville man ellers fått unødvendige utgifter til datatrafikk mellom Google Cloud og GitHub
         */
        maven {
            url = uri("https://maven.pkg.github.com/navikt/maven-release")
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
        }
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    ext.set("tbdLibsVersion", tbdLibsVersion)
    ext.set("rapidsAndRiversVersion", rapidsAndRiversVersion)
    ext.set("jacksonVersion", jacksonVersion)
    ext.set("jsonAssertVersion", jsonAssertVersion)
    ext.set("hikariCPVersion", hikariCPVersion)
    ext.set("postgresqlVersion", postgresqlVersion)
    ext.set("kotliqueryVersion", kotliqueryVersion)

    val testImplementation by configurations
    val testRuntimeOnly by configurations
    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    configure<KotlinJvmProjectExtension> {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of("21"))
        }
    }

    tasks {
        if (project.name in setOf("spiskammerset", "mottak", "opprydding-dev")) {
            withType<Jar> {
                archiveBaseName.set("app")

                doFirst {
                    manifest {
                        val runtimeClasspath by configurations
                        attributes["Main-Class"] = "no.nav.helse.spiskammerset.${project.name.replace("-", "_")}.AppKt"
                        attributes["Class-Path"] = runtimeClasspath.joinToString(separator = " ") {
                            it.name
                        }
                    }
                }
            }

            val copyDeps by registering(Sync::class) {
                val runtimeClasspath by configurations
                from(runtimeClasspath)
                into("build/libs")
            }
            named("assemble") {
                dependsOn(copyDeps)
            }
        }

        withType<Test> {
            useJUnitPlatform()
            testLogging {
                events("skipped", "failed")
            }
        }
    }
}
