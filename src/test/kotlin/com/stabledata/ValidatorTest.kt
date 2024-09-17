package com.stabledata

import com.fasterxml.uuid.Generators.timeBasedEpochGenerator
import kotlin.test.Test

class ValidatorTest {
    @Test
    fun `validates schemas correctly` () {
        val uuid = timeBasedEpochGenerator().generate()
        val validJSON = """
            {
              "id": "$uuid",
              "path": "classes"
            }
        """.trimIndent()

        val (isValid, errors) = validatePayloadAgainstSchema("create.collection.json", validJSON)
        assert(isValid)
        assert(errors.isEmpty())
    }

    @Test
    fun `validates invalid schemas correctly` () {
        val invalidJSON = """
            {
              "foo": "bar"
            }
        """.trimIndent()

        val (isValid, errors) = validatePayloadAgainstSchema("create.collection.json", invalidJSON)
        assert(!isValid)
        assert(errors.isNotEmpty())
    }

    @Test
    fun `handles parsing errors` () {
        configureLogging()
        val notActuallyJSON = """
            Server error
        """.trimIndent()

        val (isValid, errors) = validatePayloadAgainstSchema("create.collection.json", notActuallyJSON)
        assert(!isValid)
        assert(errors.isEmpty())
    }
}