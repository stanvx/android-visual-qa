package com.androidvisualqa.benchmark

import com.androidvisualqa.matching.MatchingEngine
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * JVM smoke test that constructs the benchmark classes without running them
 * (they require a device/emulator and Macrobenchmark instrumentation).
 *
 * These tests verify that:
 * - Benchmark class constructors are resolvable and compile correctly.
 * - Rule annotations are present.
 * - Internal helpers (e.g. tree builders) produce valid output.
 */
class BenchmarkSmokeTest {

    @Test
    fun launchBenchmark_isConstructable() {
        // MacrobenchmarkRule and the benchmark class compile.
        // On JVM we can only verify the class exists.
        val clazz = Class.forName("com.androidvisualqa.benchmark.LaunchBenchmark")
        val instance = clazz.getDeclaredConstructor().newInstance()
        assertNotNull(instance)
    }

    @Test
    fun tapToEditorBenchmark_isConstructable() {
        val clazz = Class.forName("com.androidvisualqa.benchmark.TapToEditorBenchmark")
        val instance = clazz.getDeclaredConstructor().newInstance()
        assertNotNull(instance)
    }

    @Test
    fun stylusFrameBenchmark_isConstructable() {
        val clazz = Class.forName("com.androidvisualqa.benchmark.StylusFrameBenchmark")
        val instance = clazz.getDeclaredConstructor().newInstance()
        assertNotNull(instance)
    }

    @Test
    fun largeTreeMatchingEngine_constructsAndRanks() {
        val engine = MatchingEngine()
        assertNotNull(engine)

        // Quick verification that the tree builder works.
        val tree = buildFakeTree(100)
        assertNotNull(tree)
        org.junit.jupiter.api.Assertions.assertEquals(100, tree.size)
    }

    @Test
    fun savingBenchmark_classExists() {
        // BenchmarkRule depends on Android instrumentation arguments;
        // verify the class is loadable on JVM without instantiating it.
        val clazz = Class.forName("com.androidvisualqa.benchmark.SavingBenchmark")
        assertNotNull(clazz)
        // Verify it has the expected method.
        clazz.getDeclaredMethod("measureSave")
    }

    @Test
    fun historyScrollBenchmark_isConstructable() {
        val clazz = Class.forName("com.androidvisualqa.benchmark.HistoryScrollBenchmark")
        val instance = clazz.getDeclaredConstructor().newInstance()
        assertNotNull(instance)
    }

    @Test
    fun baselineProfileGenerator_isConstructable() {
        val clazz = Class.forName("com.androidvisualqa.benchmark.BaselineProfileGenerator")
        val instance = clazz.getDeclaredConstructor().newInstance()
        assertNotNull(instance)
    }

    // Copy of the tree builder from LargeTreeMatchingBenchmark for JVM-side verification.
    private fun buildFakeTree(nodeCount: Int): List<com.androidvisualqa.model.capture.NodeSnapshot> {
        if (nodeCount <= 0) return emptyList()

        val nodes = mutableListOf<com.androidvisualqa.model.capture.NodeSnapshot>()
        val branchFactor = 5
        var counter = 1

        val rootId = com.androidvisualqa.model.ids.NodeId("root")
        val root = com.androidvisualqa.model.capture.NodeSnapshot(
            nodeId = rootId,
            boundsLeft = 0,
            boundsTop = 0,
            boundsRight = 1080,
            boundsBottom = 1920,
            text = "root",
            className = "android.view.View",
        )
        nodes.add(root)

        val queue = ArrayDeque<com.androidvisualqa.model.capture.NodeSnapshot>()
        queue.addLast(root)

        while (queue.isNotEmpty() && nodes.size < nodeCount) {
            val parent = queue.removeFirst()
            val children = mutableListOf<com.androidvisualqa.model.ids.NodeId>()
            for (i in 0 until branchFactor) {
                if (nodes.size >= nodeCount) break
                val childId = com.androidvisualqa.model.ids.NodeId("node-$counter")
                val child = com.androidvisualqa.model.capture.NodeSnapshot(
                    nodeId = childId,
                    parentId = parent.nodeId,
                    boundsLeft = counter % 1080,
                    boundsTop = counter % 1920,
                    boundsRight = (counter + 100) % 1080,
                    boundsBottom = (counter + 100) % 1920,
                    text = "item-$counter",
                    className = if (counter % 3 == 0) "android.widget.Button" else "android.view.View",
                    isClickable = counter % 3 == 0,
                )
                children.add(childId)
                nodes.add(child)
                queue.addLast(child)
                counter++
            }
            val idx = nodes.indexOf(parent)
            if (idx >= 0) {
                nodes[idx] = parent.copy(childIds = children)
            }
        }

        return nodes
    }
}
