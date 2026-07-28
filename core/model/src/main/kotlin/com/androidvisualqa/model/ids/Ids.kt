package com.androidvisualqa.model.ids

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * UUID-based value-class ID wrappers.
 *
 * Each serializes as a plain JSON string, not a JSON object.
 */

@JvmInline
@Serializable(with = ReportIdSerializer::class)
value class ReportId(val value: String)

@JvmInline
@Serializable(with = DraftIdSerializer::class)
value class DraftId(val value: String)

@JvmInline
@Serializable(with = NodeIdSerializer::class)
value class NodeId(val value: String)

@JvmInline
@Serializable(with = SdkComponentIdSerializer::class)
value class SdkComponentId(val value: String)

@JvmInline
@Serializable(with = AttachmentIdSerializer::class)
value class AttachmentId(val value: String)

// --- Serializers ---

object ReportIdSerializer : KSerializer<ReportId> {
    override val descriptor = PrimitiveSerialDescriptor("com.androidvisualqa.model.ReportId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ReportId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): ReportId = ReportId(decoder.decodeString())
}

object DraftIdSerializer : KSerializer<DraftId> {
    override val descriptor = PrimitiveSerialDescriptor("com.androidvisualqa.model.DraftId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: DraftId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): DraftId = DraftId(decoder.decodeString())
}

object NodeIdSerializer : KSerializer<NodeId> {
    override val descriptor = PrimitiveSerialDescriptor("com.androidvisualqa.model.NodeId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: NodeId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): NodeId = NodeId(decoder.decodeString())
}

object SdkComponentIdSerializer : KSerializer<SdkComponentId> {
    override val descriptor = PrimitiveSerialDescriptor("com.androidvisualqa.model.SdkComponentId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: SdkComponentId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): SdkComponentId = SdkComponentId(decoder.decodeString())
}

object AttachmentIdSerializer : KSerializer<AttachmentId> {
    override val descriptor = PrimitiveSerialDescriptor("com.androidvisualqa.model.AttachmentId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: AttachmentId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): AttachmentId = AttachmentId(decoder.decodeString())
}
