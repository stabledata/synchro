package com.stabledata.endpoint

import com.stabledata.plugins.JWT_NAME
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import org.slf4j.Logger

fun Application.configureRouting(logger: Logger) {
    routing {

        get("/") {
            logger.info("Healthcheck / endpoint called.")
            call.respondText("ok")
        }
        authenticate(JWT_NAME) {
            get("/secure") {
                call.respondText("secured")
            }
        }
    }
}
