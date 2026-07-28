package com.androidvisualqa.capture.api

import java.util.UUID

/**
 * Opaque identifier for a persisted draft.
 *
 * ponytail: simple UUID wrapper; migrate to a value class in a later lane
 * if serialization boundaries require it.
 */
public value class DraftId(public val value: String) {
    public companion object {
        public fun random(): DraftId = DraftId(UUID.randomUUID().toString())
    }
}
