plugins {
    id("application")
}

application {
    mainClass.set("no.nav.helse.spiskammerset.opprydding_dev.AppKt")
    applicationName = "app"
}

dependencies {
    implementation(libs.rapids.and.rivers)
    implementation(libs.hikaricp)
    implementation(libs.postgresql)
    implementation(libs.kotliquery)

    testImplementation(project(":migreringer"))
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.tbd.libs.rapids.and.rivers.test)
    testImplementation(libs.flyway.database.postgresql)
}
