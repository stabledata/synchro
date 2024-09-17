package com.stabledata

import io.github.optimumcode.json.schema.JsonSchema
import io.github.optimumcode.json.schema.ValidationError
//import io.ktor.server.application.*
//import io.ktor.server.request.*
//
//import kotlinx.serialization.SerializationException
//import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

import java.nio.file.Files
import java.nio.file.Paths

// Next: - use this in routing contexts
//suspend fun validateIncomingPOSTAgainstSchema(schemaFileName: String, call: ApplicationCall): Pair<Boolean, List<ValidationError>> {
//    try {
//        val payload = extractJsonFromApplicationCall(call)
//        return validatePayloadAgainstSchema(schemaFileName, payload)
//    } catch (e: SerializationException) {
//
//        return Pair(false, emptyList())
//    }
//}

// Next: - use this in routing context
//suspend fun extractJsonFromApplicationCall (call: ApplicationCall): JsonElement {
//    return Json.parseToJsonElement(call.receiveText())
//}

fun validatePayloadAgainstSchema(schemaFileName: String, payload: JsonElement): Pair<Boolean, MutableList<ValidationError>> {
    val schemaLocation = "src/main/resources/openapi/schemas/$schemaFileName"
    val schemaJson = Files.readString(Paths.get(schemaLocation))
    val schema = JsonSchema.fromDefinition(schemaJson)
    val errors = mutableListOf<ValidationError>()
    return Pair(
        schema.validate(payload, errors::add),
        errors
    )
}

