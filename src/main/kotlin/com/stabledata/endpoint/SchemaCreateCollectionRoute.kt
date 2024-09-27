package com.stabledata.endpoint

import com.stabledata.DatabaseOperations
import com.stabledata.dao.CollectionsTable
import com.stabledata.dao.LogsTable
import com.stabledata.endpoint.io.CollectionsResponseBody
import com.stabledata.endpoint.io.CreateCollectionRequestBody
import com.stabledata.getLogger
import com.stabledata.plugins.JWT_NAME
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger


fun Application.configureCreateCollectionRoute(logger: Logger = getLogger()) {
    routing {
        authenticate(JWT_NAME) {
            post("schema/collection/create") {
                val (collection, userCredentials, envelope, logEntry) = contextualize(
                    "collection/create"
                ) { postData ->
                    CreateCollectionRequestBody.fromJSON(postData)
                } ?: return@post

                logger.debug("Create collection requested by {} event id: {}", userCredentials.email, envelope.eventId)

                // consider just putting this in the envelope?
                logEntry.path(collection.path)

                // check if the table exists already at the path
                // but... we should also check for collections that might have that path
                if (DatabaseOperations.tableExistsAtPath(collection.path)) {
                    return@post call.respond(
                        HttpStatusCode.Conflict,
                        "path ${collection.path} already exists"
                    )
                }

                try {
                    val finalLogEntry = logEntry.build()
                    transaction {
                        exec(DatabaseOperations.createTableAtPathSQL(collection.path))
                        CollectionsTable.insertRowFromRequest(collection)
                        LogsTable.insertLogEntry(finalLogEntry)
                    }

                    logger.debug("Collection created at path '{}', with id: {}", collection.path, collection.id)

                    return@post call.respond(
                        HttpStatusCode.OK,
                        CollectionsResponseBody(
                            id = collection.id,
                            confirmedAt = finalLogEntry.confirmedAt
                        )
                    )

                } catch (e: ExposedSQLException) {
                    logger.error("Create collection transaction failed: ${e.localizedMessage}")
                    return@post call.respond(HttpStatusCode.InternalServerError, e.localizedMessage)
                }
            }
        }
    }
}
