package com.stabledata

import com.fasterxml.uuid.Generators.timeBasedEpochGenerator
import com.stabledata.plugins.validateJSONUsingSchema
import kotlin.test.Test

class ValidatorTest {
    @Test
    fun `validates collection creation correctly` () {
        configureLogging()
        val uuid = timeBasedEpochGenerator().generate()
        val validJSON = """
            {
              "id": "$uuid",
              "path": "classes"
            }
        """.trimIndent()

        val (isValid, errors) = validateJSONUsingSchema("collection/create.json", validJSON)
        assert(isValid)
        assert(errors.isEmpty())
    }

    @Test
    fun `validates collection update correctly` () {
        configureLogging()
        val uuid = timeBasedEpochGenerator().generate()
        val validJSON = """
            {
              "id": "$uuid",
              "path": "classes",
              "icon":"folder",
               "type":"default",
               "label":"some nice stuff",
               "description":"we keep the nice stuff in here"
            }
        """.trimIndent()

        val (isValid, errors) = validateJSONUsingSchema("collection/update.json", validJSON)
        assert(isValid)
        assert(errors.isEmpty())
    }

    @Test
    fun `validates invalid schemas correctly` () {
        configureLogging()
        val invalidJSON = """
            {
              "foo": "bar"
            }
        """.trimIndent()

        val (isValid, errors) = validateJSONUsingSchema("collection/create.json", invalidJSON)
        assert(!isValid)
        assert(errors.isNotEmpty())
    }

    @Test
    fun `handles parsing errors` () {
        configureLogging()
        val notActuallyJSON = """
            Server error
        """.trimIndent()

        val (isValid, errors) = validateJSONUsingSchema("collection/create.json", notActuallyJSON)
        assert(!isValid)
        assert(errors.isEmpty())
    }

    @Test
    fun `handles schema not found errors` () {
        configureLogging()
        val notActuallyJSON = """
            Server error
        """.trimIndent()

        val (isValid, errors) = validateJSONUsingSchema("not.an.existing.schema.json", notActuallyJSON)
        assert(!isValid)
        assert(errors.isEmpty())
    }
}