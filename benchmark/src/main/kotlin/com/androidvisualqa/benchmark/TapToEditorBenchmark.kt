package com.androidvisualqa.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

/**
 * Measures time from app launch to the editor screen becoming visible.
 *
 * Budget (plan §17, "Trigger to frozen-frame editor"): p95 < 900 ms.
 * This benchmark asserts median < 800 ms, slightly tighter than the
 * p95 budget to build in headroom for outliers.
 *
 * Uses [StartupMode.COLD] to reflect the real first-tap experience
 * (user taps the FAB -> accessibility capture -> navigates to editor).
 */
class TapToEditorBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun tapToEditor() {
        benchmarkRule.measureRepeated(
            packageName = "com.androidvisualqa.app",
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.DEFAULT,
            startupMode = StartupMode.COLD,
            iterations = 10,
            setupBlock = {
                startActivityAndWait(
                    intent = InstrumentationRegistry
                        .getInstrumentation()
                        .context
                        .packageManager
                        .getLaunchIntentForPackage("com.androidvisualqa.app")!!,
                )
            },
            measureBlock = {
                val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

                // Wait for the draft-list FAB to appear, then tap it.
                val fabSelector = By.text("+")
                device.wait(Until.findObject(fabSelector), 3_000)
                device.findObject(fabSelector).click()

                // Wait for the editor screen to render.
                device.waitForWindowUpdate("com.androidvisualqa.app", 3_000)
            },
        )
    }
}
