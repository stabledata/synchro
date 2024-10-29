package com.stabledata

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.stabledata.context.Roles
import com.stabledata.context.UserCredentials
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.jwt.*
import java.util.*

fun getStableJwtSecret(): String {
    return System.getenv("JWT_SECRET") ?: "JWT_SECRET"
}

 fun jwtTokenWithCredentials(userCredentials: UserCredentials): String {
     val oneWeekInMillis = 7 * 24 * 60 * 60 * 1000
     return JWT
             .create()
             .withClaim("email", userCredentials.email)
             .withClaim("team", userCredentials.team)
             .withClaim("id", userCredentials.id)
             .withClaim("role", userCredentials.role)
             .withExpiresAt(Date(System.currentTimeMillis() + oneWeekInMillis))
         .sign(Algorithm.HMAC256(getStableJwtSecret()))
 }

fun generateTokenForTesting(
    withRole: String? = Roles.MEMBER,
    withTeam: String? = "test",
    withEmail: String? = "test@makeitstable.com"
): String {
    val fakeCredentials = UserCredentials(
        withEmail.orDefault("test@makeitstable.com"),
        withTeam.orDefault("test"),
        "testing.user.id",
        withRole
    )
    val token = jwtTokenWithCredentials(fakeCredentials)
    return token
}

fun getVerifier (): JWTVerifier {
    val secret = getStableJwtSecret()
    val algorithm = Algorithm.HMAC256(secret)
    return JWT
        .require(algorithm)
        .build()
}

fun credentialFromToken(token: String): JWTCredential {
    val logger = KotlinLogging.logger {}
    val verifier = getVerifier()
    try {
        val decodedJWT = verifier.verify(token)
        decodedJWT.claims.mapValues { it.value.asString() }
        return JWTCredential(decodedJWT)
    } catch (e: Exception) {
        logger.error { "Failed to verify JWT token: $token " }
        throw UnauthorizedException("Request requires a valid bearer token")
    }
}
