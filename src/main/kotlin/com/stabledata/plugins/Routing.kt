package com.stabledata.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.openapi.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        openAPI(path="openapi", swaggerFile = "openapi/doc.yaml")
        staticResources("/openapi", basePackage = "openapi")
        get("/redoc") {
            call.respondText(
                this::class.java.classLoader.getResource("openapi/redoc.html")!!.readText(),
                io.ktor.http.ContentType.Text.Html
            )
        }

    }
}
