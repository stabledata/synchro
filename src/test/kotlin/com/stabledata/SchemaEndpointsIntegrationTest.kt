package com.stabledata

import com.stabledata.context.generateTokenForTesting
import io.github.serpro69.kfaker.Faker
import io.kotest.core.spec.style.WordSpec
import io.ktor.http.*
import kotlin.test.assertEquals

class SchemaEndpointsIntegrationTest : WordSpec({

    "schema collection create, update, delete workflow" should {

        val faker = Faker()
        val token = generateTokenForTesting("admin")
        val collectionPath = faker.lorem.words()
        val collectionId = uuidString()
        val collectionIdForPathCheck = uuidString()
        val creationEventId = uuidString()
        val updateEventId = uuidString()

        "create a new collection" {
            testWithDefaultAppModule {
                val response = postJson(
                    uri = "/schema/collection/create",
                    token = token,
                    eventId = creationEventId
                ) {
                     """
                        {
                           "id":"$collectionId",
                           "path":"$collectionPath"
                        }
                    """.trimIndent()
                }
                assertEquals(HttpStatusCode.Created, response.status)
            }
        }

        "returns conflict with existing event" {
            testWithDefaultAppModule {
                val response = postJson(
                    uri = "/schema/collection/create",
                    token = token,
                    eventId = creationEventId
                ) {
                    """
                    {
                       "id":"$collectionId",
                       "path":"new.path.in.old.envelope"
                    }
                    """.trimIndent()
                }
                assertEquals(HttpStatusCode.Conflict, response.status)
            }
        }

        "returns conflict on existing path" {
            testWithDefaultAppModule {
                val response = postJson(
                    uri = "/schema/collection/create",
                    token = token
                ) {
                    """
                    {
                       "id":"$collectionIdForPathCheck",
                       "path":"$collectionPath"
                    }
                    """.trimIndent()
                }
                assertEquals(HttpStatusCode.Conflict, response.status)
            }
        }

        "returns conflict on existing id" {
            testWithDefaultAppModule {
                val response = postJson(
                    uri = "/schema/collection/create",
                    token = token
                ) {
                    """
                    {
                       "id":"$collectionId",
                       "path":"new.path.existing.id"
                    }
                    """.trimIndent()
                }
                assertEquals(HttpStatusCode.Conflict, response.status)
            }
        }

        "updates the collection" {
            testWithDefaultAppModule {
                val response = postJson(
                    uri = "/schema/collection/update",
                    token = token,
                    eventId = updateEventId
                ) {
                    """
                    {
                       "id":"$collectionId",
                       "path":"$collectionPath",
                       "icon":"folder",
                       "type":"default",
                       "label":"some nice stuff",
                       "description":"we keep the nice stuff in here"
                    }
                    """.trimIndent()
                }
                assertEquals(HttpStatusCode.OK, response.status)
            }
        }

        "returns not found for missing path" {
            testWithDefaultAppModule {
                val response = postJson(
                    uri = "/schema/collection/update",
                    token = token,
                    eventId = uuidString()
                ) {
                    """
                    {
                       "id":"$collectionId",
                       "path":"this.path.never.existed",
                       "icon":"folder",
                       "type":"default",
                       "label":"some nice stuff",
                       "description":"we keep the nice stuff in here"
                    }
                    """.trimIndent()
                }
                assertEquals(HttpStatusCode.NotFound, response.status)
            }
        }

        "deletes the collection" {
            testWithDefaultAppModule {
                val response = postJson(
                    uri = "/schema/collection/delete",
                    token = token,
                    eventId = uuidString()
                ) {
                    """
                    {
                       "id":"$collectionId",
                       "path":"$collectionPath"
                    }
                    """.trimIndent()
                }
                assertEquals(HttpStatusCode.OK, response.status)
            }
        }
    }
})