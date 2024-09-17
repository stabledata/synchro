package com.stabledata.plugins

import com.stabledata.getVerifier
import com.stabledata.validateCredentials
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

const val JWT_NAME = "stable-jwt-auth"
const val JWT_REALM = "stable-jwt-realm"
fun Application.configureAuth () {
    install(Authentication) {
        jwt(JWT_NAME) {
            realm = JWT_REALM
            verifier(getVerifier())
            validate{ credential -> validateCredentials(credential) }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "A valid bearer token must be included in the request.")
            }
        }
    }
}