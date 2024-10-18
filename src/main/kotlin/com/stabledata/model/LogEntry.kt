package com.stabledata.model

import kotlinx.serialization.Serializable
import stable.LogEntry.LogEntryMessage

@Serializable
data class LogEntry (
    val id: String,
    val teamId: String,
    val path: String,
    val actorId: String,
    val eventType: String,
    val createdAt: Long,
    val confirmedAt: Long,
)

fun LogEntry.toMessage(): LogEntryMessage {
    return LogEntryMessage.newBuilder()
        .setId(this.id)
        .setTeamId(this.teamId)
        .setPath(this.path)
        .setActorId(this.actorId)
        .setEventType(this.eventType)
        .setCreatedAt(this.createdAt)
        .setConfirmedAt(this.confirmedAt)
        .build()
}