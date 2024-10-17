package com.stabledata

import com.stabledata.context.StableEventIdHeader
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*


fun testWithDefaultAppModule(
    block: suspend ApplicationTestBuilder.() -> Unit) {
    testApplication {
        application {
            module()
        }
        block()
    }
}

suspend fun ApplicationTestBuilder.postJson(
    uri: String,
    token: String,
    eventId: String = uuidString(),
    body: () -> String
): HttpResponse {
    return client.post(uri) {
        headers {
            append(HttpHeaders.Authorization, "Bearer $token")
            append(StableEventIdHeader, eventId)
        }
        contentType(ContentType.Application.Json)
        setBody(body())
    }
}