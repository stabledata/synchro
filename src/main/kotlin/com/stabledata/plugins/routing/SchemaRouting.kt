package com.stabledata.plugins


import com.stabledata.getLogger
import com.stabledata.validatePayloadAgainstSchema
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import org.slf4j.Logger


fun Application.configureSchemaRouting(logger: Logger = getLogger()) {
    routing {
        authenticate(JWT_NAME) {

            post("schema/create.collection") {
                val body = call.receiveText()
                logger.debug("Create collection endpoint called with: $body")
                val (isValidPayload, errors) = validatePayloadAgainstSchema(
                    "create.collection.json",
                    body
                )

                if (!isValidPayload) {
                    return@post call.respond(HttpStatusCode.BadRequest, errors)
                }

                return@post call.respond(HttpStatusCode.OK, "huzzah")
            }
        }
    }
}
