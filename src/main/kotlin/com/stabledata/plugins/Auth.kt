package com.stabledata.plugins

import com.stabledata.envString
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
data class UserCredentials (
    val email: String,
    val team: String
) : Principal {
    companion object {
        fun fromJWTCredential (credential: JWTCredential): UserCredentials {
            return UserCredentials(
                email = credential.payload.getClaim("email").asString(),
                team = credential.payload.getClaim("team").asString()
            )
        }

        fun validateJWTCredential (credential: JWTCredential): Boolean {
            val team = credential.payload.getClaim("team")
            val hasNullRequiredClaims = (
                credential.payload.getClaim("email").isNull ||
                team.isNull
            )

            val credentialsMatchDeployEnv = team.asString().equals(envString("STABLE_TEAM"))

            return credentialsMatchDeployEnv && !hasNullRequiredClaims
        }
    }

}

fun Application.configureAuth () {
    install(Authentication) {
        jwt(JWT_NAME) {
            realm = JWT_REALM
            verifier(getVerifier())
            validate { credential ->
                if (UserCredentials.validateJWTCredential(credential)) {
                    UserCredentials.fromJWTCredential(credential)
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
