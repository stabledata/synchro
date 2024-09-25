package com.stabledata

import com.stabledata.plugins.UserCredentials
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SchemaEndpointsTest {
    @Test
    fun `responds unauthorized without token` () = testApplication {
        application {
            testModule(configureLogging())
        }

        val response = client.post("/schema/create.collection") {
            contentType(ContentType.Application.Json)
            setBody("")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `returns errors with an invalid payload` () = testApplication {
        application {
             // this sets default logger, or inject mock
            testModule(configureLogging())
        }

        val token = generateJwtTokenWithCredentials(UserCredentials("ben@testing.com", "test"))
        val response = client.post("/schema/create.collection") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
            contentType(ContentType.Application.Json)
            setBody("""
                {
                   "bad":"payload"
                }
            """.trimIndent())
        }
        val body = Json.parseToJsonElement(response.bodyAsText()) as JsonArray
        assertNotNull(body.size > 0)
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

}