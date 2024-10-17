package com.stabledata.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Access (
    val id: String,
    val type: String?,
    val role: String,
    val path: String
) {
    companion object {
        fun fromJSON (json: String): Access {
            val jsonParser = Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
                explicitNulls = false
            }
            return jsonParser.decodeFromString<Access>(json)
        }
    }
}