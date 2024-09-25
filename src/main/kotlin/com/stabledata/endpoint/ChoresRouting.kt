package com.stabledata.endpoint

import com.stabledata.hikari
import com.stabledata.plugins.JWT_NAME
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException

import org.slf4j.Logger

fun Application.configureChoresRouting(logger: Logger) {
    routing {

        get("/") {
            logger.info("Healthcheck / endpoint called.")
            call.respondText("ok")
        }
        authenticate(JWT_NAME) {
            get("/secure") {
                call.respondText("secured")
            }

            get("migrate") {
                logger.info("Attempting db migration")
                try {

                    Flyway.configure()
                        .dataSource(hikari())
                        .load()
                        .migrate()

                    return@get call.respond(HttpStatusCode.OK, "Migration success")
                } catch (fe: FlywayException) {
                    return@get call.respond(HttpStatusCode.InternalServerError, "Migration failed")
                }
            }
        }
    }
}
