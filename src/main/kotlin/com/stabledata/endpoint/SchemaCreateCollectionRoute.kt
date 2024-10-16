package com.stabledata.endpoint

import com.stabledata.Ably
import com.stabledata.DatabaseOperations
import com.stabledata.dao.CollectionsTable
import com.stabledata.dao.LogsTable
import com.stabledata.endpoint.io.CollectionRequest
import com.stabledata.plugins.JWT_NAME
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureCreateCollectionRoute() {

    val logger = KotlinLogging.logger {}

    routing {
        authenticate(JWT_NAME) {
            post("schema/collection/create") {
                val (collection, user, envelope, logEntry) = contextualizeWriteRequest(
                    "collection/create",
                    "collection/create"
                ) { postData ->
                    CollectionRequest.fromJSON(postData)
                } ?: return@post

                logger.debug { "Create collection requested by ${user.id} with event id ${envelope.eventId}" }

                // consider requiring this as part of the request envelope
                logEntry.path(collection.path)

                // check for existing SQL table
                if (DatabaseOperations.tableExistsAtPath(user.team, collection.path)) {
                    return@post call.respond(
                        HttpStatusCode.Conflict,
                        "Path ${collection.path} already exists"
                    )
                }

                // also check for id collision, since this is not retryable.
                CollectionsTable.getCollection(collection.id)?.run {
                    return@post call.respond(
                        HttpStatusCode.Conflict,
                        "Collection id ${collection.id} already exists"
                    )
                }

                try {
                    val finalLogEntry = logEntry.build()

                    transaction {
                        exec(DatabaseOperations.createTableAtPathSQL(user.team, collection.path))
                        CollectionsTable.insertRowFromRequest(user.team, collection)
                        LogsTable.insertLogEntry(finalLogEntry)
                        Ably.publish(user.team, "collection/create", finalLogEntry)
                    }

                    logger.debug {"Collection created at path '${collection.path} with id ${collection.id}" }

                    return@post call.respond(
                        HttpStatusCode.Created,
                        finalLogEntry
                    )

                } catch (e: ExposedSQLException) {
                    logger.error { "Create collection transaction failed: ${e.localizedMessage}" }
                    return@post call.respond(HttpStatusCode.InternalServerError, "Synchro service error")
                }
            }
        }
    }
}
