package com.stabledata.grpc

import com.stabledata.context.StableEventCreatedOnHeader
import com.stabledata.context.StableEventIdHeader
import com.stabledata.synchro.DataRequest
import com.stabledata.synchro.DataResponse
import com.stabledata.synchro.SynchroGrpcServiceGrpc
import io.grpc.*
import io.grpc.stub.StreamObserver
import io.ktor.http.*


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