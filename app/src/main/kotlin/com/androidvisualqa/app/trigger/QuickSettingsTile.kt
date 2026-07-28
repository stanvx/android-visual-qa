package com.androidvisualqa.app.trigger

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.androidvisualqa.app.CaptureForegroundService

/**
 * Quick Settings tile that launches [CaptureForegroundService] on tap.
 *
 * Lifecycle:
 * 1. User adds tile to Quick Settings → [onTileAdded]
 * 2. Tile visible → [onStartListening] — updates subtitle to "Ready"
 * 3. User taps → [onClick] — starts [CaptureForegroundService]
 * 4. Tile hidden → [onStopListening]
 */
public class QuickSettingsTile : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let { tile ->
            tile.subtitle = "Ready"
            tile.state = Tile.STATE_ACTIVE
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
