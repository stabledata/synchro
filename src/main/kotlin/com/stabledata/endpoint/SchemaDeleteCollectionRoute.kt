package com.stabledata.endpoint

import com.stabledata.Ably
import com.stabledata.DatabaseOperations
import com.stabledata.dao.CollectionDeleteFailedException
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


fun Application.configureDeleteCollectionRoute() {

    val logger = KotlinLogging.logger {}

    routing {
        authenticate(JWT_NAME) {
            post("schema/collection/delete") {
                val (collection, user, envelope, logEntry) = contextualize(
                    "collection/delete"
                ) { postData ->
                    CollectionRequest.fromJSON(postData)
                } ?: return@post

                logger.debug { "Delete collection requested by ${user.id} with event id ${envelope.eventId}" }
                logEntry.path(collection.path)

                try {
                    val finalLogEntry = logEntry.build()
                    transaction {
                        CollectionsTable.deleteAtPath(collection.path)
                        exec(DatabaseOperations.dropTableAtPath(user.team, collection.path))
                        LogsTable.insertLogEntry(finalLogEntry)
                        Ably.publish(user.team, "collection/delete", finalLogEntry)
                    }

                    logger.debug {"Collection deleted at path '${collection.path} with id ${collection.id}" }

                    return@post call.respond(
                        HttpStatusCode.OK,
                        finalLogEntry
                    )
                } catch (e: CollectionDeleteFailedException) {
                    logger.error { "Delete collection transaction failed at delete query: ${e.localizedMessage}" }
                    return@post call.respond(HttpStatusCode.NotFound, e.localizedMessage)
                } catch (e: ExposedSQLException) {
                    logger.error { "Delete collection transaction failure: ${e.localizedMessage}" }
                    return@post call.respond(HttpStatusCode.InternalServerError, e.localizedMessage)
                }
            }
        }
    }
}
