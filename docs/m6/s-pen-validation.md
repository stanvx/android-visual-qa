# S Pen Validation Checklist

Physical validation checklist for QA teams testing Android Visual QA with
Samsung S Pen devices.  Each test must be performed on physical hardware
(the Android emulator does not simulate stylus input).

> **Target audience:** QA engineers validating the capture, annotation, and
> matching workflows with S Pen input.

---

## Test Matrix

| No | Test | Device Model | Android Version | SDK Version | Expected Behaviour | Actual Result | Screenshot |
|---|---|---|---|---|---|---|---|
| 1 | Pressure level: 10 | _fill_ | _fill_ | _fill_ | Annotation stroke visible at minimum pressure | _pass / fail_ | _path_ |
| 2 | Pressure level: 100 | _fill_ | _fill_ | _fill_ | Stroke width noticeably thicker than test 1 | _pass / fail_ | _path_ |
| 3 | Pressure level: 256 | _fill_ | _fill_ | _fill_ | Stroke medium width, proportional | _pass / fail_ | _path_ |
| 4 | Pressure level: 1024 | _fill_ | _fill_ | _fill_ | Stroke clearly thicker than test 3 | _pass / fail_ | _path_ |
| 5 | Pressure level: 4096 | _fill_ | _fill_ | _fill_ | Stroke at maximum width, no aliasing | _pass / fail_ | _path_ |
| 6 | Tilt: −30° | _fill_ | _fill_ | _fill_ | Annotation elliptical, skewed left | _pass / fail_ | _path_ |
| 7 | Tilt: +30° | _fill_ | _fill_ | _fill_ | Annotation elliptical, skewed right | _pass / fail_ | _path_ |
| 8 | Barrel button shortcut | _fill_ | _fill_ | _fill_ | App-defined action fires on button press | _pass / fail_ | _path_ |
| 9 | Hover detection | _fill_ | _fill_ | _fill_ | Cursor/crosshair appears near but not touching screen | _pass / fail_ | _path_ |
| 10 | Low-latency mode | _fill_ | _fill_ | _fill_ | Stroke appears within ~16ms of S Pen contact | _pass / fail_ | _path_ |
| 11 | Palm rejection | _fill_ | _fill_ | _fill_ | Resting palm on screen does not trigger marks | _pass / fail_ | _path_ |
| 12 | Capture trigger via S Pen button | _fill_ | _fill_ | _fill_ | Quick capture fires on S Pen button press | _pass / fail_ | _path_ |

---

## Setup

1. **Device:** Samsung Galaxy Tab S-series or Note-series with active S Pen.
2. **Android version:** 12 (API 31) or higher.
3. **App build:** `release` variant with R8 enabled (signed APK or sideload).
4. **S Pen settings:** Ensure Air Actions / S Pen remote is enabled in
   system settings.
5. **Low latency:** Verify the device supports
   `WindowManager.LayoutParams.FLAG_WRITE_SURFACE_TRANSACTION_PRIORITY` or
   equivalent low-latency stylus pipeline.

---

## Procedure

### Test 1–5: Pressure levels

1. Open the annotation editor.
2. Select the pen tool (not highlighter or shape).
3. Apply strokes at each pressure level in the test matrix.
4. Verify the stroke width scales proportionally.
5. Capture a screenshot showing the stroke variation side-by-side.

### Test 6–7: Tilt

1. Hold the S Pen at approximately −30° (angled left) and +30° (angled
   right).
2. Draw a horizontal line across the screen.
3. Verify the stroke is elliptical and the ellipse centre is offset in
   the tilt direction.
4. Screenshot the result for each tilt angle.

### Test 8: Barrel button shortcuts

1. While hovering or in contact, press the S Pen barrel button.
2. Verify the app-defined shortcut fires (e.g., toggle highlighter,
   undo last stroke, capture screenshot).
3. Test both single-press and press-hold if the app supports both.

### Test 9: Hover detection

1. Hold the S Pen 2–10 mm above the screen without touching.
2. Verify a cursor or crosshair appears on the annotation surface.
3. Verify that hover does NOT trigger any mark on the canvas.

### Test 10: Low-latency mode

1. Enable low-latency mode in the app settings (if available) or via
   managed configuration.
2. Draw fast strokes using the S Pen.
3. Verify the stroke appears nearly instantaneously — no visible lag
   between pen tip contact and ink on screen.
4. Use a high-speed camera (240 fps) if available to measure latency.

### Test 11: Palm rejection

1. Rest the side of your palm on the screen while holding the S Pen.
2. Draw a stroke with the pen tip.
3. Verify no accidental marks appear where the palm contacts the screen.
4. Verify the stroke from the pen tip is unaffected.

### Test 12: S Pen button capture trigger

1. While the app is in the foreground (or running as a foreground service
   with the AccessibilityService active), press the S Pen button.
2. If the system supports Air Actions, verify the capture action fires.
3. Verify the captured screenshot opens in the editor.

---

## Expected Results Summary

| Metric | Threshold |
|---|---|
| Pressure sensitivity | All 5 test levels produce distinctly different stroke widths |
| Tilt response | Elliptical offset visible at ±30° |
| Barrel button | Action fires within 200 ms of press |
| Hover accuracy | Cursor within 2 mm of actual S Pen position |
| Low-latency mode | ≤ 16 ms between contact and first pixel |
| Palm rejection | Zero accidental marks in 10 consecutive trials |
| Capture trigger | Capture starts within 1 second of button press |

---

## Known Limitations

- S Pen Air Actions behaviour depends on the Samsung firmware version and
  may vary across regional builds.
- Low-latency rendering is available only on devices whose GPU drivers
  support the `BufferQueue` producer hint `NATIVE_WINDOW_SCALING_MODE_FREEZE`.
- Pressure curves differ by device generation.  Tab S8 series reports
  4096 levels; older Note models report 2048.
- Barrel button behaviour in non-Samsung apps may be overridden by system
  gesture shortcuts (e.g., Bixby, Edge Panel).
