package com.stabledata

import com.stabledata.plugins.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import kotlinx.serialization.json.Json

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    configureLogging()

    // cors, auth etc. (below)
    staticConfig()

    // injectables for testing
    val logger = getLogger()

    // schema endpoints
    configureSchemaRouting(logger)

    configureRouting(logger) // static temp routes for now
    configureDocsRouting()
}

/*
Handles non-injectable setup
 */
fun Application.staticConfig () {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true })
    }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
    }
    configureAuth()
}
