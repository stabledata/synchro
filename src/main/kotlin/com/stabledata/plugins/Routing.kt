package com.stabledata.plugins


import com.stabledata.UserCredentials
import com.stabledata.generateJwtTokenWithCredentials
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.openapi.*
import org.slf4j.Logger

fun Application.configureRouting(logger: Logger) {
    routing {

        openAPI(path="openapi", swaggerFile = "openapi/doc.yaml")
        staticResources("/openapi", basePackage = "openapi")

        get("/") {
            logger.info("Hello world endpoint called")
            call.respondText("Hello World!")
        }
        authenticate("auth-jwt") {

            get("/secure") {
                call.respondText("secured")
            }
        }

        get("/redoc") {
            call.respondText(
                this::class.java.classLoader.getResource("openapi/redoc.html")!!.readText(),
                io.ktor.http.ContentType.Text.Html
            )
        }

        get("/token") {
            val userCredentials = UserCredentials("ben")
            val token = generateJwtTokenWithCredentials(userCredentials)
            call.respond(token)
        }

    }
}
