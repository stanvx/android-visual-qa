package com.androidvisualqa.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Transparent entry point used by S Pen Air Command and the capture tile. */
public class CaptureLaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForegroundService(
            Intent(this, CaptureForegroundService::class.java).putExtra(
                CaptureForegroundService.EXTRA_AUTO_OPEN_EDITOR,
                true,
            ),
        )
    }
}
