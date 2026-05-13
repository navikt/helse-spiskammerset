plugins {
    alias(libs.plugins.kotlin.jvm)
    id("application")
}

application {
    mainClass.set("no.nav.helse.spiskammerset.spiskammerset.AppKt")
    applicationName = "app"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":migreringer"))
    api(libs.rapids.and.rivers)
    api(libs.tbd.libs.azure.token.client.default)
    api(libs.logback.classic)
    api(libs.logstash.logback.encoder)

    api(libs.ktor.server.auth)
    api(libs.ktor.server.auth.jwt) {
        exclude(group = "junit")
    }

    api(libs.tbd.libs.naisful.app)
    api(libs.flyway.database.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.postgresql)
    implementation(libs.kotliquery)
    implementation(libs.tbd.libs.sql.dsl)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.tbd.libs.rapids.and.rivers.test)
    testImplementation(libs.tbd.libs.mock.http.client)
    testImplementation(libs.jsonassert)
    testImplementation(libs.tbd.libs.postgres.testdatabaser)
    testImplementation(libs.tbd.libs.naisful.test.app)
    testImplementation(libs.tbd.libs.signed.jwt.issuer.test)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("skipped", "failed")
    }
}
