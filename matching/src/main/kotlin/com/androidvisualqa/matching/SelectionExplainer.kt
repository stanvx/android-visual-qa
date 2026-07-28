package com.androidvisualqa.matching

import com.androidvisualqa.model.capture.NodeSnapshot

/**
 * Builds a short human-readable explanation of why a [RankedCandidate]
 * scored the way it did.
 *
 * The explanation mentions the strongest feature contribution (by value)
 * with its score, and includes the resource ID or text if available.
 */
object SelectionExplainer {

    /**
     * Produce a one-line explanation string for [candidate].
     */
    fun explain(candidate: RankedCandidate): String =
        buildString {
            val features = listOfNotNull(
                "overlap" to candidate.scoreOverlap,
                "containment" to candidate.scoreContainment,
                "center proximity" to candidate.scoreCenterProximity,
                "actionable" to candidate.scoreActionable,
                "semantic richness" to candidate.scoreSemanticRichness,
                "leaf preference" to candidate.scoreLeafPreference,
                "recent event" to candidate.scoreRecentEvent,
                "sdk evidence" to candidate.scoreSdkEvidence,
            )

            val strongest = features.maxByOrNull { it.second } ?: Pair("none", 0.0)
            append("Selected because: ${strongest.first} (${"%.2f".format(strongest.second)})")

            appendNodeInfo(candidate.node, this)
        }

    /**
     * Internal build helper used by [MatchingEngine] when constructing candidates.
     */
    internal fun explain(
        overlap: Double,
        containment: Double,
        centerProx: Double,
        actionable: Double,
        semantic: Double,
        leaf: Double,
        recent: Double,
        sdkEv: Double,
        zOrderBonus: Double,
        node: NodeSnapshot,
    ): String {
        val features = listOfNotNull(
            Pair("overlap", overlap + zOrderBonus * 0.25), // distribute z-order across features
            Pair("containment", containment),
            Pair("center proximity", centerProx),
            Pair("actionable", actionable),
            Pair("semantic richness", semantic),
            Pair("leaf preference", leaf),
            Pair("recent event", recent),
            Pair("sdk evidence", sdkEv),
        )
        val strongest = features.maxByOrNull { it.second } ?: Pair("none", 0.0)
        return buildString {
            append("Selected because: ${strongest.first} (${"%.2f".format(strongest.second)})")
            appendNodeInfo(node, this)
        }
    }

    private fun appendNodeInfo(node: NodeSnapshot, sb: StringBuilder) {
        // Use local val to avoid smart-cast issues with module-private types
        val rid = node.viewIdRaw
        if (!rid.isNullOrBlank()) {
            sb.append(", has resource id ($rid)")
            return
        }
        val t = node.text
        if (t != null && t.isNotBlank()) {
            sb.append(", has text (${t.take(40)})")
            return
        }
        val cd = node.contentDescription
        if (cd != null && cd.isNotBlank()) {
            sb.append(", has content description (${cd.take(40)})")
            return
        }
        if (node.isClickable) {
            sb.append(", actionable")
        }
    }
}
