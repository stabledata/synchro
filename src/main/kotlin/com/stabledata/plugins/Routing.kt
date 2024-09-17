package com.stabledata.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import org.slf4j.Logger

fun Application.configureRouting(logger: Logger) {
    routing {

        get("/") {
            logger.info("Hello world endpoint called")
            call.respondText("Hello World!")
        }
        authenticate(JWT_NAME) {
            get("/secure") {
                call.respondText("secured")
            }
        }
    }
}
