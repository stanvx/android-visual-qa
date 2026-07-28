package com.androidvisualqa.files

import java.security.MessageDigest

/**
 * SHA-256 hashing utilities.
 *
 * Uses only stdlib [java.security.MessageDigest]. No third-party dependencies.
 */
public object Hashing {

    private const val HEX_CHARS: String = "0123456789abcdef"

    /**
     * Computes the lower-case hex-encoded SHA-256 digest of [bytes].
     *
     * Example:
     * ```
     * sha256("abc".toByteArray()) // "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
     * ```
     */
    public fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        val hex = StringBuilder(hash.size * 2)
        for (b in hash) {
            hex.append(HEX_CHARS[(b.toInt() ushr 4) and 0xF])
            hex.append(HEX_CHARS[b.toInt() and 0xF])
        }
        return hex.toString()
    }
}
