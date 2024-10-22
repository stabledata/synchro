package com.stabledata.endpoint

import com.stabledata.Operations
import com.stabledata.context.JWT_NAME
import com.stabledata.context.contextualizeHTTPWriteRequest
import com.stabledata.model.Collection
import com.stabledata.workload.schemaDeleteCollectionWorkload
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.configureDeleteCollectionRoute() {
    routing {
        authenticate(JWT_NAME) {
            post("schema/collection/delete") {
                val ctx = contextualizeHTTPWriteRequest(
                    operation = Operations.Schema.DELETE_COLLECTION,
                    jsonSchema = Operations.Schema.DELETE_COLLECTION
                ) { postData ->
                    Collection.fromJSON(postData)
                }

                val logEntry = schemaDeleteCollectionWorkload(ctx)

                return@post call.respond(
                    HttpStatusCode.OK,
                    logEntry
                )
            }
        }
    }
}
