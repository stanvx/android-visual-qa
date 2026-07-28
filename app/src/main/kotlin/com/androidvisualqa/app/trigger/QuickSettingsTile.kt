package com.androidvisualqa.app.trigger

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.androidvisualqa.app.AppServiceRegistry
import com.androidvisualqa.app.CaptureForegroundService

/**
 * Quick Settings tile that launches [CaptureForegroundService] on tap.
 *
 * Lifecycle:
 * 1. User adds tile to Quick Settings → [onTileAdded]
 * 2. Tile visible → [onStartListening] — updates subtitle to "Ready"
 * 3. User taps → [onClick] — starts [CaptureForegroundService]
 * 4. Tile hidden → [onStopListening]
 *
 * The tile and the bridge accessibility service run in the same process
 * (`android:process=":a11y"`), so the [AppServiceRegistry] sees the
 * connected bridge service if the user has enabled it in Settings.
 */
public class QuickSettingsTile : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let { tile ->
            val ready = AppServiceRegistry.accessibilityService != null
            tile.subtitle = if (ready) "Ready" else "Enable a11y"
            tile.state = if (ready) Tile.STATE_ACTIVE else Tile.STATE_UNAVAILABLE
            tile.updateTile()
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        qsTile?.let { tile ->
            tile.subtitle = ""
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, CaptureForegroundService::class.java)
        startForegroundService(intent)
    }
}
