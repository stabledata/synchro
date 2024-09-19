import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import java.util.Optional

@Serializable
data class HeaderEnvelope (
    val stableEventId: String?,
    val stableEventCreatedAt: String?
)


val EnvelopeKey = AttributeKey<HeaderEnvelope>("x-stable")
val HeaderEnvelopeParser = createApplicationPlugin(name = "HeaderEnvelopePlugin") {
    onCall { call ->
        call.request.origin.apply {
            // extract the event id from the header
            val eventId = call.request.headers["x-stable-event-id"]
            val createdAt = call.request.headers["x-stable-event-created"]
            call.attributes.put(EnvelopeKey, HeaderEnvelope(
                stableEventId = eventId,
                stableEventCreatedAt = createdAt
            ))

        }
    }
}