package com.stabledata.context

import io.grpc.*
import io.ktor.http.*

class GrpcContextInterceptor : ServerInterceptor {
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

        val context: Context = Context.current()
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