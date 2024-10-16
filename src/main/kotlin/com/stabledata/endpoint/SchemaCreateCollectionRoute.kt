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

                val finalLogEntry = logEntry
                    .path(collection.path)
                    .build()

                transaction {
                    CollectionsTable.insertRowFromRequest(user.team, collection)
                    exec(DatabaseOperations.createTableAtPathSQL(user.team, collection.path))
                    LogsTable.insertLogEntry(finalLogEntry)
                    Ably.publish(user.team, "collection/create", finalLogEntry)
                }

                logger.debug { "Collection created at path '${collection.path}" }

                return@post call.respond(
                    HttpStatusCode.Created,
                    finalLogEntry
                )
            }
        }
    }
}
