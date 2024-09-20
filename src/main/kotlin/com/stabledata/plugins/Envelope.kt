import com.fasterxml.uuid.Generators
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.util.*

data class Envelope(
    val eventId: String,
    val createdAt: Long
)

val EnvelopeKey = AttributeKey<Envelope>("x-stable")
val HeaderEnvelopeParser = createApplicationPlugin(name = "HeaderEnvelopePlugin") {
    onCall { call ->
        call.request.origin.apply {
            // extract the event id from the header
            val eventId = call.request.headers["x-stable-event-id"]
            val createdAt = call.request.headers["x-stable-event-created"]
            call.attributes.put(EnvelopeKey, Envelope(
                eventId = eventId ?: Generators.timeBasedEpochGenerator().generate().toString(),
                createdAt = createdAt?.toLong() ?: System.currentTimeMillis()
            ))

        }
    }
}