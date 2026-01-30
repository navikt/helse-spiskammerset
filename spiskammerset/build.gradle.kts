val tbdLibsVersion: String by project
val kotliqueryVersion = "1.9.0"
val logbackClassicVersion = "1.5.25"
val logbackEncoderVersion = "8.0"
val ktorVersion = "3.2.3" // bør være samme som i <com.github.navikt.tbd-libs:naisful-app>
val flywayCoreVersion = "11.5.0"
val hikariCPVersion = "6.3.0"
val postgresqlVersion = "42.7.7"

dependencies {
    implementation(project(":oppbevaringsboks"))
    implementation(project(":forsikring"))

    api("ch.qos.logback:logback-classic:$logbackClassicVersion")
    api("net.logstash.logback:logstash-logback-encoder:$logbackEncoderVersion")

    api("io.ktor:ktor-server-auth:$ktorVersion")
    api("io.ktor:ktor-server-auth-jwt:$ktorVersion") {
        exclude(group = "junit")
    }

    api("com.github.navikt.tbd-libs:naisful-app:${tbdLibsVersion}")
    api("org.flywaydb:flyway-database-postgresql:${flywayCoreVersion}")
    implementation("com.zaxxer:HikariCP:${hikariCPVersion}")
    implementation("org.postgresql:postgresql:${postgresqlVersion}")
    implementation("com.github.navikt.tbd-libs:sql-dsl:${tbdLibsVersion}")
    implementation("com.github.seratch:kotliquery:${kotliqueryVersion}")

    testImplementation("com.github.navikt.tbd-libs:postgres-testdatabaser:${tbdLibsVersion}")
    testImplementation("com.github.navikt.tbd-libs:naisful-test-app:${tbdLibsVersion}")
    testImplementation("com.github.navikt.tbd-libs:signed-jwt-issuer-test:${tbdLibsVersion}")
}
