package com.stabledata.dao

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.util.*

data class LogEntry (
    val id: String,
    val path: String,
    val actorId: String,
    val eventType: String,
    val createdAt: Long,
    val confirmedAt: Long,
)

class LogEntryBuilder {
    var id: String? = null
    var path: String? = null
    var actorId: String? = null
    var eventType: String? = null
    var createdAt: Long? = null
    var confirmedAt: Long? = null

    fun id(id: String) = apply { this.id = id }
    fun path(path: String) = apply { this.path = path }
    fun actorId(actorId: String) = apply { this.actorId = actorId }
    fun eventType(eventType: String) = apply { this.eventType = eventType }
    fun createdAt(createdAt: Long) = apply { this.createdAt = createdAt }
    fun confirmedAt(confirmedAt: Long) = apply { this.confirmedAt = confirmedAt }

    private val providedExplainer = "must be provided to LogEntryBuilder"
    fun build(): LogEntry {
        return LogEntry(
            id = requireNotNull(id) { "id $providedExplainer" },
            path = requireNotNull(path) { "path $providedExplainer" },
            actorId = requireNotNull(actorId) { "actorId $providedExplainer" },
            eventType = requireNotNull(eventType) { "eventType  $providedExplainer" },
            createdAt = requireNotNull(createdAt) { "createdAt timestamp $providedExplainer" },
            confirmedAt = requireNotNull(confirmedAt) { "confirmedAt timestamp $providedExplainer" }
        )
    }
}

object LogsTable : Table("stable.logs") {
    val eventId = uuid("id")
    val actorId = text("actor_id")
    val path = text("path")
    val eventType = text("event_type")
    val createdAt = long("created_at")
    val confirmedAt = long("confirmed_at")


    // later when writing data...
    // OR.... Do, we just make sure everything has a path... neat.
    // val documentId = uuid("document_id")


    fun findById(logId: String): LogEntry? {
        val row = LogsTable
            .select { eventId eq UUID.fromString(logId) }
            .singleOrNull()

        return row?.let {
            LogEntry(
                id = it[eventId].toString(),
                actorId = it[actorId],
                path = it[path],
                eventType = it[eventType],
                confirmedAt = it[confirmedAt],
                createdAt = it[createdAt]
            )
        }
    }

    fun insertLogEntry (entry: LogEntry): InsertStatement<Number> {
        return LogsTable.insert { log ->
            log[eventId] = UUID.fromString(entry.id)
            log[createdAt] = entry.createdAt
            log[eventType] = entry.eventType
            log[actorId] = entry.actorId
            log[confirmedAt] = entry.confirmedAt
            log[path] = entry.path
        }
    }

}