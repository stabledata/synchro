package com.stabledata.endpoint

import com.stabledata.Operations
import com.stabledata.context.JWT_NAME
import com.stabledata.context.contextualizeHTTPWriteRequest
import com.stabledata.model.Collection
import com.stabledata.workload.schemaCreateCollectionWorkload
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureCreateCollectionRoute() {

    routing {
        authenticate(JWT_NAME) {
            post("schema/collection/create") {
               val writeContextFromHTTP = contextualizeHTTPWriteRequest(
                    operation = Operations.Schema.CREATE_COLLECTION,
                    jsonSchema = Operations.Schema.CREATE_COLLECTION
                ) { postData ->
                    Collection.fromJSON(postData)
                }

                val logEntry = schemaCreateCollectionWorkload(writeContextFromHTTP)

                return@post call.respond(
                    HttpStatusCode.Created,
                    logEntry
                )
            }
        }
    }
}
