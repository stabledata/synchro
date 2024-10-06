package com.stabledata

import com.stabledata.endpoint.configureChoresRouting
import com.stabledata.endpoint.configureApplicationRouting
import com.stabledata.plugins.configureDocsRouting
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Disabled
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun `simple response healthcheck`() = testApplication {
        application {
            testModule()
        }

        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ok", response.bodyAsText())
    }


    @Test
    fun `requires auth for migrations`() = testApplication {
        application {
            testModuleWithDatabase()
        }

        val response = client.get("/migrate") {}
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    @Disabled("Enable this later. Now it gets in the way of manual db changes during early dev.")
    fun `attempts to run migrate and returns success or failure`() = testApplication {
        application {
            testModuleWithDatabase()
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

fun Application.testModule() {
    configurePlugins()
    configureApplicationRouting()
    configureChoresRouting()
    configureDocsRouting()
}

fun Application.testModuleWithDatabase() {
    configurePlugins()
    configureChoresRouting()
    configureApplicationRouting()
    configureDocsRouting()
    Database.connect(hikari())
}
