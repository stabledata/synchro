package com.stabledata.endpoint

import com.stabledata.dao.LogEntryBuilder
import com.stabledata.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*

data class RequestContext<T>(
    val body: T,
    val userCredentials: UserCredentials,
    val envelope: Envelope,
    val logEntry: LogEntryBuilder
)

suspend fun <T>PipelineContext<Unit, ApplicationCall>.contextualize(
    operation: String,
    bodyParser: suspend(body: String) -> T
): RequestContext<T>? {
    val postData = validate("$operation.json") { isValid, errors ->
        if (!isValid) {
            call.respond(HttpStatusCode.BadRequest, errors)
        }
    } ?: return null


    val userCredentials = permissions(operation) { error ->
        if (error !== null) {
            call.respond(error.status, error.message)
        }
    } ?: return null

    val envelope = idempotent { existingRecord, envelope ->
        existingRecord?.let {
            call.respond(
                HttpStatusCode.Conflict,
                "Event id: ${existingRecord.id} was processed on ${existingRecord.confirmedAt}"
            )
            return@idempotent null
        }
        envelope
    } ?: return null

    val body = bodyParser(postData)

    val logEntry = LogEntryBuilder()
        .eventType(operation)
        .actorId(userCredentials.id)
        .teamId(userCredentials.team)
        .id(envelope.eventId)
        .createdAt(envelope.createdAt)

    return RequestContext(
        body,
        userCredentials,
        envelope,
        logEntry
    )


}