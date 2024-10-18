package com.stabledata

import com.fasterxml.uuid.Generators
import kotlinx.serialization.json.Json
import java.util.*

fun uuid(): UUID = Generators.timeBasedEpochGenerator().generate()
fun uuidString() = uuid().toString()
fun String?.orDefault(default: String): String {
    return this ?: default
}

val jsonParser = Json {
    ignoreUnknownKeys = true // Allows parsing even if some fields are missing
    isLenient = true // Allows more relaxed JSON parsing
    encodeDefaults = true // Serialize default values too
    explicitNulls = false // Omit fields that are null
}