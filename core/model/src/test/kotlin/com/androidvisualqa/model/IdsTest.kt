package com.androidvisualqa.model

import com.androidvisualqa.model.ids.AttachmentId
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.model.ids.NodeId
import com.androidvisualqa.model.ids.ReportId
import com.androidvisualqa.model.ids.SdkComponentId
import com.androidvisualqa.model.serialization.JsonConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IdsTest {

    @Serializable
    data class IdContainer(
        val reportId: ReportId,
        val draftId: DraftId,
        val nodeId: NodeId,
        val sdkComponentId: SdkComponentId,
        val attachmentId: AttachmentId,
    )

    @Test
    fun `value class IDs serialize as plain strings not objects`() {
        val container = IdContainer(
            reportId = ReportId("r-001"),
            draftId = DraftId("d-001"),
            nodeId = NodeId("n-001"),
            sdkComponentId = SdkComponentId("s-001"),
            attachmentId = AttachmentId("a-001"),
        )

        val json = JsonConfig.encodeToString(container)

        // Verify each ID is a string value, not an object with "value" key
        assertEquals(
            """{"reportId":"r-001","draftId":"d-001","nodeId":"n-001","sdkComponentId":"s-001","attachmentId":"a-001"}""",
            json,
        )
    }

    @Test
    fun `value class IDs deserialize from plain strings`() {
        val json = """{"reportId":"r-002","draftId":"d-002","nodeId":"n-002","sdkComponentId":"s-002","attachmentId":"a-002"}"""

        val container = JsonConfig.decodeFromString<IdContainer>(json)

        assertEquals(ReportId("r-002"), container.reportId)
        assertEquals(DraftId("d-002"), container.draftId)
        assertEquals(NodeId("n-002"), container.nodeId)
        assertEquals(SdkComponentId("s-002"), container.sdkComponentId)
        assertEquals(AttachmentId("a-002"), container.attachmentId)
    }
}
