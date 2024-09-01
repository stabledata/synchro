package com.stabledata

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.serialization.Serializable

@Serializable
data class UserCredentials (val username: String)

fun getStableJwtSecret(): String {
    return System.getenv("JWT_SECRET") ?: "JWT_SECRET"
}

 fun generateJwtTokenWithCredentials(userCredentials: UserCredentials): String {
     return JWT
             .create()
             .withClaim("username", userCredentials.username)
             // .withExpiresAt(Date(System.currentTimeMillis() + 60000))
         .sign(Algorithm.HMAC256(getStableJwtSecret()))
 }

fun getVerifier (): JWTVerifier {
    val secret = getStableJwtSecret()
    val algorithm = Algorithm.HMAC256(secret)
    return JWT.require(algorithm)
        .build()  // Reusable verifier instance

}

fun validateCredentials(credential: JWTCredential): JWTPrincipal? {
    return if (credential.payload.getClaim("username").asString() != "") {
         JWTPrincipal(credential.payload)
    } else {
        null
    }

}

fun verifyToken(token: String): DecodedJWT? {
    val logger = getLogger()
    return try {

        val verifier = getVerifier()
        val decodedJWT: DecodedJWT = verifier.verify(token)
        decodedJWT
    } catch (exception: JWTVerificationException) {
        logger.error("Invalid token: ${exception.message}")
        null  // Return null if verification fails
    }
}