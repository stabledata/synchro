package com.stabledata.endpoint

import com.stabledata.hikari
import com.stabledata.plugins.JWT_NAME
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException

fun Application.configureChoresRouting() {

    val logger = KotlinLogging.logger {}

    routing {

        get("/") {
            logger.debug { "Healthcheck / endpoint called." }
            call.respondText("ok")
        }
        authenticate(JWT_NAME) {
            get("/secure") {
                call.respondText("secured")
            }

            get("migrate") {
                logger.debug { "Attempting db migration" }
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
