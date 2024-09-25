package com.stabledata.plugins

import com.stabledata.getLogger
import io.github.optimumcode.json.schema.JsonSchema
import io.github.optimumcode.json.schema.ValidationError
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.errors.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json.Default.parseToJsonElement
import java.nio.file.Files
import java.nio.file.Paths

fun validateJSONUsingSchema(schemaFileName: String, payload: String): Pair<Boolean, List<String>> {
    try {
        val schemaLocation = "src/main/resources/openapi/schemas/$schemaFileName"
        val schemaJson = Files.readString(Paths.get(schemaLocation))
        val schema = JsonSchema.fromDefinition(schemaJson)
        val json = parseToJsonElement(payload)
        val errors = mutableListOf<ValidationError>()
        val isValid =
            schema.validate(json, errors::add)
        return Pair(
            isValid,
            errors.map {
                it.message
            }
        )
    } catch (e: IOException) {
        getLogger().error("Unable to locate schema at: resources/openapi/schemas/{}", schemaFileName)
        return Pair(false, emptyList())
    }  catch (e: SerializationException) {
        getLogger().error("Validation failed to parse JSON {}", payload)
        return Pair(false, emptyList())
    }
}

/**
 * Validates the raw body text against the schema file at schemaLocation, in resources
 * @param schemaLocation
 */
suspend fun PipelineContext<Unit, ApplicationCall>.validate(
    schemaLocation: String,
    block: suspend (Boolean, List<String>) -> Unit
): String? {
    val body = call.receiveText()
    val (isValid, errors) = validateJSONUsingSchema(schemaLocation, body)
    block(isValid, errors)
    return if(isValid) { body } else { null }
}
