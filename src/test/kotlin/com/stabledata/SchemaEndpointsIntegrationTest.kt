package com.stabledata

import com.stabledata.plugins.StableEventIdHeader
import io.github.serpro69.kfaker.Faker
import io.kotest.core.spec.style.WordSpec
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
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
            testApplication {
                application {
                    module()
                }
                val response = client.post("/schema/collection/create") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                        append(StableEventIdHeader, creationEventId)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                            {
                               "id":"$collectionId",
                               "path":"$collectionPath"
                            }
                        """.trimIndent()
                    )
                }
                assertEquals(HttpStatusCode.Created, response.status)
            }
        }

        "returns conflict with existing event" {
            testApplication {
                application {
                    module()
                }
                val response = client.post("/schema/collection/create") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                        append(StableEventIdHeader, creationEventId)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                           "id":"$collectionId",
                           "path":"new.path.in.old.envelope"
                        }
                    """.trimIndent()
                    )
                }
                assertEquals(HttpStatusCode.Conflict, response.status)
            }
        }

        "returns conflict on existing path" {
            testApplication {
                application {
                    module()
                }
                val response = client.post("/schema/collection/create") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                    {
                       "id":"$collectionIdForPathCheck",
                       "path":"$collectionPath"
                    }
                """.trimIndent()
                    )
                }
                assertEquals(HttpStatusCode.Conflict, response.status)
            }
        }

        "returns conflict on existing id" {
            testApplication {
                application {
                    module()
                }
                val response = client.post("/schema/collection/create") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                    {
                       "id":"$collectionId",
                       "path":"new.path.existing.id"
                    }
                """.trimIndent()
                    )
                }
                assertEquals(HttpStatusCode.Conflict, response.status)
            }
        }

        "updates the collection" {
            testApplication {
                application {
                    module()
                }
                val response = client.post("/schema/collection/update") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                        append(StableEventIdHeader, updateEventId)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(
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
                    )
                }
                assertEquals(HttpStatusCode.OK, response.status)
            }
        }

        "returns not found for missing path" {
            testApplication {
                application {
                    module()
                }
                val response = client.post("/schema/collection/update") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                        append(StableEventIdHeader, uuidString())
                    }
                    contentType(ContentType.Application.Json)
                    setBody(
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
                    )
                }
                assertEquals(HttpStatusCode.NotFound, response.status)
            }
        }

        "deletes the collection" {
            testApplication {
                application {
                    module()
                }
                val response = client.post("/schema/collection/delete") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                        append(StableEventIdHeader, uuidString())
                    }
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                           "id":"$collectionId",
                           "path":"$collectionPath"
                        }
                    """.trimIndent()
                    )
                }
                assertEquals(HttpStatusCode.OK, response.status)
            }
        }
    }
})