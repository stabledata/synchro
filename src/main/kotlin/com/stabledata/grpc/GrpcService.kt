package com.stabledata.grpc

import com.stabledata.Operations
import com.stabledata.context.*
import com.stabledata.dao.LogEntryBuilder
import com.stabledata.model.Collection
import com.stabledata.model.toMessage
import com.stabledata.synchro.DataRequest
import com.stabledata.synchro.DataResponse
import com.stabledata.synchro.SynchroGrpcServiceGrpc
import com.stabledata.workload.schema.createCollectionWorkload
import io.grpc.*
import io.grpc.stub.StreamObserver
import io.ktor.http.*
import stable.LogEntry.LogEntryMessage
import stable.Schema
import stable.SchemaServiceGrpc


internal class GrpcContextInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata?,
        next: ServerCallHandler<ReqT, RespT>?
    ): ServerCall.Listener<ReqT> {

        val tokenKey = Metadata.Key.of(HttpHeaders.Authorization, Metadata.ASCII_STRING_MARSHALLER)
        val eventIdKey = Metadata.Key.of(StableEventIdHeader, Metadata.ASCII_STRING_MARSHALLER)
        val eventCreatedKey = Metadata.Key.of(StableEventCreatedOnHeader, Metadata.ASCII_STRING_MARSHALLER)

        val token: String? = headers?.get(tokenKey)
        val eventId: String? = headers?.get(eventIdKey)
        val eventCreatedAt: Long? = headers?.get(eventCreatedKey)?.toLong()

        val context: Context = Context
            .current()
            .withValue(tokenContext, token)
            .withValue(eventIdContext, eventId)
            .withValue(eventCreatedAtContext, eventCreatedAt)

        return Contexts.interceptCall(context, call, headers, next)
    }

    companion object {
        val tokenContext: Context.Key<Any> = Context.key(HttpHeaders.Authorization)
        val eventIdContext: Context.Key<Any> = Context.key(StableEventIdHeader)
        val eventCreatedAtContext: Context.Key<Any> = Context.key(StableEventCreatedOnHeader)
    }
}

class GrpcService : SynchroGrpcServiceGrpc.SynchroGrpcServiceImplBase() {
    override fun getData(request: DataRequest, responseObserver: StreamObserver<DataResponse>) {
        val token = GrpcContextInterceptor.tokenContext.get(Context.current())
        val response = DataResponse.newBuilder()
            .setData("Data for id: ${request.id} w token in context: $token")
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}

class SchemaService : SchemaServiceGrpc.SchemaServiceImplBase() {
    override fun createCollection(
        request: Schema.CollectionRequest,
        responseObserver: StreamObserver<LogEntryMessage>
    ) {

        val token = GrpcContextInterceptor.tokenContext.get(Context.current()).toString()
        val userCredentials = credentialsCanPerformOperation(
            UserCredentials.fromRawToken(token),
            Operations.Schema.CREATE_COLLECTION
        )
        val collection = Collection.fromMessage(request)
        val envelope = Envelope(
            GrpcContextInterceptor.eventIdContext.get(Context.current()).toString(),
            GrpcContextInterceptor.eventCreatedAtContext.get(Context.current()).toString().toLongOrNull() ?: System.currentTimeMillis()
        )

        val logEntry = LogEntryBuilder()
            .eventType(Operations.Schema.CREATE_COLLECTION)
            .actorId(userCredentials.id)
            .teamId(userCredentials.team)
            .id(envelope.eventId)
            .createdAt(envelope.createdAt)

        val ctx = WriteRequestContext(
            collection,
            userCredentials = userCredentials,
            envelope = envelope,
            logEntry = logEntry
        )

        val result = createCollectionWorkload(ctx)
        val response = result.toMessage()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}