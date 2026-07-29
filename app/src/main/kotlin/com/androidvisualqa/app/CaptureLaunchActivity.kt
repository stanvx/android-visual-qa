package com.androidvisualqa.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Transparent entry point used by S Pen Air Command and the capture tile. */
public class CaptureLaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_CAPTURE_REQUEST, false)) {
            startForegroundService(
                Intent(this, CaptureForegroundService::class.java).putExtra(
                    CaptureForegroundService.EXTRA_AUTO_OPEN_EDITOR,
                    true,
                ),
            )
        } else {
            // Android Studio may retain this activity in a run configuration from M2.
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }

    internal companion object {
        internal const val EXTRA_CAPTURE_REQUEST: String = "captureRequest"
    }
}
