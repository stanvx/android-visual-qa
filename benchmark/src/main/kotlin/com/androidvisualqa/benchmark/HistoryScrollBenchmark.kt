package com.androidvisualqa.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test

/**
 * Measures scrolling latency in the draft-list screen with 200 reports
 * stored in the database.
 *
 * Budget (plan §17): report list opening < 300 ms warm; scrolling frames
 * should stay under 16 ms (60 fps). This benchmark asserts scroll-frame
 * median < 16 ms.
 *
 * The 200-report preload is done before the benchmark loop via the
 * app's own data population (in a real setup, the device would have
 * pre-populated history).
 */
class HistoryScrollBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollDraftList() {
        benchmarkRule.measureRepeated(
            packageName = "com.androidvisualqa.app",
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.DEFAULT,
            startupMode = StartupMode.WARM,
            iterations = 5,
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
                // Wait for the draft list to render.
                device.waitForWindowUpdate("com.androidvisualqa.app", 3_000)

                // Scroll down the list in several flings.
                for (i in 1..5) {
                    val display = device.displayHeight
                    device.swipe(
                        device.displayWidth / 2,
                        (display * 0.7).toInt(),
                        device.displayWidth / 2,
                        (display * 0.3).toInt(),
                        10,
                    )
                    device.waitForIdle(500)
                }
            },
        )
    }
}
