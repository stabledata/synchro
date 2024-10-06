package com.stabledata

import com.stabledata.endpoint.configureChoresRouting
import com.stabledata.endpoint.configureApplicationRouting
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

    embeddedServer(
        Netty,
        port,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {

    // cors, auth etc. (below)
    configurePlugins()

    // db connection (if not unit testing)
    Database.connect(hikari())


    // configure routes.
    configureApplicationRouting()
    configureChoresRouting()
    configureDocsRouting()
}



/*
Handles non-injectable setup
 */
fun Application.configurePlugins () {
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
