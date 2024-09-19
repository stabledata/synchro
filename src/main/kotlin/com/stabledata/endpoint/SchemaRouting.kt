package com.stabledata.endpoint

import com.stabledata.DatabaseOperations
import com.stabledata.dao.CollectionsTable
import com.stabledata.dao.LogsTable
import com.stabledata.endpoint.io.CollectionsResponseBody
import com.stabledata.endpoint.io.CreateCollectionRequestBody
import com.stabledata.endpoint.io.callContextProvider
import com.stabledata.getLogger
import com.stabledata.plugins.JWT_NAME
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
                val (request, envelope, credentials, validation) = callContextProvider(
                    call,
                    "create.collection.json"
                ) { body -> CreateCollectionRequestBody.fromJSON(body)  }

                if (!validation.first) {
                    return@post call.respond(HttpStatusCode.BadRequest, validation.second)
                }

                logger.debug("Create collection requested by {} event id {}", credentials.email, envelope.stableEventId)

                val requestedCollectionId = UUID.fromString(request.id)
                val response = CollectionsResponseBody(
                    id = requestedCollectionId.toString(),
                    confirmedAt = System.currentTimeMillis()
                )

                // try to find an event with existing id
                // "best-effort idempotency"
                val hasExistingLog = LogsTable.findById(envelope.stableEventId) !== null

                // also see if the table exits already
                val existingSQL = DatabaseOperations.tableExistsAtPath(request.path)

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
                        // create new table at the path
                        exec(DatabaseOperations.createTableAtPathSQL(request.path))

                        // add new row to stable.collections table
                        CollectionsTable.insertRowFromRequest(request)

                        // log event
                        LogsTable.insert { log ->
                            log[collectionId] = requestedCollectionId
                            log[eventId] = UUID.fromString(envelope.stableEventId)
                            log[eventType] = "collection.create"
                            log[actorId] = credentials.email
                            log[confirmedAt] = response.confirmedAt
                            log[createdAt] = envelope.stableEventCreatedAt
                            log[path] = request.path
                        }
                    }
                } catch(e: ExposedSQLException) {
                    logger.error("Unable to create collection: ${e.localizedMessage}")
                    return@post call.respond(HttpStatusCode.InternalServerError, e.localizedMessage)
                }

                logger.debug("Collection created at path {}, with id {}", request.path, requestedCollectionId)

                return@post call.respond(
                    HttpStatusCode.OK,
                    response
                )
            }
        }
    }
}
