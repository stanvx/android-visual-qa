package com.androidvisualqa.privacy

/**
 * Whether the captured window had [FLAG_SECURE][android.view.WindowManager.LayoutParams.FLAG_SECURE].
 */
enum class SecureWindowResult {
    Secure,
    NotSecure,
    Unknown,
}
