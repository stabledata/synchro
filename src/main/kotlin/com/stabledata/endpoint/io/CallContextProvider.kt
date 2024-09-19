package com.stabledata.endpoint.io

import EnvelopeKey
import HeaderEnvelope
import com.fasterxml.uuid.Generators
import com.stabledata.plugins.MissingCredentialsException
import com.stabledata.plugins.UserCredentials
import com.stabledata.validateStringAgainstJSONSchema
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import java.util.*

data class CallContext<T> (
    val request: T,
    val envelope: HeaderEnvelope,
    val credentials: UserCredentials,
    val validation: Pair<Boolean, List<String>>,
    val stableEventId: UUID
)
suspend fun <T> callContextProvider(
    call: ApplicationCall,
    bodyValidationSchemaLocation: String?,
    bodyRequestMapper: (body: String) -> T
): CallContext<T> {

    val body = call.receiveText()
    val envelope = call.attributes[EnvelopeKey]
    val credentials = call.principal<UserCredentials>()
        ?: throw MissingCredentialsException()

    val (isValid, errors) = if (bodyValidationSchemaLocation.isNullOrEmpty()) {
        Pair(true, emptyList())
    } else {
        validateStringAgainstJSONSchema(
            bodyValidationSchemaLocation,
            body
        )
    }

    return CallContext(
        request = bodyRequestMapper(body),
        envelope = envelope,
        credentials = credentials,
        validation = Pair(isValid, errors),
        stableEventId = if (envelope.stableEventId?.isNotBlank() == true) {
            UUID.fromString(envelope.stableEventId)
        } else {
            Generators.timeBasedEpochGenerator().generate()
        }
    )
}