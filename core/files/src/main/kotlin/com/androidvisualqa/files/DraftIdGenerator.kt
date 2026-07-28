package com.androidvisualqa.files

import com.androidvisualqa.model.ids.DraftId
import java.util.UUID

/**
 * Generates unique [DraftId] values using [UUID.randomUUID].
 */
public object DraftIdGenerator {
    public fun new(): DraftId = DraftId("draft-${UUID.randomUUID()}")
}
