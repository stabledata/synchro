package com.stabledata.plugins

import com.stabledata.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import java.util.*

fun Application.configureSchemaRouting(logger: Logger = getLogger()) {
    routing {
        authenticate(JWT_NAME) {

            post("schema/create.collection") {
                // validate body
                val body = call.receiveText()
                logger.debug("Create collection endpoint called with: $body")
                val (isValidPayload, errors) = validatePayloadAgainstSchema(
                    "create.collection.json",
                    body
                )

                if (!isValidPayload) {
                    return@post call.respond(HttpStatusCode.BadRequest, errors)
                }

                val requestBody = CreateCollectionRequestBody.fromJSON(body)
                val id = UUID.fromString(requestBody.id)

                // write logs and broadcast to ably in transaction
                try {
                    transaction {
                        Tables.Logs.insert { log ->
                            log[eventId] = id
                            log[eventType] = "collection.create"
                            log[actorId] = "foo@bar.com"
                            log[path] = requestBody.path
                            log[createdAt] = System.currentTimeMillis()
                        }

                        val convertedPath = requestBody.path.replace(".", "_")
                        val createTableSQL = """
                        CREATE TABLE $convertedPath (id UUID PRIMARY KEY)
                    """.trimIndent()
                        exec(createTableSQL)
                    }
                } catch(e: Exception) {
                    return@post call.respond(HttpStatusCode.InternalServerError, e)
                }

                return@post call.respond(HttpStatusCode.OK, "huzzah")
            }
        }
    }
}
