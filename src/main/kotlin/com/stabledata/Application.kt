package com.stabledata

import com.stabledata.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    configureLogging()
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
    }
    configureAuth()
    configureRouting()

}
