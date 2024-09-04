package com.stabledata

import ch.qos.logback.classic.Logger
import com.stabledata.plugins.configureRouting
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito

import kotlin.test.*

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
}

fun Application.testModule(logger: Logger) {
    configureLogging()
    staticConfig()
    configureRouting(logger)
}
