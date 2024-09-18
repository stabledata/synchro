package com.stabledata.plugins

import com.stabledata.getVerifier
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

const val JWT_NAME = "stable-jwt-auth"
const val JWT_REALM = "stable-jwt-realm"

class MissingCredentialsException : Exception("Unable to retrieve credentials from request")

@Serializable
data class SynchroUserCredentials (
    val email: String
) : Principal {
    companion object {
        fun fromJWTCredential (credential: JWTCredential): SynchroUserCredentials {
            return SynchroUserCredentials(email = credential.payload.getClaim("email").asString())
        }

        fun validateJWTCredential (credential: JWTCredential): Boolean {
            return !credential.payload.getClaim("email").isNull
        }
    }

}

fun Application.configureAuth () {
    install(Authentication) {
        jwt(JWT_NAME) {
            realm = JWT_REALM
            verifier(getVerifier())
            validate { credential ->
                if (SynchroUserCredentials.validateJWTCredential(credential)) {
                    SynchroUserCredentials.fromJWTCredential(credential)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "A valid bearer token must be included in the request.")
            }
        }
    }
}