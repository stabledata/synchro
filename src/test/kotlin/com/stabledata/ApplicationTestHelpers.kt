package com.stabledata

import com.stabledata.context.StableEventIdHeader
import com.stabledata.grpc.GrpcContextInterceptor
import io.grpc.*
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
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

class MetadataInterceptor(private val metadata: Metadata) : ClientInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)
        ) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                // Add the metadata to the call
                headers.merge(metadata)
                super.start(responseListener, headers)
            }
        }
    }
}

fun <T : Any> grpcTest(
    serviceImpl: BindableService,  // The actual gRPC service implementation
    stubCreator: (ManagedChannel) -> T,  // Function to create the service stub
    test: (stub: T) -> Unit  // The test function, receiving the stub
) {
    val serverName = InProcessServerBuilder.generateName()

    val server = InProcessServerBuilder
        .forName(serverName)
        .directExecutor()
        .intercept(GrpcContextInterceptor())
        .addService(serviceImpl)
        .build()
        .start()

    val channel = InProcessChannelBuilder
        .forName(serverName)
        .directExecutor()
        .build()

    try {
        val stub = stubCreator(channel)
        test(stub)
    } finally {
        channel.shutdown()
        server.shutdown()
    }
}