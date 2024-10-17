package com.stabledata.endpoint

import com.stabledata.envFlag
import com.stabledata.generateTokenForTesting
import com.stabledata.hikari
import com.stabledata.context.JWT_NAME
import com.stabledata.uuidString
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

        get("/token") {
            // only respond to this in test mode.
            val isTokenEndpointEnabled = envFlag("ENABLE_TOKEN_ENDPOINT")

            if (!isTokenEndpointEnabled) {
                call.respond(HttpStatusCode.Forbidden)
                return@get
            }

            logger.info { "Token endpoint called, responding with token" }

            val role = call.request.queryParameters["role"]
            val email = call.request.queryParameters["email"]
            val team = call.request.queryParameters["team"]
            val jwtToken = generateTokenForTesting(
                role,
                team,
                email,
            )
            call.respond(HttpStatusCode.OK, mapOf(
                "token" to jwtToken,
                "team" to team,
                "role" to role,
                "uuid" to uuidString()
            ))
        }

        authenticate(JWT_NAME) {

            get("/secure") {
                call.respond(HttpStatusCode.OK,"ok")
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
                    logger.error { "Flyway migration exception: $fe"  }
                    return@get call.respond(HttpStatusCode.InternalServerError, "Migration failed")
                }
            }
        }
    }
}
