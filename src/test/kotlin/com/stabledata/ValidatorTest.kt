package com.stabledata

import com.fasterxml.uuid.Generators.timeBasedEpochGenerator
import kotlinx.serialization.json.Json.Default.parseToJsonElement
import kotlin.test.Test

class ValidatorTest {
    @Test
    fun `validates schemas correctly` () {
        val uuid = timeBasedEpochGenerator().generate()
        val validJSON = parseToJsonElement("""
            {
              "id": "$uuid",
              "path": "classes"
            }
        """.trimIndent())

        val (isValid, errors) = validatePayloadAgainstSchema("create.collection.json", validJSON)
        assert(isValid)
        assert(errors.size == 0)
    }

    @Test
    fun `validates invalid schemas correctly` () {
        val validJSON = parseToJsonElement("""
            {
              "foo": "bar"
            }
        """.trimIndent())

        val (isValid, errors) = validatePayloadAgainstSchema("create.collection.json", validJSON)
        assert(!isValid)
        assert(errors.size > 0)
    }
}