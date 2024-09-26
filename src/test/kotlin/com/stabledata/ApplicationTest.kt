package com.stabledata

import com.stabledata.endpoint.configureChoresRouting
import com.stabledata.endpoint.configureSchemaRouting
import com.stabledata.plugins.configureDocsRouting
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.Database
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.slf4j.Logger
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
        assertEquals("ok", response.bodyAsText())
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
