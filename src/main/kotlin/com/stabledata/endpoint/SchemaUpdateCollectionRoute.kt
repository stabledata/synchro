package com.stabledata.endpoint

import com.stabledata.Ably
import com.stabledata.dao.CollectionUpdateFailedException
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


fun Application.configureUpdateCollectionRoute() {
    val logger = KotlinLogging.logger {}
    routing {
        authenticate(JWT_NAME) {
            post("schema/collection/update") {
                val (collection, user, envelope, logEntry) = contextualizeWriteRequest(
                    "collection/update",
                    "collection/update"
                ) { postData ->
                    CollectionRequest.fromJSON(postData)
                } ?: return@post

                logger.debug { "Update collection requested by ${user.id} with event id ${envelope.eventId}" }

                // consider just putting this in the envelope?
                logEntry.path(collection.path)


                try {
                    val finalLogEntry = logEntry.build()
                    transaction {
                        CollectionsTable.updateAtPath(collection.path, collection)
                        LogsTable.insertLogEntry(finalLogEntry)
                        Ably.publish(user.team, "collection/update", finalLogEntry)
                    }

                    logger.debug {"Collection updated at path '${collection.path} with id ${collection.id}" }

                    return@post call.respond(
                        HttpStatusCode.OK,
                        finalLogEntry
                    )
                } catch (e: CollectionUpdateFailedException) {
                    logger.error { "Update collection transaction failed at update query: ${e.localizedMessage}" }
                    return@post call.respond(HttpStatusCode.InternalServerError, "Synchro service error")
                } catch (e: ExposedSQLException) {
                    logger.error { "Update collection transaction failure: ${e.localizedMessage}" }
                    return@post call.respond(HttpStatusCode.InternalServerError, "Synchro service error")
                }
            }
        }
    }
}
