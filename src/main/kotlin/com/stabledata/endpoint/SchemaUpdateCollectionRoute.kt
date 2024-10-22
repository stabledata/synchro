package com.stabledata.endpoint

import com.stabledata.Operations
import com.stabledata.context.JWT_NAME
import com.stabledata.context.contextualizeHTTPWriteRequest
import com.stabledata.model.Collection
import com.stabledata.workload.schemaUpdateCollectionWorkload
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.configureUpdateCollectionRoute() {
    routing {
        authenticate(JWT_NAME) {
            post("schema/collection/update") {
                val ctx = contextualizeHTTPWriteRequest(
                    operation = Operations.Schema.UPDATE_COLLECTION,
                    jsonSchema = Operations.Schema.UPDATE_COLLECTION
                ) { postData ->
                    Collection.fromJSON(postData)
                }

                val logEntry = schemaUpdateCollectionWorkload(ctx)

                return@post call.respond(
                    HttpStatusCode.OK,
                    logEntry
                )
            }
        }
    }
}
