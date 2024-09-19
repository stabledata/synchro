package com.stabledata

import com.fasterxml.uuid.Generators
import io.github.serpro69.kfaker.Faker
import io.kotest.core.spec.style.WordSpec
import io.ktor.client.request.*
import io.ktor.client.request.headers
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.assertEquals

class SchemaEndpointsIntegrationTest : WordSpec({

    "schema collection endpoints" should {

            val faker = Faker()
            val collectionId = Generators.timeBasedEpochGenerator().generate()
            val token = generateJwtTokenWithCredentials(UserCredentials("ben@testing.co"))
            val collectionPath = faker.lorem.words()

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
                           "path":"$collectionPath"
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