# Performance Budgets

This document defines the performance thresholds that the Android Visual QA
app must meet for each critical user journey.  These budgets are enforced in
CI via the `:benchmark` module (Lane U) using Jetpack Macrobenchmark.

---

## Budget Table

| Metric | Budget | Measurement Method | CI Gate | Priority |
|---|---|---|---|---|
| Cold launch (first install) | < 1.5 s | `MacrobenchmarkRule.measureRepeated("coldStartup")` | Blocking | P0 |
| Tap to editor | < 800 ms | `MacrobenchmarkRule.measureRepeated("tapToEditor")` | Blocking | P0 |
| Stylus frame time | < 16 ms (60 fps) | `FrameTimingMetric` on editor activity | Warning | P1 |
| Large-tree matching | < 200 ms | `StartupTimingMetric` on match engine | Blocking | P0 |
| Save capture | < 750 ms | `MacrobenchmarkRule.measureRepeated("saveCapture")` | Blocking | P0 |
| History scroll | < 16 ms | `FrameTimingMetric` on history list | Warning | P1 |
| Export share sheet | < 500 ms | `MacrobenchmarkRule.measureRepeated("exportShare")` | Warning | P1 |

---

## How Each Metric Is Measured

### Cold Launch (`coldStartup`)

```kotlin
@RunWith(AndroidJUnit4::class)
class ColdStartupBenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() = benchmarkRule.measureRepeated(
        packageName = "com.androidvisualqa.app",
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage("com.androidvisualqa.app")
            }
        )
    }
}
```

**Bound:** 1.5 seconds.  This is the full cold path: process spawn, class
loading, `Application.onCreate`, `MainActivity.onCreate`, first frame of
`setContent`.

### Tap to Editor (`tapToEditor`)

From the moment the floating capture button is tapped to the first frame
of the annotation editor being fully drawn.  Includes the capture pipeline
(accessibility screenshot, pixel capture) and the navigation transaction to
the editor screen.

**Bound:** 800 ms.  This is the primary user-facing latency — any slower
and the user perceives a "stutter" after tapping capture.

### Stylus Frame Time (`frameTiming`)

Measured as the **median** frame duration across a 5-second annotation
session.  Uses `FrameTimingMetric` with `FrameTimingMetric.Mode.STANDALONE`.
If the median exceeds 16 ms, the 60 fps target is missed and the user sees
janky inking.

**Bound:** 16 ms.  Exceeding this triggers a CI warning (non-blocking).
Only enforced when the benchmark device has low-latency stylus support.

### Large-Tree Matching (`largeTreeMatching`)

```kotlin
benchmarkRule.measureRepeated(
    metrics = listOf(StartupTimingMetric()),
    iterations = 10,
    startupMode = StartupMode.WARM,
) {
    // Load a 5000-node accessibility tree and run match()
    matchEngine.match(tree, template)
}
```

**Bound:** 200 ms.  This is the worst-case scenario — a deeply nested UI
with hundreds of interactive elements.  The matching engine is used for
every screenshot comparison; any regression here cascades into the "tap to
editor" budget.

### Save Capture (`saveCapture`)

From pressing "Save" in the editor to the confirmation toast appearing.
Includes: JSONL serialisation of the report, writing the screenshot (WebP),
and inserting the database record.

**Bound:** 750 ms (plan §17).  The benchmark asserts under 1 000 ms to allow margin for larger payloads.

### History Scroll (`historyScroll`)

`FrameTimingMetric` measured during a fast fling through 100+ report
entries in the history list.  The metric is the P99 frame time.

**Bound:** 16 ms (P99).  Exceeding triggers a CI warning.

### Export Share Sheet (`exportShare`)

From pressing "Share" to the Android share sheet appearing.  Includes
compressing the export payload and creating the `ContentResolver` URI.

**Bound:** 500 ms.  Exceeding triggers a CI warning.

---

## CI Gates

| Gate | Action on Failure |
|---|---|
| Blocking (P0) | Pipeline is RED.  Release cannot proceed. |
| Warning (P1) | Pipeline is YELLOW.  Release can proceed with engineering approval. |

Blocking gates are:
- `coldStartup` > 1.5 s
- `tapToEditor` > 800 ms
- `largeTreeMatching` > 200 ms
- `saveCapture` > 750 ms

---

## Device Under Test

All benchmarks run on a **Google Pixel 6** (API 31) in the CI emulator.

| Property | Value |
|---|---|
| Device | Pixel 6 (avd) |
| API level | 31 |
| ABI | arm64-v8a |
| RAM | 6 GB |
| Heap | 256 MB |
| Storage | 32 GB |
| Locale | en-US |

Different devices will produce different absolute numbers.  The budgets
above are calibrated against the Pixel 6 reference device.  A device with
a slower CPU (e.g., a low-end AVD with 2 GB RAM) may not meet the budgets
without profile-guided optimisation.

---

## Profile-Guided Optimisation

The `:benchmark` module produces a **Baseline Profile** (`baseline-prof.txt`)
that is bundled into the release APK.  This profile tells the Android
Runtime (ART) which methods to AOT-compile on first install, reducing cold
start time by 30–40 %.

The baseline profile is regenerated on every CI run that touches the
critical path code.

---

## Regression Workflow

1. Benchmark fails in CI → the `:benchmark` test output shows the delta.
2. Engineer investigates: is it a code change, a device fluctuation, or a
   new dependency?
3. If code change: revert or optimise the offending path.
4. If device fluctuation: re-run the benchmark job.
5. If new dependency: audit its class-loading and init cost.  Add baseline
   profile rules if needed.
6. Re-run benchmarks locally with `./gradlew :benchmark:pixel6Api31Benchmark`
   before merging.
