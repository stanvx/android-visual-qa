package com.androidvisualqa.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.ids.NodeId
import java.util.UUID

/**
 * Converts an Android [AccessibilityNodeInfo] into a [NodeSnapshot] for the
 * capture pipeline.
 *
 * Text fields are truncated to [TEXT_MAX_LENGTH] characters to limit memory
 * and serialization size in the ring buffer and snapshot tree.
 *
 * ponytail: 200-char truncation is a safety net. The NodeSnapshot schema
 * already defines MAX_TEXT_LENGTH = 1024, but in practice no meaningful UI
 * text exceeds 200 chars. Increase if longer text is ever needed for matching.
 */
public object NodeNormalizer {

    /** Maximum length for truncated text fields. */
    internal const val TEXT_MAX_LENGTH: Int = 200

    /**
     * Normalizes [info] into a [NodeSnapshot].
     *
     * @param info The platform accessibility node. **Not** recycled by this
     *             method — the caller owns the lifecycle.
     * @param parentId The snapshot-local ID of the parent, or `null` for root.
     * @return A snapshot with fields extracted from [info]. Password-field text
     *         is set to `null` to avoid leaking sensitive content.
     */
    public fun normalize(info: AccessibilityNodeInfo, parentId: NodeId?): NodeSnapshot {
        val nodeId = NodeId(UUID.randomUUID().toString())

        // Bounds — must be present; callers should pre-filter empty bounds.
        // Use the property accessor which maps to getBoundsInScreen on the JVM
        // but may dispatch differently on JVM stubs.
        val boundsRect = android.graphics.Rect()
        info.getBoundsInScreen(boundsRect)

        // Text — handle SecurityException for protected fields.
        // ponytail: truncation prevents a single rogue node (e.g. a WebView
        // with the full page text) from bloating the snapshot.
        val rawText: CharSequence? = try {
            info.text
        } catch (_: SecurityException) {
            null
        }
        val text = if (info.isPassword) {
            null
        } else {
            rawText?.take(TEXT_MAX_LENGTH)?.toString()
        }

        val contentDescription: String? = try {
            info.contentDescription?.take(TEXT_MAX_LENGTH)?.toString()
        } catch (_: SecurityException) {
            null
        }

        // Children — generate placeholder IDs (the traversal driver sets
        // actual parentId references on children).
        val childCount = info.childCount
        val childIds = List(childCount) {
            NodeId(UUID.randomUUID().toString())
        }

        // Class name
        val className = info.className?.toString()

        return NodeSnapshot(
            nodeId = nodeId,
            parentId = parentId,
            childIds = childIds,
            windowId = info.windowId,
            boundsLeft = boundsRect.left,
            boundsTop = boundsRect.top,
            boundsRight = boundsRect.right,
            boundsBottom = boundsRect.bottom,
            text = text,
            contentDescription = contentDescription,
            className = className,
            viewIdRaw = info.viewIdResourceName,
            isEnabled = info.isEnabled,
            isClickable = info.isClickable,
            isFocusable = info.isFocusable,
            isChecked = info.isChecked,
            isScrollable = info.isScrollable,
            isPassword = info.isPassword,
            isVisibleToUser = info.isVisibleToUser,
            traversalDepth = 0, // set by the traversal driver
        )
    }
}
