package com.stabledata.model

import kotlinx.serialization.Serializable

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