package com.stabledata.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Collection (
    val id: String,
    val path: String,
    val description: String? = null,
    val icon: String? = null,
    val label: String? = null,
    val type: String? = null
) {
    companion object {
        fun fromJSON (json: String): Collection {
            val jsonParser = Json {
                ignoreUnknownKeys = true // Allows parsing even if some fields are missing
                isLenient = true // Allows more relaxed JSON parsing
                encodeDefaults = true // Serialize default values too
                explicitNulls = false // Omit fields that are null
            }
            return jsonParser.decodeFromString<Collection>(json)
        }
    }
}