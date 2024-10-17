package com.stabledata

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.stabledata.context.Roles
import com.stabledata.context.UserCredentials
import io.github.oshai.kotlinlogging.KotlinLogging
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
    withRole: String? = Roles.Default,
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
