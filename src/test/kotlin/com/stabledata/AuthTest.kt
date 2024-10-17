package com.stabledata

import com.stabledata.context.UserCredentials
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.Assert.assertNotNull
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

    @Test
    @Disabled("""
        This is just a smoke test. 
        the symmetric signing keys would have to match the pipeline, they don't.
    """)
    fun `verify JWT` () {
        val nodeGeneratedToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyXzAxSFJUQ1JIUDlWWDRCNFRGMFBONlRUM0NSIiwiaWF0IjoxNzI3Nzc1NDYyLCJleHAiOjE3MjgyOTM4NjIsIm9iamVjdCI6InVzZXIiLCJpZCI6InVzZXJfMDFIUlRDUkhQOVZYNEI0VEYwUE42VFQzQ1IiLCJlbWFpbCI6ImVtYWlsQGJlbmlwc2VuLmNvbSIsImVtYWlsVmVyaWZpZWQiOnRydWUsImZpcnN0TmFtZSI6IkJlbiIsInByb2ZpbGVQaWN0dXJlVXJsIjoiaHR0cHM6Ly93b3Jrb3NjZG4uY29tL2ltYWdlcy92MS9ER1BRcF9IbnVCZ2k1cnZWNzJIUnFMNE9zaXlfSXRDVnBBeFo4WHFfb1NnIiwibGFzdE5hbWUiOiJJcHNlbiIsImNyZWF0ZWRBdCI6IjIwMjQtMDMtMTJUMjI6MzM6MDYuNDY0WiIsInVwZGF0ZWRBdCI6IjIwMjQtMDktMjFUMDg6MDI6MzguOTExWiJ9.twZmbJGUoyKTwQDPh6sK8Gh6Av9n-K5WwnOdfl7JpbA"

        // this token was generated by jose lib.
        // of note, other libs did not seem to work
        val v = verifyToken(nodeGeneratedToken)
        assertNotNull(v)
    }

}