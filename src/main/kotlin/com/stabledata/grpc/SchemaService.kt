package com.stabledata.grpc

import com.stabledata.context.WriteRequestContext
import com.stabledata.model.Collection
import com.stabledata.model.toMessage
import com.stabledata.workload.schema.createCollectionWorkload
import io.grpc.stub.StreamObserver
import stable.LogEntry.LogEntryMessage
import stable.Schema
import stable.SchemaServiceGrpc


class SchemaService : SchemaServiceGrpc.SchemaServiceImplBase() {
    override fun createCollection(
        request: Schema.CollectionRequest,
        responseObserver: StreamObserver<LogEntryMessage>
    ) {
        val ctx = WriteRequestContext.fromGrpcContext {
            Collection.fromMessage(request)
        }
        val result = createCollectionWorkload(ctx)
        val response = result.toMessage()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}