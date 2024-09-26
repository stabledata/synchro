package com.stabledata

import StableEventIdHeader
import com.fasterxml.uuid.Generators
import com.stabledata.plugins.UserCredentials
import io.github.serpro69.kfaker.Faker
import io.kotest.core.spec.style.WordSpec
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.assertEquals

class SchemaEndpointsIntegrationTest : WordSpec({

    "schema collection endpoints" should {

            val faker = Faker()
            val collectionId = Generators.timeBasedEpochGenerator().generate()
            val token = generateJwtTokenWithCredentials(UserCredentials("ben@testing.co", "test"))
            val collectionPath = faker.lorem.words()
            val eventId = Generators.timeBasedEpochGenerator().generate().toString()

            "create a new collection" {
                testApplication {
                    application {
                        module()
                    }
                    val response = client.post("/schema/create.collection") {
                        headers {
                            append(HttpHeaders.Authorization, "Bearer $token")
                            append(StableEventIdHeader, eventId)
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

            "returns conflict with existing event" {
                testApplication {
                    application {
                        module()
                    }
                    val response = client.post("/schema/create.collection") {
                        headers {
                            append(HttpHeaders.Authorization, "Bearer $token")
                            append(StableEventIdHeader, eventId)
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
                    val response = client.post("/schema/create.collection") {
                        headers {
                            append(HttpHeaders.Authorization, "Bearer $token")
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
                    assertEquals(HttpStatusCode.Conflict, response.status)
                }
            }


        }

})