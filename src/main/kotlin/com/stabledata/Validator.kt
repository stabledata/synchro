package com.stabledata

import io.github.optimumcode.json.schema.JsonSchema
import io.github.optimumcode.json.schema.ValidationError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json.Default.parseToJsonElement
import java.nio.file.Files
import java.nio.file.Paths

fun validatePayloadAgainstSchema(schemaFileName: String, payload: String): Pair<Boolean, List<ValidationError>> {
    val schemaLocation = "src/main/resources/openapi/schemas/$schemaFileName"
    val schemaJson = Files.readString(Paths.get(schemaLocation))
    val schema = JsonSchema.fromDefinition(schemaJson)
    val errors = mutableListOf<ValidationError>()
    try {
        val json = parseToJsonElement(payload)
        return Pair(
            schema.validate(json, errors::add),
            errors
        )
    } catch (e: SerializationException) {
        getLogger().error("Validation failed to parse JSON: $payload")
        return Pair(false, emptyList())
    }
}

