package com.stabledata.model

import com.stabledata.jsonParser
import kotlinx.serialization.Serializable

@Serializable
data class Access (
    val id: String,
    val type: String?,
    val role: String,
    val path: String
) {
    companion object {
        fun fromJSON (json: String): Access {
            return jsonParser.decodeFromString<Access>(json)
        }
    }
}