package com.stabledata.workload

import com.stabledata.Ably
import com.stabledata.DatabaseOperations
import com.stabledata.dao.CollectionsTable
import com.stabledata.dao.LogEntry
import com.stabledata.dao.LogsTable
import com.stabledata.endpoint.WriteRequestContext
import com.stabledata.model.Collection
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction


fun schemaDeleteCollectionWorkload(
    writeRequestContext: WriteRequestContext<Collection>
): LogEntry {

    val logger = KotlinLogging.logger {}
    val (collection, user, envelope, logEntry) = writeRequestContext;
    logger.debug { "Delete collection requested by ${user.id} with event id ${envelope.eventId}" }

    val finalLogEntry = logEntry
        .path(collection.path)
        .build()

    transaction {
        CollectionsTable.deleteAtPath(collection.path)
        exec(DatabaseOperations.dropTableAtPath(user.team, collection.path))
        LogsTable.insertLogEntry(finalLogEntry)
        Ably.publish(user.team, "collection/delete", finalLogEntry)
    }

    logger.debug {"Collection deleted at path '${collection.path} with id ${collection.id}" }

    return finalLogEntry
}