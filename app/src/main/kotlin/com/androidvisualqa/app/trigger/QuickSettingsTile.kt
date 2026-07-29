package com.androidvisualqa.app.trigger

import android.app.PendingIntent
import android.os.Build
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.androidvisualqa.app.AppServiceRegistry
import com.androidvisualqa.app.CaptureLaunchActivity

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
        val captureIntent = Intent(this, CaptureLaunchActivity::class.java)
            .putExtra(CaptureLaunchActivity.EXTRA_CAPTURE_REQUEST, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ rejects the Intent overload from a TileService.
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    1004,
                    captureIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        } else {
            startActivityAndCollapse(captureIntent)
        }
    }
}
