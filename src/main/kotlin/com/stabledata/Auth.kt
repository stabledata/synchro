package com.stabledata

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureAuth () {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "stable"
            verifier(getVerifier())
            validate{ credential -> validateCredentials(credential) }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }
}