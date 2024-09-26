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


fun Application.configureSchemaRouting(logger: Logger = getLogger()) {
    routing {
        authenticate(JWT_NAME) {
            post("schema/create.collection") {
                val (body, userCredentials, envelope, logEntry) = contextualize(
                    "create.collection"
                ) { postData ->
                    CreateCollectionRequestBody.fromJSON(postData)
                } ?: return@post

                logger.debug("Create collection requested by {} event id: {}", userCredentials.email, envelope.eventId)

                // consider just putting this in the envelope?
                logEntry.path(body.path)

                // check if the table exists already at the path
                if (DatabaseOperations.tableExistsAtPath(body.path)) {
                    return@post call.respond(
                        HttpStatusCode.Conflict,
                        "path ${body.path} already exists"
                    )
                }

                try {
                    val finalLogEntry = logEntry.build()
                    transaction {
                        exec(DatabaseOperations.createTableAtPathSQL(body.path))
                        CollectionsTable.insertRowFromRequest(body)
                        LogsTable.insertLogEntry(finalLogEntry)
                    }

                    logger.debug("Collection created at path '{}', with id: {}", body.path, body.id)

                    return@post call.respond(
                        HttpStatusCode.OK,
                        CollectionsResponseBody(
                            id = body.id,
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
