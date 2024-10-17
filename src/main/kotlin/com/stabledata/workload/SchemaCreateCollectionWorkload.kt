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

fun schemaCreateCollectionWorkload(
    writeRequestContext: WriteRequestContext<Collection>
): LogEntry {

    val logger = KotlinLogging.logger {}

    val (collection, user, envelope, logEntry) = writeRequestContext
    logger.debug { "Create collection requested by ${user.id} with event id ${envelope.eventId}" }

    val finalLogEntry = logEntry
        .path(collection.path)
        .build()

    transaction {
        CollectionsTable.insertRowFromRequest(user.team, collection)
        exec(DatabaseOperations.createTableAtPathSQL(user.team, collection.path))
        LogsTable.insertLogEntry(finalLogEntry)
        Ably.publish(user.team, "collection/create", finalLogEntry)
    }

    logger.debug { "Collection created at path '${collection.path}" }

    return finalLogEntry
}