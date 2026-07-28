package com.androidvisualqa.geometry

/**
 * Pure functions that convert a [Point] between coordinate spaces.
 *
 * Every function requires the caller to supply the scale factors that define the
 * relationship between the source and target spaces.  No "magic" or ambient
 * state is used.
 */

// ---------------------------------------------------------------------------
// ScreenPx <-> WindowPx
// ---------------------------------------------------------------------------

/**
 * Translates a screen-pixel point into window-pixel space by subtracting the
 * [statusBarHeight] and [leftInset] (the position of the window frame within
 * the screen).
 *
 * In multi-window / edge-to-edge layouts the window origin is not (0,0) in
 * screen space.  The caller is responsible for providing the current window
 * insets.
 *
 * @param point       Point in screen pixels.
 * @param statusBarHeight  Height of the status bar (top inset) in screen px.
 * @param leftInset   Left inset (e.g. display cutout) in screen px.
 */
fun screenToWindow(
    point: Point<CoordinateSpace.ScreenPx>,
    statusBarHeight: Double,
    leftInset: Double,
): Point<CoordinateSpace.WindowPx> =
    windowPx(
        x = point.x - leftInset,
        y = point.y - statusBarHeight,
    )

/**
 * Translates a window-pixel point back into screen-pixel space by adding the
 * window insets.
 */
fun windowToScreen(
    point: Point<CoordinateSpace.WindowPx>,
    statusBarHeight: Double,
    leftInset: Double,
): Point<CoordinateSpace.ScreenPx> =
    screenPx(
        x = point.x + leftInset,
        y = point.y + statusBarHeight,
    )

// ---------------------------------------------------------------------------
// ScreenPx <-> CapturePx
// ---------------------------------------------------------------------------

/**
 * Maps a screen-pixel point into capture-pixel space.
 *
 * The capture bitmap may have a different resolution than the display
 * (accessibility screenshots are typically scaled).  The caller supplies
 * `scaleX`/`scaleY` as `captureWidth / screenWidth` and
 * `captureHeight / screenHeight`.
 *
 * @param point  Point in screen pixels.
 * @param scaleX Horizontal scale factor (capturePx per screenPx).
 * @param scaleY Vertical scale factor.
 */
fun screenToCapture(
    point: Point<CoordinateSpace.ScreenPx>,
    scaleX: Double,
    scaleY: Double,
): Point<CoordinateSpace.CapturePx> =
    capturePx(x = point.x * scaleX, y = point.y * scaleY)

/**
 * Maps a capture-pixel point back into screen-pixel space.
 */
fun captureToScreen(
    point: Point<CoordinateSpace.CapturePx>,
    scaleX: Double,
    scaleY: Double,
): Point<CoordinateSpace.ScreenPx> =
    screenPx(x = point.x / scaleX, y = point.y / scaleY)

// ---------------------------------------------------------------------------
// CapturePx <-> EditorPx
// ---------------------------------------------------------------------------

/**
 * Maps a capture-pixel point into editor-pixel space.  When the captured image
 * is displayed inside the editor the editor viewport may apply a uniform zoom
 * and a pan offset.
 *
 * @param point     Point in capture pixels.
 * @param zoom      Uniform zoom factor (editorPx per capturePx).
 * @param panX      Horizontal pan offset in editor pixels.
 * @param panY      Vertical pan offset in editor pixels.
 */
fun captureToEditor(
    point: Point<CoordinateSpace.CapturePx>,
    zoom: Double,
    panX: Double,
    panY: Double,
): Point<CoordinateSpace.EditorPx> =
    editorPx(
        x = point.x * zoom + panX,
        y = point.y * zoom + panY,
    )

/**
 * Maps an editor-pixel point back into capture-pixel space.
 */
fun editorToCapture(
    point: Point<CoordinateSpace.EditorPx>,
    zoom: Double,
    panX: Double,
    panY: Double,
): Point<CoordinateSpace.CapturePx> =
    capturePx(
        x = (point.x - panX) / zoom,
        y = (point.y - panY) / zoom,
    )

// ---------------------------------------------------------------------------
// CapturePx <-> NormalizedFrame
// ---------------------------------------------------------------------------

/**
 * Maps a capture-pixel point into normalized frame coordinates [0, 1].
 *
 * The [container] defines the capture-space rectangle that maps to the
 * normalized unit square.  This is typically the full capture bitmap size.
 *
 * Contract:
 * - The returned x is in range [0, 1] when the source point falls within the
 *   `container` horizontally. Points outside are representable (negative or
 *   >1).
 * - The returned y is inverted so that (0,0) is top-left in capture space and
 *   bottom-left in normalized space when y=height.
 */
fun captureToNormalized(
    point: Point<CoordinateSpace.CapturePx>,
    container: Bounds<CoordinateSpace.CapturePx>,
): Point<CoordinateSpace.NormalizedFrame> =
    normalizedFrame(
        x = (point.x - container.left) / container.width,
        y = 1.0 - (point.y - container.top) / container.height,
    )

/**
 * Maps a normalized-frame point back into capture-pixel space.
 */
fun normalizedToCapture(
    point: Point<CoordinateSpace.NormalizedFrame>,
    container: Bounds<CoordinateSpace.CapturePx>,
): Point<CoordinateSpace.CapturePx> =
    capturePx(
        x = point.x * container.width + container.left,
        y = (1.0 - point.y) * container.height + container.top,
    )
