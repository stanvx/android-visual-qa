package com.androidvisualqa.sdk.composecore

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe registry of SDK component descriptors.
 *
 * Real persistence (e.g. across process death) belongs in [com.androidvisualqa.app]
 * or a future database-backed module. This in-memory implementation is suitable
 * for a single capture session.
 */
interface SdkRegistry {
    /** Register a component descriptor. Returns `false` if [SdkComponentDescriptor.stableId] already exists. */
    fun register(descriptor: SdkComponentDescriptor): Boolean

    /** Unregister a component by its [stableId]. Returns `false` if no entry existed. */
    fun unregister(stableId: String): Boolean

    /** Retrieve a registered component by its [stableId], or `null`. */
    fun get(stableId: String): SdkComponentDescriptor?

    /** Return all currently registered components. */
    fun all(): List<SdkComponentDescriptor>
}

/**
 * In-memory implementation of [SdkRegistry].
 *
 * Thread-safe via a single [Mutex]. Not persisted — register again after process death.
 */
class InMemorySdkRegistry : SdkRegistry {

    private val mutex = Mutex()
    private val store = mutableMapOf<String, SdkComponentDescriptor>()

    override fun register(descriptor: SdkComponentDescriptor): Boolean = runBlocking {
        mutex.withLock {
            if (descriptor.stableId in store) return@withLock false
            store[descriptor.stableId] = descriptor
            true
        }
    }

    override fun unregister(stableId: String): Boolean = runBlocking {
        mutex.withLock {
            if (stableId !in store) return@withLock false
            store.remove(stableId)
            true
        }
    }

    override fun get(stableId: String): SdkComponentDescriptor? = runBlocking {
        mutex.withLock {
            store[stableId]
        }
    }

    override fun all(): List<SdkComponentDescriptor> = runBlocking {
        mutex.withLock {
            store.values.toList()
        }
    }
}

// ponytail: blocking bridge for non-suspendable SdkRegistry interface.
// If all callers become suspendable in a future iteration, remove this shim.
private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}
