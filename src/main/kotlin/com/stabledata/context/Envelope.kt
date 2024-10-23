
package com.stabledata.context

import com.stabledata.EventAlreadyProcessedException
import com.stabledata.dao.LogsTable
import com.stabledata.grpc.GrpcContextInterceptor
import com.stabledata.uuidString
import io.grpc.Context
import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.transactions.transaction

data class Envelope(
    val eventId: String,
    val createdAt: Long
) {
    companion object {
        fun fromGrpcContext(): Envelope {
            val eventId: Any? = GrpcContextInterceptor.eventIdContext.get(Context.current())
            val eventIdString: String = eventId?.toString() ?: uuidString()
            val eventCreatedAt: Long = GrpcContextInterceptor.eventCreatedAtContext
                .get(Context.current())
                ?.toString()
                ?.toLongOrNull() ?: System.currentTimeMillis()

            return idempotency(
                eventIdString,  // Use eventIdString here
                eventCreatedAt
            )
        }
    }
}


const val StableEventIdHeader = "x-stable-event-id"
const val StableEventCreatedOnHeader = "x-stable-event-created-on"
// We may want to consider this as part of the envelope,
// though in this later, it's implicit in endpoint being called.
// const val StableEventTypeHeader = "x-stable-event"

/**
 * Checks for stable.log record that match incoming x-stable-event-id
 * and calls the lambda block with existing log entry and an Envelope
 * This will create the event id and use current ms if the headers
 * are not present, for example in (e.g. API calls)
 * @return Envelope?
 */
fun PipelineContext<Unit, ApplicationCall>.idempotencyCheck(): Envelope {
    val eventId = call.request.headers[StableEventIdHeader]
    val createdAt = call.request.headers[StableEventCreatedOnHeader]

    return idempotency(
        eventId,
        createdAt?.toLongOrNull() ?: System.currentTimeMillis()
    )
}

fun idempotency(eventId: String?, createdAt: Long): Envelope {

    val envelope = Envelope(
        eventId = eventId ?: uuidString(),
        createdAt = createdAt
    )

    if (eventId == null) {
        return envelope
    }

    val logEntry = transaction {
        LogsTable.findById(envelope.eventId)
    }

    if (logEntry != null) {
        throw EventAlreadyProcessedException("Event ${logEntry.id} was already processed on ${logEntry.confirmedAt}")
    }

    return envelope
}
