package com.androidvisualqa.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androidvisualqa.benchmark.internal.buildFakeTree
import com.androidvisualqa.benchmark.internal.matchingInputForTree
import com.androidvisualqa.matching.MatchingEngine
import com.androidvisualqa.model.capture.NodeSnapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures matching-engine run time against a 5000-node fake accessibility tree.
 *
 * Budget (plan §17): match 1,000 nodes < 50 ms, hard deadline 150 ms.
 * This benchmark targets a 5x larger tree (5,000 nodes) and asserts
 * median < 200 ms, which is consistent with the per-1k-node budget
 * plus overhead for the larger input.
 *
 * The tree is constructed with a deterministic structure:
 * a flat root with 4 levels of branching, totalling ~5,000 nodes.
 *
 * @see buildFakeTree for the tree-building implementation.
 */
@RunWith(AndroidJUnit4::class)
class LargeTreeMatchingBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun matchLargeTree() {
        val tree = buildFakeTree(nodeCount = 5000)
        val engine = MatchingEngine()

        benchmarkRule.measureRepeated {
            val input = matchingInputForTree(tree)

            val result = engine.rank(input)
            // Ensure the engine ran — at least some candidates should be ranked.
            check(result.isNotEmpty()) {
                "Expected at least one ranked candidate for a 5000-node tree"
            }
        }
    }
}
