package com.androidvisualqa.matching

import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import com.androidvisualqa.geometry.Polygon
import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.capture.SdkComponentSnapshot
import com.androidvisualqa.model.ids.NodeId

/**
 * Input to the matching engine.
 *
 * @property selectionPolygon The lasso / freehand polygon drawn by the user.
 * @property screenBounds The full screen bounds (used for root/large-container penalty).
 * @property candidates All candidate [NodeSnapshot]s from the accessibility tree.
 * @property sdkOverrides First-party SDK component snapshots (optional; from M4).
 * @property recentEventNodeIds Set of node IDs that received recent focus/click events
 *   (ring buffer from the accessibility service).
 * @property activeWindowZOrder Topmost-first window ordering for z-order bonus.
 */
data class MatchingInput(
    val selectionPolygon: Polygon<CoordinateSpace.ScreenPx>,
    val screenBounds: Bounds<CoordinateSpace.ScreenPx>,
    val candidates: List<NodeSnapshot>,
    val sdkOverrides: List<SdkComponentSnapshot> = emptyList(),
    val recentEventNodeIds: Set<NodeId> = emptySet(),
    val activeWindowZOrder: List<NodeId> = emptyList(),
)
