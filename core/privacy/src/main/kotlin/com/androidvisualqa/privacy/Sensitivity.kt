package com.androidvisualqa.privacy

/**
 * Classification of sensitive content for redaction decisions.
 *
 * Each value has a stable lowercase wire string suitable for serialization.
 */
enum class Sensitivity(val wire: String) {
    Public("public"),
    Pii("pii"),
    Credentials("credentials"),
    Financial("financial"),
    Health("health"),
    Other("other"),
}
