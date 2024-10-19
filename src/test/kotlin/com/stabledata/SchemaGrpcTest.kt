package com.stabledata

import com.stabledata.context.StableEventIdHeader
import com.stabledata.grpc.SchemaService
import io.grpc.Metadata
import io.kotest.core.spec.style.WordSpec
import io.ktor.http.*
import stable.Schema
import stable.SchemaServiceGrpc

class SchemaGrpcTest:WordSpec({
    "returns unauthorized" should {
        grpcTest(
            serviceImpl = SchemaService(),
            stubCreator = { channel -> SchemaServiceGrpc.newBlockingStub(channel) }
        ) { stub ->

            val metadata = Metadata().apply {
                put(Metadata.Key.of(HttpHeaders.Authorization, Metadata.ASCII_STRING_MARSHALLER), "Bearer some-token")
                put(Metadata.Key.of(StableEventIdHeader, Metadata.ASCII_STRING_MARSHALLER), "12345")
            }

            // Create a call options object to pass the metadata interceptor
            val callOptions = stub.withInterceptors(MetadataInterceptor(metadata))

            val request = Schema.CollectionRequest.newBuilder()
                .setId("collection-123")
                .setPath("/example/path")
                .build()

            val response = callOptions.createCollection(request)
            assert(response != null)
        }

    }
})