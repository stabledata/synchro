package com.stabledata.endpoint

import com.stabledata.Ably
import com.stabledata.dao.AccessTable
import com.stabledata.dao.LogsTable
import com.stabledata.endpoint.io.AccessRequest
import com.stabledata.plugins.JWT_NAME
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureAccessCreateRoute() {

    val logger = KotlinLogging.logger {}

    routing {
        authenticate(JWT_NAME) {
            post("access/grant") {
                val (access, user, envelope, logEntry) = contextualizeWriteRequest(
                    "access/grant",
                    "access/manage"
                ) { postData ->
                    AccessRequest.fromJSON(postData)
                } ?: return@post

                // slightly borrowed, but not crazy use case issues in logs anyway
                logEntry.path("grant")

                logger.debug { "Grant access to roles requested by ${user.id} with event id ${envelope.eventId}" }

                try {
                    val finalLogEntry = logEntry.build()

                    transaction {
                        AccessTable.insertFromRequest("grant", user.team, access)
                        LogsTable.insertLogEntry(finalLogEntry)
                        Ably.publish(user.team, "access/manage", finalLogEntry)
                    }

                    logger.debug {"Access control record created for role ${access.role} on path ${access.path}" }

                    return@post call.respond(
                        HttpStatusCode.Created,
                        finalLogEntry
                    )

                } catch (e: ExposedSQLException) {
                    logger.error { "Grant access record failed: ${e.localizedMessage}" }
                    return@post call.respond(HttpStatusCode.InternalServerError, e.localizedMessage)
                }
            }
        }
    }
}
