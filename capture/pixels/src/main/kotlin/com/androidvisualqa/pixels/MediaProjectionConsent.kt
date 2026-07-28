package com.androidvisualqa.pixels

/**
 * Interface for requesting the user's consent to use MediaProjection.
 *
 * The real consent dialog implementation lives in **M1 lane H-fix** (Android adapter).
 * This interface exists so that [PixelCaptureRouter] and consumers can be wired
 * against a pure-Kotlin contract.
 */
public interface MediaProjectionConsent {
    /** Request the user's consent. Returns [Granted] or [Denied]. */
    public suspend fun requestConsent(): ConsentOutcome
}

/**
 * Outcome of a MediaProjection consent request.
 */
public sealed interface ConsentOutcome {
    /** The user granted consent. */
    public data object Granted : ConsentOutcome

    /** The user denied or dismissed the consent dialog. */
    public data object Denied : ConsentOutcome
}
