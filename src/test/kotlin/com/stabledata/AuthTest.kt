package com.stabledata

import com.stabledata.context.UserCredentials
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Disabled
import kotlin.test.Test
import kotlin.test.assertEquals


class AuthTest {

    @Test
    fun `authorizes calls with ktor generated tokens` () = testApplication {
        application { module() }

        val token = generateTokenForTesting()
        val response = client.get("/secure") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }

        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    @Disabled("This can be re-enabled for dedicated team deployments")
    fun `requires team to claim to match` () = testApplication {
        application { module() }

        val token = jwtTokenWithCredentials(UserCredentials("ben@testing.com", "wrong.team", "bad.hombre"))
        val response = client.get("/secure") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }

        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `generate JWT` () {
        val jwt = generateTokenForTesting()
        println(jwt)
    }
}