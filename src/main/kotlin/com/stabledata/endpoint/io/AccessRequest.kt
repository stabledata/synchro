package com.stabledata.endpoint.io

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AccessRequest (
    val id: String,
    val role: String,
    val operation: String?,
    val path: String?
) {
    companion object {
        fun fromJSON (json: String): AccessRequest {
            val jsonParser = Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
                explicitNulls = false
            }
            return jsonParser.decodeFromString<AccessRequest>(json)
        }
    }
}