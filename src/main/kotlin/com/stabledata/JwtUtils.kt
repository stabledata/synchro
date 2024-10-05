package com.stabledata

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.stabledata.plugins.UserCredentials
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

fun getStableJwtSecret(): String {
    return System.getenv("JWT_SECRET") ?: "JWT_SECRET"
}

 fun generateJwtTokenWithCredentials(userCredentials: UserCredentials): String {
     val oneWeekInMillis = 7 * 24 * 60 * 60 * 1000
     return JWT
             .create()
             .withClaim("email", userCredentials.email)
             .withClaim("team", userCredentials.team)
             .withClaim("id", userCredentials.id)
             .withExpiresAt(Date(System.currentTimeMillis() + oneWeekInMillis))
         .sign(Algorithm.HMAC256(getStableJwtSecret()))
 }

fun generateTokenForTesting(): String {
    val token = generateJwtTokenWithCredentials(UserCredentials("ben@testing.com", "test", "fake.id"))
    return token
}

fun getVerifier (): JWTVerifier {
    val secret = getStableJwtSecret()
    val algorithm = Algorithm.HMAC256(secret)
    return JWT
        .require(algorithm)
        .build()

}

fun verifyToken(token: String): DecodedJWT? {
    val logger = KotlinLogging.logger {}
    return try {
        val verifier = getVerifier()
        val decodedJWT: DecodedJWT = verifier.verify(token)
        decodedJWT
    } catch (exception: JWTVerificationException) {
        logger.error {"Invalid token: ${exception.message}" }
        null
    }
}