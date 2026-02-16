val rapidsAndRiversVersion: String by project
val tbdLibsVersion: String by project
val hikariCPVersion: String by project
val postgresqlVersion: String by project

dependencies {
    api("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")
    api("com.github.navikt.tbd-libs:azure-token-client-default:$tbdLibsVersion")

    implementation("org.postgresql:postgresql:${postgresqlVersion}")
    implementation("com.zaxxer:HikariCP:${hikariCPVersion}")
    implementation("com.github.navikt.tbd-libs:sql-dsl:${tbdLibsVersion}")

    testImplementation("com.github.navikt.tbd-libs:postgres-testdatabaser:${tbdLibsVersion}")

    testImplementation("com.github.navikt.tbd-libs:rapids-and-rivers-test:$tbdLibsVersion")
    testImplementation("com.github.navikt.tbd-libs:mock-http-client:$tbdLibsVersion")
    testImplementation(project(":spiskammerset")) // TODO burde putte migreringene et annet sted så vi slipper å dra inn hele spiskammerset for det her

}

tasks.named("test") {
    val spiskammersetCopyDeps = project(":spiskammerset").tasks.findByName("copyDeps")
    if (spiskammersetCopyDeps != null) {
        mustRunAfter(spiskammersetCopyDeps)
    }
}
