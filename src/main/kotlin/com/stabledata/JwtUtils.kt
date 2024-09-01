package com.stabledata

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
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

fun verifyToken(token: String): DecodedJWT? {
    val secret = getStableJwtSecret()
    val algorithm = Algorithm.HMAC256(secret)

    return try {
        // Create the verifier with the HS256 algorithm
        val verifier = JWT.require(algorithm)
            .build()  // Reusable verifier instance

        // Verify the token
        val decodedJWT: DecodedJWT = verifier.verify(token)
        println("Token is valid!")
        println("Username: " + decodedJWT.getClaim("username").asString())

        decodedJWT  // Return the decoded JWT object
    } catch (exception: JWTVerificationException) {
        // Token is invalid, handle the error
        println("Invalid token: ${exception.message}")
        null  // Return null if verification fails
    }
}