package com.stabledata.endpoint

import com.stabledata.dao.CollectionDeleteFailedException
import com.stabledata.dao.CollectionsTable
import com.stabledata.dao.LogsTable
import com.stabledata.endpoint.io.CollectionRequest
import com.stabledata.endpoint.io.CollectionsResponse
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


fun Application.configureDeleteCollectionRoute(logger: Logger = getLogger()) {
    routing {
        authenticate(JWT_NAME) {
            post("schema/collection/delete") {
                val (collection, user, envelope, logEntry) = contextualize(
                    "collection/delete"
                ) { postData ->
                    CollectionRequest.fromJSON(postData)
                } ?: return@post

                logger.debug("Delete collection requested by {} event id: {}", user.email, envelope.eventId)
                logEntry.path(collection.path)

                try {
                    val finalLogEntry = logEntry.build()
                    transaction {
                        CollectionsTable.deleteAtPath(collection.path)
                        LogsTable.insertLogEntry(finalLogEntry)
                    }

                    logger.debug("Collection deleted at path '{} with id: {}", collection.path, collection.id)

                    return@post call.respond(
                        HttpStatusCode.OK,
                        CollectionsResponse(
                            id = collection.id,
                            confirmedAt = finalLogEntry.confirmedAt
                        )
                    )
                } catch (e: CollectionDeleteFailedException) {
                    logger.error("Delete collection transaction failed at update query: ${e.localizedMessage}")
                    return@post call.respond(HttpStatusCode.NotFound, e.localizedMessage)
                } catch (e: ExposedSQLException) {
                    logger.error("Delete collection transaction failure: ${e.localizedMessage}")
                    return@post call.respond(HttpStatusCode.InternalServerError, e.localizedMessage)
                }
            }
        }
    }
}
