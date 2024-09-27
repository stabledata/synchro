
package com.stabledata.plugins

import com.fasterxml.uuid.Generators
import com.stabledata.dao.LogEntry
import com.stabledata.dao.LogsTable
import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.transactions.transaction

data class Envelope(
    val eventId: String,
    val createdAt: Long
)

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
suspend fun PipelineContext<Unit, ApplicationCall>.idempotent(
    block: suspend (LogEntry?, Envelope?) -> Envelope?
): Envelope? {
    val eventId = call.request.headers[StableEventIdHeader]
    val createdAt = call.request.headers[StableEventCreatedOnHeader]


    val envelope = Envelope(
        eventId = eventId ?:
        Generators.timeBasedEpochGenerator().generate().toString(),
        createdAt = createdAt?.toLong() ?:
        System.currentTimeMillis()

    )

    // if there is no event id in the header, we can short circuit false
    // to the block to avoid lookup overhead
    if (eventId == null) {
        return block(null, envelope)
    }

    val logEntry = transaction {
        LogsTable.findById(envelope.eventId)
    }

    return block(logEntry, envelope)
}
