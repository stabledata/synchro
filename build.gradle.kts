import com.google.gradle.osdetector.OsDetector

val kotlinVersion: String by project
val logbackVersion: String by project
val ktorVersion: String by project

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    id("io.ktor.plugin") version "2.3.12"
    id("org.openapi.generator") version "7.8.0"
    id("com.google.protobuf") version "0.9.4"
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
        java.srcDirs("src/main/kotlin/chores")
        proto {
            srcDir("src/main/resources/proto")  // Specify the proto files location
        }
    }
}

protobuf {
    protoc {
        artifact = if (project.extensions.getByType(OsDetector::class).os == "osx") {
            "com.google.protobuf:protoc:3.14.0:osx-x86_64"
        } else {
            "com.google.protobuf:protoc:3.14.0"
        }
    }
    plugins {
        create("grpc"){
            artifact = "io.grpc:protoc-gen-grpc-java:1.52.1"
        }
//        create("grpckt") {
//            artifact = "com.google.protobuf:protoc-gen-kotlin:3.17.3"
//        }
    }
    generateProtoTasks {
        all().forEach { task ->

            task.plugins {
                create("grpc")
                // create("grpckt")

            }
//            task.builtins {
//                create("kotlin")
//            }
        }
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
    generatorName.set("typescript")
    templateDir.set("$rootDir/src/main/resources/openapi/templates")
    additionalProperties.putAll(
        mapOf(
            "npmPackageName" to "@stabledata/synchro-client",
            "npmPackageVersion" to version,
            "useObjectParameters" to "true"
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
    implementation("io.ktor:ktor-server-openapi:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:2.0.0")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // serialization  + json + validation
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("io.github.optimumcode:json-schema-validator:0.2.3")
    implementation("io.ktor:ktor-serialization-gson:2.3.1")
    implementation("com.google.code.gson:gson:2.9.0")


    // grpc
    implementation("io.grpc:grpc-netty-shaded:1.48.1")
    // FIXME: this is still vulnerable!
    // https://github.com/protocolbuffers/protobuf/security/advisories/GHSA-735f-pc8j-v9w8
    implementation("io.grpc:grpc-protobuf:1.68.0")
    implementation("io.grpc:grpc-stub:1.68.0")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("io.grpc:grpc-services:1.42.1")

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

    // ably
    implementation("io.ably:ably-java:1.2.40")

    // log
    implementation("ch.qos.logback:logback-classic:1.5.7")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.4")

    // tests
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.0")
    testImplementation("io.kotest:kotest-framework-engine:5.9.0")
    testImplementation("io.github.serpro69:kotlin-faker:1.16.0")
    testImplementation("io.mockk:mockk:1.13.4")
    testImplementation("io.grpc:grpc-testing:1.68.0")
}
