package com.stabledata.plugins.routing

import com.stabledata.*
import com.stabledata.Collection.createAtPathSQL
import com.stabledata.Collection.existsAtPathSQL
import com.stabledata.plugins.JWT_NAME
import com.stabledata.plugins.routing.io.CreateCollectionRequestBody
import com.stabledata.plugins.routing.io.CreateCollectionRequestResponseBody
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import java.util.*

fun Application.configureSchemaRouting(logger: Logger = getLogger()) {
    routing {
        authenticate(JWT_NAME) {

            post("schema/create.collection") {
                // validate body
                val body = call.receiveText()
                logger.debug("Create collection endpoint called with: $body")
                val (isValidPayload, errors) = validatePayloadAgainstSchema(
                    "create.collection.json",
                    body
                )

                if (!isValidPayload) {
                    return@post call.respond(HttpStatusCode.BadRequest, errors)
                }

                val requestBody = CreateCollectionRequestBody.fromJSON(body)
                val id = UUID.fromString(requestBody.id)

                // try to find an event with existing id, or see if the table exits already
                // best-effort idempotency
                val existingLog = Tables.Logs.findById(id)
                val existingSQL = existsAtPathSQL(requestBody.path)
                if (existingLog !== null || existingSQL) {
                    return@post call.respond(
                        HttpStatusCode.Conflict,
                        CreateCollectionRequestResponseBody(
                            id = id.toString()
                        )
                    )
                }

                // write logs and broadcast to ably in transaction
                try {
                    transaction {
                        Tables.Logs.insert { log ->
                            log[eventId] = id
                            log[eventType] = "collection.create"
                            log[actorId] = "foo@bar.com"
                            log[path] = requestBody.path
                            log[createdAt] = System.currentTimeMillis()
                        }
                        // create table
                        exec(createAtPathSQL(requestBody.path))
                    }
                } catch(e: ExposedSQLException) {
                    logger.error("Unable to create collection: ${e.localizedMessage}")
                    return@post call.respond(HttpStatusCode.InternalServerError, e.localizedMessage)
                }

                return@post call.respond(
                    HttpStatusCode.OK,
                    CreateCollectionRequestResponseBody(
                        id = id.toString()
                    )
                )
            }
        }
    }
}
