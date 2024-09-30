
val kotlin_version: String by project
val logback_version: String by project
val ktor_version: String by project

plugins {
    kotlin("jvm") version "2.0.20"
    id("io.ktor.plugin") version "2.3.12"
    kotlin("plugin.serialization") version "2.0.20"
    id("org.openapi.generator") version "7.8.0"
}

group = "com.stabledata"
version = "0.0.1"

application {
    mainClass.set("com.stabledata.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        java.srcDirs("src/main/kotlin/cli")
    }
}

tasks.register<JavaExec>("migrate") {
    group = "Execution"
    description = "Run Flyway commands from gradle"

    classpath = sourceSets.getByName("main").runtimeClasspath
    mainClass.set("com.stabledata.chores.Migrations")

    val task = if (project.hasProperty("task")) {
        project.property("task") as String
    } else {
        "migrate" // Default to help if no task is provided
    }

    // Pass this to the application as an argument
    args = listOf(task)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

openApiValidate {
    inputSpec.set("$rootDir/src/main/resources/openapi/doc.yaml")
}

openApiGenerate {
    inputSpec.set("$rootDir/src/main/resources/openapi/doc.yaml")
    outputDir.set("$rootDir/client")
    generatorName.set("typescript-fetch")
    templateDir.set("$rootDir/src/main/resources/openapi/templates")
    additionalProperties.putAll(
        mapOf(
            "npmPackageName" to "@stabledata/client",
            "npmPackageVersion" to version
        )
    )
}

tasks.register("generatePackageJson") {

    val version = project.findProperty("version")?.toString()
        ?: throw Exception("Specify a version to build client via -Pversion=123")


    doLast {
        val packageJsonFile = file("$rootDir/client/package.json")
        packageJsonFile.writeText("""
        {
            "name": "@stabledata/synchro-client",
            "version": "$version",
            "description": "This client is generated with the open api typescript-fetch template during synchro builds",
            "main": "index.ts",
            "scripts": {
                "build": "tsc"
            },
            "dependencies": {
                "whatwg-fetch": "^3.0.0"
            },
            "devDependencies": {
                "typescript": "^4.0.0"
            }
        }
        """.trimIndent())
    }
}

tasks.named("openApiGenerate").configure {
    finalizedBy("generatePackageJson")
}

dependencies {
    // ktor core
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")

    implementation("io.ktor:ktor-server-content-negotiation:2.2.4")
    implementation("io.ktor:ktor-server-openapi:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")

    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")

    // serialization  + json + validation
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("io.github.optimumcode:json-schema-validator:0.2.3")
    implementation("io.ktor:ktor-serialization-gson:2.3.1")
    implementation("com.google.code.gson:gson:2.9.0")


    // db
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.jetbrains.exposed:exposed-core:0.40.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.40.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.40.1")

    // db chores
    implementation("org.flywaydb:flyway-core:10.4.1")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:10.17.3")

    // misc
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation("com.fasterxml.uuid:java-uuid-generator:5.1.0")

    // log
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // tests
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.0")
    testImplementation("io.kotest:kotest-framework-engine:5.9.0")
    testImplementation("io.github.serpro69:kotlin-faker:1.16.0")
    testImplementation("io.mockk:mockk:1.13.4")

}
