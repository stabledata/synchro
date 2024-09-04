package com.stabledata.plugins

import com.stabledata.validateCredentials
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureAuth () {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "stable"
            verifier(com.stabledata.getVerifier())
            validate{ credential -> validateCredentials(credential) }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "A valid bearer token must be included in the request.")
            }
        }
    }
}