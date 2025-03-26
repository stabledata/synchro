package com.stabledata

import com.stabledata.context.generateTokenForTesting
import com.stabledata.grpc.SchemaService
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.Database
import stable.Schema
import stable.SchemaServiceGrpc
import kotlin.test.fail

class SchemaGrpcTest:WordSpec({
    "schema grpc endpoints" should {

        Database.connect(hikari())

        "return unauthorized message without invalid token" {
            grpcTest(
                serviceImpl = SchemaService(),
                stubCreator = { channel -> SchemaServiceGrpc.newBlockingStub(channel) },
                "bad token",

            ) { stub ->
                val request = Schema.CollectionRequest.newBuilder()
                    .setId("collection-123")
                    .setPath("/example/path")
                    .build()

                try {
                    stub.createCollection(request)
                    fail("Expected UNAUTHENTICATED exception")
                } catch (e: StatusRuntimeException) {
                    e.status.code shouldBe Status.UNAUTHENTICATED.code
                }
            }
        }

        "return bad request with invalid collection" {
            grpcTest(
                serviceImpl = SchemaService(),
                stubCreator = { channel -> SchemaServiceGrpc.newBlockingStub(channel) },
                generateTokenForTesting("admin"),

                ) { stub ->
                val request = Schema.CollectionRequest.newBuilder()
                    // .setId("collection-123") missing
                    .setPath("/example/path")
                    .build()

                try {
                    stub.createCollection(request)
                    fail("Expected INVALID_ARGUMENT exception")
                } catch (e: StatusRuntimeException) {
                    e.status.code shouldBe Status.INVALID_ARGUMENT.code
                }

                val requestWithoutPath = Schema.CollectionRequest.newBuilder()
                    .setId("collection-123")
                    // .setPath("/example/path")
                    .build()

                try {
                    stub.createCollection(requestWithoutPath)
                    fail("Expected INVALID_ARGUMENT exception")
                } catch (e: StatusRuntimeException) {
                    e.status.code shouldBe Status.INVALID_ARGUMENT.code
                }
            }
        }

        "return unauthorized for non admin token" {
            grpcTest(
                serviceImpl = SchemaService(),
                stubCreator = { channel -> SchemaServiceGrpc.newBlockingStub(channel) },
                generateTokenForTesting(),

                ) { stub ->
                val request = Schema.CollectionRequest.newBuilder()
                    .setId("collection-123")
                    .setPath("/example/path")
                    .build()

                try {
                    stub.createCollection(request)
                    fail("Expected UNAUTHENTICATED exception")
                } catch (e: StatusRuntimeException) {
                    e.status.code shouldBe Status.UNAUTHENTICATED.code
                }
            }
        }
    }
})