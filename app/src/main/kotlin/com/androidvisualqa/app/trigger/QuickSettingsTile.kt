package com.androidvisualqa.app.trigger

import android.service.quicksettings.TileService

/**
 * Quick Settings tile launcher for starting a visual feedback capture.
 *
 * // TODO(m2): implement onTileAdded()/onStartListening() to wire CaptureCommand.Trigger
 * // via the state machine. This stub is the manifest entry only.
 */
public class QuickSettingsTile : TileService() {
    // M2: dispatch CaptureCommand.Trigger(TriggerSource.QuickSettingsTile) on onStartListening()
}
