package com.stabledata

import com.stabledata.context.Roles
import com.stabledata.context.StableEventIdHeader
import io.github.serpro69.kfaker.Faker
import io.kotest.core.spec.style.WordSpec
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.assertEquals

class AccessIntegrationTest: WordSpec({
    "access controls test" should {
        val ruleCreationEventId = uuidString()
        val ruleDeleteEventId = uuidString()
        val ruleId = uuidString()
        val tokenForCustomRole = generateTokenForTesting("test.role")
        val adminToken = generateTokenForTesting(Roles.Admin)

        "should deny test role collection create" {
            val faker = Faker()
            val collectionPath = faker.lorem.words()
            val collectionId = uuidString()
            val createCollectionId = uuidString()
            testApplication {
                application {
                    module()
                }
                val response = client.post("/schema/collection/create") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $tokenForCustomRole")
                        append(StableEventIdHeader, createCollectionId)
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
                assertEquals(HttpStatusCode.Forbidden, response.status)
            }
        }


        "should create a role that gives test.role collection/create access" {
            testApplication {
                application {
                    module()
                }
                val response = client.post("/access/grant") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $adminToken")
                        append(StableEventIdHeader, ruleCreationEventId)
                    }
                    contentType(ContentType.Application.Json)
                    setBody("""
                            {
                               "id": "$ruleId",
                               "role": "test.role", 
                               "path": "collection/create"
                            }
                        """.trimIndent()
                    )
                }
                assertEquals(HttpStatusCode.Created, response.status)
            }
        }

        "should have enabled a test role to create a collection" {
            val faker = Faker()
            val collectionPath = faker.lorem.words()
            val collectionId = uuidString()
            val createCollectionId = uuidString()
            testApplication {
                application {
                    module()
                }
                val response = client.post("/schema/collection/create") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $tokenForCustomRole")
                        append(StableEventIdHeader, createCollectionId)
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

        "should delete the access role it previously created" {
            testApplication {
                application {
                    module()
                }
                val response = client.post("/access/delete") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $adminToken")
                        append(StableEventIdHeader, ruleDeleteEventId)
                    }
                    contentType(ContentType.Application.Json)
                    setBody("""
                             {
                               "id": "$ruleId",
                               "role": "test.role", 
                               "path": "collection/create"
                            }
                        """.trimIndent()
                    )
                }
                assertEquals(HttpStatusCode.Created, response.status)
            }
        }
    }
})