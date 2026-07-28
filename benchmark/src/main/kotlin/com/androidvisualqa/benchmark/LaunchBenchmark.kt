package com.androidvisualqa.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

/**
 * Cold-launch benchmark for [com.androidvisualqa.app.MainActivity].
 *
 * Budget (plan §17): median cold launch < 1.5 s.
 *
 * Uses [CompilationMode.DEFAULT] to measure with the profile-guided
 * compilation the end-user will typically have, and [StartupMode.COLD]
 * to capture the worst-case process start from scratch.
 */
class LaunchBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldLaunch() {
        benchmarkRule.measureRepeated(
            packageName = "com.androidvisualqa.app",
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.DEFAULT,
            startupMode = StartupMode.COLD,
            iterations = 10,
            setupBlock = {},
            measureBlock = {
                startActivityAndWait(
                    intent = InstrumentationRegistry
                        .getInstrumentation()
                        .context
                        .packageManager
                        .getLaunchIntentForPackage("com.androidvisualqa.app")!!,
                )
            },
        )
    }
}
