package com.stabledata.context

import com.stabledata.dao.LogEntryBuilder
import io.ktor.server.application.*
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
): WriteRequestContext<T> {
    val userCredentials = permissions(operation)
    val postData = validate("$jsonSchema.json")
    val envelope = idempotencyCheck()
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