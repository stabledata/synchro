package com.stabledata

import com.stabledata.endpoint.configureChoresRouting
import com.stabledata.endpoint.configureSchemaRouting
import com.stabledata.plugins.configureDocsRouting
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jetbrains.exposed.sql.Database
import org.slf4j.Logger
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun `simple response healthcheck`() = testApplication {
        val mockLogger = mockk<Logger>()
        every { mockLogger.info(any()) } answers {}
        application {
            testModule(mockLogger)
        }

        val response = client.get("/")
        verify {
            mockLogger.info(any())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ok", response.bodyAsText())
    }


    @Test
    fun `requires auth for migrations`() = testApplication {
        val mockLogger = mockk<Logger>()
        application {
            testModuleWithDatabase(mockLogger)
        }

        val response = client.get("/migrate") {}
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `attempts to run migrate and returns success or failure`() = testApplication {
        application {
            testModuleWithDatabase(configureLogging())
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
    configurePlugins()
    configureSchemaRouting(logger)
    configureChoresRouting(logger)
    configureDocsRouting()
}

fun Application.testModuleWithDatabase(logger: Logger) {
    configurePlugins()
    configureChoresRouting(logger)
    configureSchemaRouting(logger)
    configureDocsRouting()
    Database.connect(hikari())
}
