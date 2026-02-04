val tbdLibsVersion: String by project
val hikariCPVersion = "6.3.0"

dependencies {
    implementation(project(":oppbevaringsboks"))

    testImplementation("com.github.navikt.tbd-libs:postgres-testdatabaser:${tbdLibsVersion}")
    testImplementation("com.zaxxer:HikariCP:${hikariCPVersion}")
    testImplementation(project(":spiskammerset")) // TODO burde putte migreringene et annet sted så vi slipper å dra inn hele spiskammerset for det her
}
