package com.stabledata.context

import com.stabledata.Operations
import com.stabledata.dao.LogEntryBuilder
import io.grpc.Context
import io.ktor.server.application.*
import io.ktor.util.pipeline.*

data class WriteRequestContext<T>(
    val body: T,
    val userCredentials: UserCredentials,
    val envelope: Envelope,
    val logEntry: LogEntryBuilder
) {
    companion object {
        fun <T>fromGrpcContext(
            messageBuilder: () -> T
        ): WriteRequestContext<T> {
            val token = GrpcContextInterceptor.tokenContext.get(Context.current()).toString()
            val userCredentials = credentialsCanPerformOperation(
                UserCredentials.fromRawToken(token),
                Operations.Schema.CREATE_COLLECTION
            )
            val envelope = Envelope.fromGrpcContext()
            val logEntry = LogEntryBuilder()
                .eventType(Operations.Schema.CREATE_COLLECTION)
                .actorId(userCredentials.id)
                .teamId(userCredentials.team)
                .id(envelope.eventId)
                .createdAt(envelope.createdAt)

            return WriteRequestContext(
                body = messageBuilder(),
                userCredentials = userCredentials,
                envelope = envelope,
                logEntry = logEntry
            )
        }
    }
}
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