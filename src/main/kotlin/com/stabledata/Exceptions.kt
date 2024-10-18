package com.stabledata

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

class UnauthorizedException(message: String) : Exception(message)
class JSONSchemaValidationException(message: String) : Exception(message)
class PermissionDenied(message: String) : Exception(message)
class EventAlreadyProcessedException(message: String) : Exception(message)
class SQLNotFoundException(message: String) : Exception(message)
class SQLConflictException(message: String) : Exception(message)

fun Application.configureErrorHandling() {

    val logger = KotlinLogging.logger {}

    install(StatusPages) {

        exception<UnauthorizedException> { call, err ->
            val ref = uuidString()
            logger.error { "${err.localizedMessage} ref: $ref" }
            call.respond(HttpStatusCode.Unauthorized, err.localizedMessage)
        }

        exception<PermissionDenied> { call, err ->
            val ref = uuidString()
            logger.error { "${err.localizedMessage} ref: $ref" }
            call.respond(HttpStatusCode.Forbidden, err.localizedMessage)
        }

        exception<JSONSchemaValidationException> { call, err ->
            val ref = uuidString()
            logger.error { "${err.localizedMessage} ref: $ref" }
            call.respond(HttpStatusCode.BadRequest, err.localizedMessage)
        }

        exception<SQLConflictException> { call, err ->
            val ref = uuidString()
            logger.error { "${err.localizedMessage} ref: $ref" }
            call.respond(HttpStatusCode.Conflict, err.localizedMessage)
        }

        exception<EventAlreadyProcessedException> { call, err ->
            val ref = uuidString()
            logger.error { "${err.localizedMessage} ref: $ref" }
            call.respond(HttpStatusCode.Conflict, err.localizedMessage)
        }

        exception<SQLNotFoundException> { call, err ->
            val ref = uuidString()
            logger.error { "${err.localizedMessage} ref: $ref" }
            call.respond(HttpStatusCode.NotFound, "Not found: $ref")
        }

        exception<Throwable> { call, err ->
            val ref = uuidString()
            logger.error { "${err.localizedMessage} ref: $ref" }
            call.respond(HttpStatusCode.InternalServerError, "Synchro failed. Ref: $ref")
        }
    }
}