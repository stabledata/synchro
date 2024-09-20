package com.stabledata

import HeaderEnvelopeParser
import com.stabledata.endpoint.configureRouting
import com.stabledata.endpoint.configureSchemaRouting
import com.stabledata.plugins.Validation
import com.stabledata.plugins.configureAuth
import com.stabledata.plugins.configureDocsRouting
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database

fun main() {
    val port = envInt("PORT")
    embeddedServer(Netty, port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module(withoutDatabase: Boolean = false) {
    val logger = configureLogging()

    // cors, auth etc. (below)
    staticConfig()

    // db connection (if not unit testing)
    if (!withoutDatabase) {
        Database.connect(hikari())
    }

    // schema endpoints
    configureSchemaRouting(logger)

    // static temp routes for now
    configureRouting(logger)

    // documentation
    configureDocsRouting()
}



/*
Handles non-injectable setup
 */
fun Application.staticConfig () {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true })
    }
    install(HeaderEnvelopeParser)
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
    }
    install(Validation)
    configureAuth()
}
