package com.androidvisualqa.model.capture

import com.androidvisualqa.model.ids.NodeId
import kotlinx.serialization.Serializable

/**
 * Immutable flattened accessibility node snapshot.
 *
 * Contains only the fields needed for matching and evidence.
 * Text fields are truncated to [MAX_TEXT_LENGTH] and have spans removed.
 *
 * @property nodeId Stable snapshot-local identifier.
 * @property parentId ID of the parent node (null if root).
 * @property childIds Ordered child node IDs.
 * @property windowId Accessibility window ID.
 * @property zOrder Stacking order within the window.
 * @property boundsLeft Screen-space bounds (pixels).
 * @property boundsTop Screen-space bounds (pixels).
 * @property boundsRight Screen-space bounds (pixels).
 * @property boundsBottom Screen-space bounds (pixels).
 * @property text Visible text content.
 * @property contentDescription Accessibility content description.
 * @property stateDescription Accessibility state description.
 * @property hint Accessibility hint / placeholder.
 * @property paneTitle Accessibility pane title.
 * @property viewIdRaw Raw Android view resource ID (as string).
 * @property className Fully qualified class name.
 * @property role Semantic role (e.g. "Button", "EditText").
 * @property isEnabled Whether the node is interactive.
 * @property isSelected Whether the node is selected.
 * @property isChecked Whether the node is checked.
 * @property isClickable Whether the node accepts click actions.
 * @property isFocusable Whether the node can receive focus.
 * @property isEditable Whether the node accepts text input.
 * @property isScrollable Whether the node scrolls.
 * @property isPassword Whether the node is a password field.
 * @property isVisibleToUser Whether the node is visible.
 * @property supportedActions Bitmask or list of supported accessibility actions.
 * @property collectionInfo If the node is a collection (list/grid), its metadata.
 * @property collectionItemInfo If the node is a collection item, its metadata.
 * @property privacyClassification Cached privacy classification.
 * @property traversalDepth Depth in the accessibility tree.
 */
@Serializable
data class NodeSnapshot(
    val nodeId: NodeId,
    val parentId: NodeId? = null,
    val childIds: List<NodeId> = emptyList(),
    val windowId: Int = 0,
    val zOrder: Int = 0,
    val boundsLeft: Int = 0,
    val boundsTop: Int = 0,
    val boundsRight: Int = 0,
    val boundsBottom: Int = 0,
    val text: String? = null,
    val contentDescription: String? = null,
    val stateDescription: String? = null,
    val hint: String? = null,
    val paneTitle: String? = null,
    val viewIdRaw: String? = null,
    val className: String? = null,
    val role: String? = null,
    val isEnabled: Boolean = true,
    val isSelected: Boolean = false,
    val isChecked: Boolean = false,
    val isClickable: Boolean = false,
    val isFocusable: Boolean = false,
    val isEditable: Boolean = false,
    val isScrollable: Boolean = false,
    val isPassword: Boolean = false,
    val isVisibleToUser: Boolean = true,
    val supportedActions: List<String> = emptyList(),
    val collectionInfo: CollectionInfo? = null,
    val collectionItemInfo: CollectionItemInfo? = null,
    val privacyClassification: String? = null,
    val traversalDepth: Int = 0,
) {
    companion object {
        /** Maximum length for any text field before truncation. */
        const val MAX_TEXT_LENGTH: Int = 1024
    }
}

/**
 * Collection (list/grid) metadata from accessibility.
 */
@Serializable
data class CollectionInfo(
    val rowCount: Int,
    val columnCount: Int,
    val isHierarchical: Boolean = false,
)

/**
 * Collection item position metadata from accessibility.
 */
@Serializable
data class CollectionItemInfo(
    val rowIndex: Int,
    val rowSpan: Int,
    val columnIndex: Int,
    val columnSpan: Int,
)
