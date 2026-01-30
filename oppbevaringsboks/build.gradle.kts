val jacksonVersion: String by project
val tbdLibsVersion: String by project

dependencies {
    api("com.fasterxml.jackson.module:jackson-module-kotlin:${jacksonVersion}")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")
    api("com.github.navikt.tbd-libs:sql-dsl:${tbdLibsVersion}")
}
