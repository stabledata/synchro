package com.stabledata.model

import com.stabledata.BadRequestException
import com.stabledata.jsonParser
import kotlinx.serialization.Serializable
import stable.Schema

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
            return jsonParser.decodeFromString<Collection>(json)
        }

        fun fromMessage(message: Schema.CollectionRequest): Collection {
            if (message.id.isNullOrEmpty()) {
                throw BadRequestException("Collection id is required")
            }
            if (message.path.isNullOrEmpty()) {
                throw BadRequestException("Collection path is required")
            }
            return Collection(
                id = message.id,
                path = message.path,
                description = message.description.takeIf { it.isNotBlank() },
                icon = message.icon.takeIf { it.isNotBlank() },
                label = message.label.takeIf { it.isNotBlank() },
                type = message.type.takeIf { it.isNotBlank() }
            )
        }
    }
}