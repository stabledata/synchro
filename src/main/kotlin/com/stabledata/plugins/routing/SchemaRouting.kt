package com.stabledata.plugins.routing

import com.stabledata.*
import com.stabledata.plugins.JWT_NAME
import com.stabledata.plugins.MissingCredentialsException
import com.stabledata.plugins.SynchroUserCredentials
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
                val credentials = call.principal<SynchroUserCredentials>()
                    ?: throw MissingCredentialsException()

                logger.debug("Create collection endpoint called with: $body by ${credentials.email}")
                val (isValidPayload, errors) = validatePayloadAgainstSchema(
                    "create.collection.json",
                    body
                )

                if (!isValidPayload) {
                    return@post call.respond(HttpStatusCode.BadRequest, errors)
                }

                val requestBody = CreateCollectionRequestBody.fromJSON(body)
                val id = UUID.fromString(requestBody.id)

                // try to find an event with existing id
                // "best-effort idempotency"
                val existingLog = Tables.Logs.findById(id)

                // also see if the table exits already
                val existingSQL = Tables.Collections.existsAtPath(requestBody.path)
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

                        // create table
                        exec(
                            Tables.Collections.createAtPathSQL(requestBody.path)
                        )

                        // add to stable.collections table
                        val collectionId = Tables.Collections.insert { collection ->
                            collection[path] = requestBody.path
                            collection[type] = requestBody.type
                            collection[label] = requestBody.label
                            collection[icon] = requestBody.icon
                            collection[description] = requestBody.description
                        } get Tables.Collections.id

                        // log event
                        Tables.Logs.insert { log ->
                            log[eventId] = id
                            log[eventType] = "collection.create"
                            log[actorId] = credentials.email
                            log[confirmedAt] = System.currentTimeMillis()
                            // FIXME -- this should come from payload "wrapper"
                            log[createdAt] = System.currentTimeMillis()
                            log[path] = requestBody.path
                            log[this.collectionId] = UUID.fromString(collectionId.value.toString())
                        }

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
