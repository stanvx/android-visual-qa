package com.androidvisualqa.privacy

/**
 * Pure in-memory store for user-drawn redaction regions.
 *
 * Thread-safe only for single-threaded use (UI-thread driven).
 * Thread-safety can be added in a future milestone if background operations arise.
 */
class ManualRedactionStore {

    private val regions = mutableListOf<RedactionRegion>()

    /**
     * Appends a user-drawn [region] to the store.
     */
    fun add(region: RedactionRegion) {
        regions.add(region)
    }

    /**
     * Removes the region at [index].
     * @throws IndexOutOfBoundsException if index is out of range.
     */
    fun remove(index: Int) {
        regions.removeAt(index)
    }

    /**
     * Returns an immutable snapshot of the current regions.
     */
    fun list(): List<RedactionRegion> = regions.toList()

    /**
     * Removes all regions.
     */
    fun clear() {
        regions.clear()
    }
}
