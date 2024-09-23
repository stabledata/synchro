package com.stabledata.endpoint

import EnvelopeKey
import com.stabledata.DatabaseOperations
import com.stabledata.dao.CollectionsTable
import com.stabledata.dao.LogsTable
import com.stabledata.endpoint.io.CollectionsResponseBody
import com.stabledata.endpoint.io.CreateCollectionRequestBody
import com.stabledata.getLogger
import com.stabledata.plugins.JWT_NAME
import com.stabledata.plugins.UserCredentials
import com.stabledata.plugins.Validation.Plugin.validate
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import java.util.*


fun Application.configureSchemaRouting(logger: Logger = getLogger()) {
    routing {
        authenticate(JWT_NAME) {
            post("schema/create.collection") {
                val body = validate("create.collection.json") { isValid, errors ->
                    if (!isValid) {
                        return@validate call.respond(HttpStatusCode.BadRequest, errors)
                    }
                }

                val eventEnvelope = call.attributes[EnvelopeKey]
                val userCredentials = call.principal<UserCredentials>()
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Unable to validate request credentials"
                )


                logger.debug("Create collection requested by {} event id {}", userCredentials.email, eventEnvelope.eventId)

                val createCollectionRequest = CreateCollectionRequestBody.fromJSON(body)
                val requestedCollectionId = UUID.fromString(createCollectionRequest.id)
                val response = CollectionsResponseBody(
                    id = requestedCollectionId.toString(),
                    confirmedAt = System.currentTimeMillis()
                )

                // try to find an event with existing id
                // "best-effort idempotency"
                val hasExistingLog = LogsTable.findById(eventEnvelope.eventId) !== null

                // also see if the table exits already
                val existingSQL = DatabaseOperations.tableExistsAtPath(createCollectionRequest.path)

                // if either are true, we have a conflict.
                // TODO also handle the collection id itself?
                if (hasExistingLog || existingSQL) {
                    return@post call.respond(
                        HttpStatusCode.Conflict,
                        response
                    )
                }

                try {
                    transaction {
                        // create new public schema table at the path (consider dot syntax schema support in future!)
                        exec(DatabaseOperations.createTableAtPathSQL(createCollectionRequest.path))

                        // add new row to stable.collections table
                        CollectionsTable.insertRowFromRequest(createCollectionRequest)

                        // log the event
                        LogsTable.insert { log ->
                            log[collectionId] = requestedCollectionId
                            log[eventId] = UUID.fromString(eventEnvelope.eventId)
                            log[eventType] = "collection.create"
                            log[actorId] = userCredentials.email
                            log[confirmedAt] = response.confirmedAt
                            log[createdAt] = eventEnvelope.createdAt
                            log[path] = createCollectionRequest.path
                        }
                    }
                } catch(e: ExposedSQLException) {
                    logger.error("Unable to create collection: ${e.localizedMessage}")
                    return@post call.respond(HttpStatusCode.InternalServerError, e.localizedMessage)
                }

                logger.debug("Collection created at path {}, with id {}", createCollectionRequest.path, requestedCollectionId)

                return@post call.respond(
                    HttpStatusCode.OK,
                    response
                )
            }
        }
    }
}
