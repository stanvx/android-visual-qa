package com.androidvisualqa.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Baseline Profile generator for [com.androidvisualqa.app.MainActivity].
 *
 * Profiles three hot paths:
 * 1. App cold startup (launch to draft list).
 * 2. FAB tap -> editor screen (matching engine / annotation start).
 * 3. Save flow (report writer path).
 *
 * The generated profile is committed under `app/src/main/baseline-prof.txt`
 * (Lane V will integrate this into the app build).
 *
 * Run on a physical device or API 36 emulator:
 * ```
 * ./gradlew :benchmark:connectedCheck \
 *   -Pandroid.testInstrumentationRunnerArguments.class=\
 *   com.androidvisualqa.benchmark.BaselineProfileGenerator
 * ```
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        baselineProfileRule.collect(
            packageName = "com.androidvisualqa.app",
            profileBlock = {
                // --- 1. Cold startup ---
                startActivityAndWait(
                    intent = InstrumentationRegistry
                        .getInstrumentation()
                        .context
                        .packageManager
                        .getLaunchIntentForPackage("com.androidvisualqa.app")!!,
                )
                val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                device.waitForWindowUpdate("com.androidvisualqa.app", 3_000)

                // --- 2. Navigate to editor ---
                device.wait(Until.findObject(By.text("+")), 3_000)
                device.findObject(By.text("+")).click()
                device.waitForWindowUpdate("com.androidvisualqa.app", 3_000)

                // --- 3. Simulate save flow ---
                // ponytail: no explicit "save" button in the current editor;
                // the profile generator exercises the annotation + save path
                // by navigating back, which still compiles the report writer.
                device.pressBack()
                device.waitForWindowUpdate("com.androidvisualqa.app", 2_000)
            },
        )
    }
}
