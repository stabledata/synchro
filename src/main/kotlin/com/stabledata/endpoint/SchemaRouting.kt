package com.stabledata.endpoint

import com.stabledata.DatabaseOperations
import com.stabledata.dao.CollectionsTable
import com.stabledata.dao.LogEntryBuilder
import com.stabledata.dao.LogsTable
import com.stabledata.endpoint.io.CollectionsResponseBody
import com.stabledata.endpoint.io.CreateCollectionRequestBody
import com.stabledata.getLogger
import com.stabledata.plugins.JWT_NAME
import com.stabledata.plugins.permissions
import com.stabledata.plugins.validate
import idempotent
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import java.util.*


fun Application.configureSchemaRouting(logger: Logger = getLogger()) {
    routing {
        authenticate(JWT_NAME) {
            post("schema/create.collection") {
                val body = validate("create.collection.json") { isValid, errors ->
                    if (!isValid) {
                        call.respond(HttpStatusCode.BadRequest, errors)
                    }
                } ?: return@post

                val logEntry = LogEntryBuilder().eventType("create.collection")

                val userCredentials = permissions("create.collection") { hasPermission ->
                    if (!hasPermission) {
                        call.respond(HttpStatusCode.Forbidden, "You do not have permissions to perform this operation")
                    }
                } ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        "Unable to validate request credentials"
                    )

                logEntry.actorId(userCredentials.email)

                val envelope = idempotent { existingRecord, envelope ->
                    existingRecord?.let {
                        call.respond(
                            HttpStatusCode.Conflict,
                            "Event id: ${existingRecord.id} was processed on ${existingRecord.confirmedAt}"
                        )
                        return@idempotent null
                    }
                    envelope
                } ?: return@post

                logEntry.id(envelope.eventId)
                logEntry.createdAt(envelope.createdAt)

                logger.debug("Create collection requested by {} event id {}", userCredentials.email, envelope.eventId)

                val createCollectionRequest = CreateCollectionRequestBody.fromJSON(body)
                val requestedCollectionId = UUID.fromString(createCollectionRequest.id)

                logEntry.path(createCollectionRequest.path)
                logEntry.confirmedAt(System.currentTimeMillis())

                val response = CollectionsResponseBody(
                    id = requestedCollectionId.toString(),
                )

                // check if the table exists already
                if (DatabaseOperations.tableExistsAtPath(createCollectionRequest.path)){
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
                        LogsTable.insertLogEntry(logEntry.build())
                    }
                } catch(e: ExposedSQLException) {
                    logger.error("Create collection transaction failed: ${e.localizedMessage}")
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
