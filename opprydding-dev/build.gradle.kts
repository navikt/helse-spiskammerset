plugins {
    alias(libs.plugins.kotlin.jvm)
    id("application")
}

application {
    mainClass.set("no.nav.helse.spiskammerset.opprydding_dev.AppKt")
    applicationName = "app"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(libs.rapids.and.rivers)
    api(libs.tbd.libs.azure.token.client.default)

    implementation(libs.cloud.sql.postgres.socket.factory)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.tbd.libs.sql.dsl)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.tbd.libs.postgres.testdatabaser)
    testImplementation(libs.tbd.libs.rapids.and.rivers.test)
    testImplementation(libs.tbd.libs.mock.http.client)
    testImplementation(libs.flyway.database.postgresql)
    testImplementation(project(":migreringer"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("skipped", "failed")
    }
}
