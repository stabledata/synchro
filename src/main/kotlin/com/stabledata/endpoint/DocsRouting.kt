package com.stabledata.context

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.openapi.*


fun Application.configureDocsRouting() {
    routing {

        openAPI(path="api", swaggerFile = "openapi/doc.yaml")

        staticResources("/openapi", basePackage = "openapi")

        get("/docs") {
            call.respondText(
                this::class.java.classLoader.getResource("openapi/redoc.html")!!.readText(),
                io.ktor.http.ContentType.Text.Html
            )
        }

    }
}
