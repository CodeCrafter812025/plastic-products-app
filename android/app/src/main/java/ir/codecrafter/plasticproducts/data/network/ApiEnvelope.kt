package ir.codecrafter.plasticproducts.data.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Mirrors backend/core/renderers.py CustomJSONRenderer: every response is either
 * {success, data, message, timestamp} or {success, error: {code, message}, timestamp}.
 */
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: ApiError? = null,
    val timestamp: String? = null,
)

@Serializable
data class ApiError(
    val code: String,
    val message: ErrorMessage? = null,
)

@Serializable(with = ErrorMessageSerializer::class)
sealed class ErrorMessage {
    data class StringMessage(val value: String) : ErrorMessage()
    data class FieldErrors(val fields: Map<String, List<String>>) : ErrorMessage()
}

object ErrorMessageSerializer : KSerializer<ErrorMessage> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ErrorMessage")

    override fun serialize(encoder: Encoder, value: ErrorMessage) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("ErrorMessage can only be serialized with kotlinx.serialization.json.Json")
        val element: JsonElement = when (value) {
            is ErrorMessage.StringMessage -> JsonPrimitive(value.value)
            is ErrorMessage.FieldErrors -> JsonObject(
                value.fields.mapValues { (_, messages) ->
                    JsonArray(messages.map { JsonPrimitive(it) })
                }
            )
        }
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): ErrorMessage {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("ErrorMessage can only be deserialized with kotlinx.serialization.json.Json")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonObject -> ErrorMessage.FieldErrors(
                element.mapValues { (_, fieldValue) ->
                    fieldValue.jsonArray.map { it.jsonPrimitive.content }
                }
            )
            else -> ErrorMessage.StringMessage(element.jsonPrimitive.content)
        }
    }
}
