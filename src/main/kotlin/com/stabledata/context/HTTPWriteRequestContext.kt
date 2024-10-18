package com.stabledata.context

import com.stabledata.dao.LogEntryBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*

data class WriteRequestContext<T>(
    val body: T,
    val userCredentials: UserCredentials,
    val envelope: Envelope,
    val logEntry: LogEntryBuilder
)

suspend fun <T>PipelineContext<Unit, ApplicationCall>.contextualizeHTTPWriteRequest(
    operation: String,
    jsonSchema: String,
    bodyParser: suspend (body: String) -> T
): WriteRequestContext<T>? {
    val postData = validate("$jsonSchema.json") { isValid, errors ->
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

    return WriteRequestContext(
        body,
        userCredentials,
        envelope,
        logEntry
    )


}