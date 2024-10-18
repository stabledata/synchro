package com.stabledata.workload

import com.stabledata.Ably
import com.stabledata.dao.CollectionsTable
import com.stabledata.model.LogEntry
import com.stabledata.dao.LogsTable
import com.stabledata.context.WriteRequestContext
import com.stabledata.model.Collection
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction

fun schemaUpdateCollectionWorkload(
    writeRequestContext: WriteRequestContext<Collection>
): LogEntry {
    val logger = KotlinLogging.logger {}
    val (collection, user, envelope, logEntry) = writeRequestContext

    logger.debug { "Update collection requested by ${user.id} with event id ${envelope.eventId}" }

    // consider just putting this in the envelope?
    logEntry.path(collection.path)

    val finalLogEntry = logEntry.build()
    transaction {
        CollectionsTable.updateAtPath(collection.path, collection)
        LogsTable.insertLogEntry(finalLogEntry)
        Ably.publish(user.team, "collection/update", finalLogEntry)
    }

    logger.debug {"Collection updated at path '${collection.path} with id ${collection.id}" }

    return finalLogEntry
}