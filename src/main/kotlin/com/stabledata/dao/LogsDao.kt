package com.stabledata.dao

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object LogsTable : Table("stable.logs") {
    // Note: Exposed won't let you name things id that you intend to set from external data
    // https://github.com/JetBrains/Exposed/issues/1512
    val eventId = uuid("id")
    val actorId = text("actor_id")
    val eventType = text("event_type")

    // shit, we need to think about created at
    // because it probably should come from
    // a stable client payload wrapper, which we were trying to avoid at the synchro later
    val createdAt = long("created_at")
    val confirmedAt = long("confirmed_at").nullable() // Nullable because it's optional
    val path = text("path").nullable()

    val collectionId = uuid("collection_id").nullable()

    // later when writing data...
    // val documentId = uuid("document_id").nullable() // Nullable because it's optional

    fun findById(logId: String): ResultRow? {
        return transaction {
            LogsTable
                .select { eventId eq UUID.fromString(logId) }
                .singleOrNull()
        }
    }
}