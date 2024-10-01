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
            testModule()
        }

        val response = client.post("/schema/collection/create") {
            contentType(ContentType.Application.Json)
            setBody("")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `returns errors with an invalid payload` () = testApplication {
        application {
             // this sets default logger, or inject mock
            testModule()
        }

        val token = generateJwtTokenWithCredentials(UserCredentials("ben@testing.com", "test"))

        val response = client.post("/schema/collection/create") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
            contentType(ContentType.Application.Json)
            setBody("""
                {
                   "id":"payload"
                }
            """.trimIndent())
        }
        val body = Json.parseToJsonElement(response.bodyAsText()) as JsonArray
        assertNotNull(body.size > 0)
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

// mocking exposed is hard, the internet agrees, figure this out later
// or just keep moving with integration tests.

//    @Test
//    fun `logs and returns server error when transactions fail`() = testApplication {
//        val mockLogger = mockk<Logger>()
//        application {
//            testModuleWithDatabase(mockLogger)
//        }
//
//        val mockTable = mockk<Table>()
////        val mockTransaction = mockk<Transaction>()
////        every { mockTransaction.exec<Boolean>(any()) } returns true
//        every { mockTable.insert {} } throws SQLException("Blew up")
//        every { mockLogger.info(any()) }
//
//        val id = Generators.timeBasedEpochGenerator().generate().toString();
//        val token = generateJwtTokenWithCredentials(UserCredentials("ben@testing.com", "test"))
//        val response = client.post("/schema/collection/create") {
//            headers {
//                append(HttpHeaders.Authorization, "Bearer $token")
//                append(StableEventIdHeader, eventId())
//            }
//            contentType(ContentType.Application.Json)
//            setBody("""
//                {
//                   "id":"$id",
//                   "path":"foo"
//                }
//            """.trimIndent())
//        }
//        println(response.bodyAsText())
//        assertEquals(HttpStatusCode.InternalServerError, response.status)
//
//    }

}