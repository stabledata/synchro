package com.stabledata

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

class UnauthorizedException(message: String) : Exception(message)
class PermissionDenied(message: String) : Exception(message)
class JSONSchemaValidationException(message: String) : Exception(message)
class EventAlreadyProcessedException(message: String) : Exception(message)
class NotFoundException(message: String) : Exception(message)
class SQLConflictException(message: String) : Exception(message)

data class ErrorStatus (
    val http: HttpStatusCode,
    val grpc: Status,
)

fun logAndReturnMappedError(err: Throwable): Pair<ErrorStatus, String> {
    val logger = KotlinLogging.logger {}
    val ref = uuidString()
    logger.error { "${err.localizedMessage} ref: $ref" }
    return when (err) {
        is UnauthorizedException -> Pair(
            ErrorStatus(HttpStatusCode.Unauthorized, Status.UNAUTHENTICATED),
            err.localizedMessage
        )
        is PermissionDenied -> Pair(
            ErrorStatus(HttpStatusCode.Forbidden, Status.UNAUTHENTICATED),
            err.localizedMessage
        )
        is JSONSchemaValidationException -> Pair(
            ErrorStatus(HttpStatusCode.BadRequest, Status.INVALID_ARGUMENT),
            err.localizedMessage
        )
        is SQLConflictException,
        is EventAlreadyProcessedException -> Pair(
            ErrorStatus(HttpStatusCode.Conflict, Status.ALREADY_EXISTS),
            err.localizedMessage
        )
        is NotFoundException -> Pair(
            ErrorStatus(HttpStatusCode.NotFound, Status.NOT_FOUND),
            "Resource not found: $ref"
        )
        else -> Pair(
            ErrorStatus(HttpStatusCode.InternalServerError, Status.INTERNAL),
            "Synchro failed. Ref: $ref"
        )
    }
}

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<Throwable> { call, err ->
            val (mappedError, message) = logAndReturnMappedError(err)
            call.respond(mappedError.http, message)
        }
    }
}

class ExceptionHandlingInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata?,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        return object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
            next.startCall(call, headers)
        ) {
            override fun onHalfClose() {
                try {
                    super.onHalfClose()
                } catch (e: Exception) {
                    handleException(call, e)
                }
            }
        }
    }

    private fun <ReqT, RespT> handleException(call: ServerCall<ReqT, RespT>, e: Exception) {
        val (status, message) = logAndReturnMappedError(e)
        val trailers = Metadata()
        trailers.put(Metadata.Key.of("error", Metadata.ASCII_STRING_MARSHALLER), message)
        call.close(status.grpc, trailers)
    }
}