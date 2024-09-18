package com.stabledata

import com.fasterxml.uuid.Generators
import io.kotest.core.spec.style.WordSpec
import io.ktor.client.request.*
import io.ktor.client.request.headers
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.assertEquals

class SchemaEndpointsIntegrationTest : WordSpec({

    "schema collection endpoints" should {

            val collectionId = Generators.timeBasedEpochGenerator().generate()
            val token = generateJwtTokenWithCredentials(UserCredentials("ben@testing.co"))

            "create a new collection" {
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
                           "path":"classes"
                        }
                    """.trimIndent()
                        )
                    }
                    assertEquals(HttpStatusCode.OK, response.status)
                }
            }

            "receives idempotent response on retries" {
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
                           "path":"classes"
                        }
                    """.trimIndent()
                        )
                    }
                    assertEquals(HttpStatusCode.Conflict, response.status)
                }
            }
        }

})