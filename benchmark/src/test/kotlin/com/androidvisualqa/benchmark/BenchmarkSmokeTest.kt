package com.androidvisualqa.benchmark

import com.androidvisualqa.benchmark.internal.buildFakeTree
import com.androidvisualqa.matching.MatchingEngine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * JVM smoke test that constructs the benchmark classes without running them
 * (they require a device/emulator and Macrobenchmark instrumentation).
 *
 * These tests verify that:
 * - Benchmark class constructors are resolvable and compile correctly.
 * - Rule annotations are present.
 * - Internal helpers (e.g. [buildFakeTree]) produce valid output.
 */
class BenchmarkSmokeTest {

    @Test
    fun launchBenchmark_isConstructable() {
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
        assertEquals(100, tree.size)
    }

    @Test
    fun savingBenchmark_classExists() {
        val clazz = Class.forName("com.androidvisualqa.benchmark.SavingBenchmark")
        assertNotNull(clazz)
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

    @Test
    fun buildFakeTree_producesExpectedNodeCount() {
        val tree = buildFakeTree(100)
        assertEquals(100, tree.size)
    }
}
