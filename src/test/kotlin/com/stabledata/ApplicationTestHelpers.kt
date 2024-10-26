package com.stabledata

import com.stabledata.context.StableEventIdHeader
import com.stabledata.context.GrpcContextInterceptor
import io.grpc.*
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.AbstractStub
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

fun <T : AbstractStub<T>> grpcTest(
    serviceImpl: BindableService,
    stubCreator: (ManagedChannel) -> T,
    token: String,
    eventId: String = uuidString(),
    test: (stub: T) -> Unit
) {
    val serverName = InProcessServerBuilder.generateName()

    val server = InProcessServerBuilder
        .forName(serverName)
        .directExecutor()
        .intercept(ExceptionHandlingInterceptor())
        .intercept(GrpcContextInterceptor())
        .addService(serviceImpl)
        .build()
        .start()

    val channel = InProcessChannelBuilder
        .forName(serverName)
        .directExecutor()
        .build()

    try {
        val metadata = Metadata().apply {
            put(
                Metadata.Key.of(HttpHeaders.Authorization, Metadata.ASCII_STRING_MARSHALLER),
                "Bearer $token"
            )
            put(
                Metadata.Key.of(StableEventIdHeader, Metadata.ASCII_STRING_MARSHALLER),
                eventId
            )
        }

        val callOptionsStub = stubCreator(channel)
            .withInterceptors(MetadataInterceptor(metadata))

        test(callOptionsStub)
    } finally {
        channel.shutdown()
        server.shutdown()
    }
}