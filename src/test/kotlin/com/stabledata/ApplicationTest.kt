package com.stabledata

import ch.qos.logback.classic.Logger
import com.stabledata.endpoint.configureRouting
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.Database
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun `simple response healthcheck`() = testApplication {
        val mockLogger = Mockito.mock(Logger::class.java)
        application {
            testModule(mockLogger)
        }

        val response = client.get("/")
        Mockito.verify(mockLogger).info(anyString())
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello World!", response.bodyAsText())
    }


    @Test
    fun `requires auth for migrations`() = testApplication {
        val mockLogger = Mockito.mock(Logger::class.java)
        application {
            testModuleWithDatabase(mockLogger)
        }

        val response = client.get("/migrate") {}
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `attempts to run migrate and returns success or failure`() = testApplication {
        val mockLogger = Mockito.mock(Logger::class.java)

        application {
            testModuleWithDatabase(mockLogger)
        }
        val token = generateTokenForTesting()
        val response = client.get("/migrate") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }
}

fun Application.testModule(logger: Logger) {
    configureLogging()
    staticConfig()
    configureRouting(logger)
}

fun Application.testModuleWithDatabase(logger: Logger) {
    configureLogging()
    staticConfig()
    configureRouting(logger)
    Database.connect(hikari())
}
