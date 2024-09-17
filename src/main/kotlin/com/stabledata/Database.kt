package com.stabledata

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Table

fun hikari (): HikariDataSource {
    val hikariConfig = HikariConfig().apply {
        driverClassName =  "org.postgresql.Driver"
        jdbcUrl = envString("STABLE_JDBC_URL")
        username = envString("STABLE_DB_USER")
        password = envString("STABLE_DB_PASSWORD")
        maximumPoolSize = envInt("STABLE_DB_MAX_CONNECTIONS")
    }

    return HikariDataSource(hikariConfig)
}

object Tables {
    object Logs : Table("stable.logs") {
        val eventId = uuid("id")
        val actorId = text("actor_id")
        val path = text("path").nullable() // Nullable since it's optional in the SQL definition
        val eventType = text("event_type")
        val createdAt = long("created_at")
//        val collectionId = uuid("collection_id").nullable() // Nullable because it's optional
//        val documentId = uuid("document_id").nullable() // Nullable because it's optional
//        val confirmedAt = long("confirmed_at").nullable() // Nullable because it's optional
    }
}

@Serializable
data class CreateCollectionRequestBody (
    val id: String,
    val path: String,
    val description: String? = null,
    val icon: String? = null,
    val label: String? = null,
    val type: String? = null
) {
    companion object {
        fun fromJSON (json: String): CreateCollectionRequestBody {
            val jsonParser = Json {
                ignoreUnknownKeys = true // Allows parsing even if some fields are missing
                isLenient = true // Allows more relaxed JSON parsing
                encodeDefaults = true // Serialize default values too
                explicitNulls = false // Omit fields that are null
            }
            return jsonParser.decodeFromString<CreateCollectionRequestBody>(json)
        }
    }
}