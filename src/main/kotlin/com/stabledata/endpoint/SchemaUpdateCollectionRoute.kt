package com.stabledata.endpoint

import com.stabledata.model.Collection
import com.stabledata.plugins.JWT_NAME
import com.stabledata.workload.schemaUpdateCollectionWorkload
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.configureUpdateCollectionRoute() {
    val logger = KotlinLogging.logger {}
    routing {
        authenticate(JWT_NAME) {
            post("schema/collection/update") {
                val ctx = contextualizeHTTPWriteRequest(
                    "collection/update",
                    "collection/update"
                ) { postData ->
                    Collection.fromJSON(postData)
                } ?: return@post

                val logEntry = schemaUpdateCollectionWorkload(ctx)

                return@post call.respond(
                    HttpStatusCode.OK,
                    logEntry
                )
            }
        }
    }
}
