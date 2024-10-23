package com.stabledata.endpoint

import com.stabledata.Operations
import com.stabledata.context.JWT_NAME
import com.stabledata.context.contextualizeHTTPWriteRequest
import com.stabledata.model.Collection
import com.stabledata.workload.schema.createCollectionWorkload
import com.stabledata.workload.schema.deleteCollectionWorkload
import com.stabledata.workload.schema.updateCollectionWorkload
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureSchemaRoutes() {
    routing {
        authenticate(JWT_NAME) {
            post("schema/collection/create") {
               val writeContextFromHTTP = contextualizeHTTPWriteRequest(
                    operation = Operations.Schema.CREATE_COLLECTION,
                    jsonSchema = Operations.Schema.CREATE_COLLECTION
                ) { postData ->
                    Collection.fromJSON(postData)
                }

                val logEntry = createCollectionWorkload(writeContextFromHTTP)
                return@post call.respond(
                    HttpStatusCode.Created,
                    logEntry
                )
            }
        }
    }

    routing {
        authenticate(JWT_NAME) {
            post("schema/collection/delete") {
                val ctx = contextualizeHTTPWriteRequest(
                    operation = Operations.Schema.DELETE_COLLECTION,
                    jsonSchema = Operations.Schema.DELETE_COLLECTION
                ) { postData ->
                    Collection.fromJSON(postData)
                }

                val logEntry = deleteCollectionWorkload(ctx)
                return@post call.respond(
                    HttpStatusCode.OK,
                    logEntry
                )
            }
        }
    }

    routing {
        authenticate(JWT_NAME) {
            post("schema/collection/update") {
                val ctx = contextualizeHTTPWriteRequest(
                    operation = Operations.Schema.UPDATE_COLLECTION,
                    jsonSchema = Operations.Schema.UPDATE_COLLECTION
                ) { postData ->
                    Collection.fromJSON(postData)
                }

                val logEntry = updateCollectionWorkload(ctx)
                return@post call.respond(
                    HttpStatusCode.OK,
                    logEntry
                )
            }
        }
    }
}
