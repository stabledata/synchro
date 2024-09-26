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
import java.util.*


fun Application.configureSchemaRouting(logger: Logger = getLogger()) {
    routing {
        authenticate(JWT_NAME) {
            post("schema/create.collection") {

                val (body, userCredentials, envelope, logEntry) = contextualize(
                    "create.collection"
                ) {
                    postData -> CreateCollectionRequestBody.fromJSON(postData)
                }?: return@post

                logger.debug("Create collection requested by {} event id {}", userCredentials.email, envelope.eventId)
                logEntry.path(body.path)

                val requestedCollectionId = UUID.fromString(body.id)
                val response = CollectionsResponseBody(
                    id = requestedCollectionId.toString(),
                )

                // check if the table exists already
                if (DatabaseOperations.tableExistsAtPath(body.path)){
                    return@post call.respond(
                        HttpStatusCode.Conflict,
                        response
                    )
                }

                try {
                    transaction {
                        // create new public schema table at the path
                        // consider dot syntax schema support in future!
                        exec(
                            DatabaseOperations.createTableAtPathSQL(body.path)
                        )

                        // add new row to stable.collections table
                        CollectionsTable.insertRowFromRequest(body)

                        // log the event
                        LogsTable.insertLogEntry(logEntry.build())
                    }
                } catch(e: ExposedSQLException) {
                    logger.error("Create collection transaction failed: ${e.localizedMessage}")
                    return@post call.respond(HttpStatusCode.InternalServerError, e.localizedMessage)
                }

                logger.debug("Collection created at path {}, with id {}", body.path, requestedCollectionId)

                return@post call.respond(
                    HttpStatusCode.OK,
                    response
                )
            }
        }
    }
}
