package com.stabledata.context

import com.stabledata.PermissionDenied
import com.stabledata.UnauthorizedException
import com.stabledata.credentialFromToken
import com.stabledata.dao.AccessTable
import com.stabledata.getVerifier
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable

const val JWT_NAME = "stable-jwt-auth"
const val JWT_REALM = "stable-jwt-realm"
object Roles {
    val Admin = "admin"
    val Default = "member"
}
@Serializable
data class UserCredentials (
    val email: String,
    val team: String,
    val id: String,
    val role: String? = "member",
) : Principal {
    companion object {
        fun fromJWTCredential (credential: JWTCredential): UserCredentials {
            return UserCredentials(
                email = credential.payload.getClaim("email").asString(),
                team = credential.payload.getClaim("team").asString(),
                id = credential.payload.getClaim("id").asString(),
                role = credential.payload.getClaim("role").asString()
            )
        }

        fun validateJWTCredential (credential: JWTCredential): Boolean {
            val team = credential.payload.getClaim("team")
            val email = credential.payload.getClaim("email")
            val id = credential.payload.getClaim("id")
            val missingRequiredClaims = email.isNull || id.isNull || team.isNull

            // For dedicated team deployments in the future we can/should re-enable this for added team security.
            // val credentialsMatchDeployEnv = team.asString().equals(envString("STABLE_TEAM"))
            // return credentialsMatchDeployEnv && !missingRequiredClaims
            return !missingRequiredClaims
        }

        fun fromRawToken(tokenInHeader: String): UserCredentials {
            val token = removeBearerPrefix(tokenInHeader)
            val credential = credentialFromToken(token)
            if (!validateJWTCredential(credential)) {
                throw PermissionDenied("Could not create credentials from request")
            }
            return fromJWTCredential(credential)
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

fun PipelineContext<Unit, ApplicationCall>.permissions(
    operation: String
): UserCredentials {
    val userCredentials = call.principal<UserCredentials>()
    val logger = KotlinLogging.logger {}
    logger.debug { "Checking permissions for $userCredentials.id as ($userCredentials.role) on $operation" }
    userCredentials?.let {
        return credentialsCanPerformOperation(userCredentials, operation)
    }

    throw UnauthorizedException("Could not get get valid user credentials")
}

fun credentialsCanPerformOperation(userCredentials: UserCredentials, operation: String): UserCredentials {
    if (userCredentials.role == Roles.Admin) {
        return userCredentials
    }

    // otherwise, find matching access rules
    val roleToCheck = if (userCredentials.role.isNullOrEmpty())
        Roles.Default
    else
        userCredentials.role

    val (allowingRules, blockingRules) = AccessTable.findMatchingRules(
        userCredentials.team,
        checkRole = roleToCheck,
        checkPath = operation
    )

    var hasPermission = false

    // if there are matching allow rules, allow op
    if (allowingRules.isNotEmpty()) {
        hasPermission = true
    }

    // if there are any blocking rules, deny op
    if (blockingRules.isNotEmpty()) {
        hasPermission = false
    }

    if (!hasPermission) {
        throw PermissionDenied("User does not have permission to $operation")
    }

    return userCredentials
}

fun removeBearerPrefix(token: String): String {
    return if (token.startsWith("Bearer ", ignoreCase = true)) {
        token.substring(7) // Remove the "Bearer " part
    } else {
        token
    }
}