package com.androidvisualqa.geometry

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TransformsTest {

    @Test
    fun `screenToWindow and windowToScreen round-trip`() {
        val screenPt = screenPx(150.0, 300.0)
        val windowPt = screenToWindow(screenPt, statusBarHeight = 48.0, leftInset = 0.0)
        assertEquals(150.0, windowPt.x, 1e-9)
        assertEquals(252.0, windowPt.y, 1e-9)

        val back = windowToScreen(windowPt, statusBarHeight = 48.0, leftInset = 0.0)
        assertEquals(screenPt.x, back.x, 1e-9)
        assertEquals(screenPt.y, back.y, 1e-9)
    }

    @Test
    fun `screenToCapture and captureToScreen round-trip`() {
        val screenPt = screenPx(100.0, 200.0)
        val scaleX = 0.5 // capture is half the width
        val scaleY = 0.5
        val capturePt = screenToCapture(screenPt, scaleX, scaleY)
        assertEquals(50.0, capturePt.x, 1e-9)
        assertEquals(100.0, capturePt.y, 1e-9)

        val back = captureToScreen(capturePt, scaleX, scaleY)
        assertEquals(screenPt.x, back.x, 1e-9)
        assertEquals(screenPt.y, back.y, 1e-9)
    }

    @Test
    fun `captureToNormalized maps origin to top-left`() {
        val container = Bounds(0.0, 0.0, 1920.0, 1080.0, CoordinateSpace.CapturePx)
        val origin = capturePx(0.0, 0.0)
        val n = captureToNormalized(origin, container)
        assertEquals(0.0, n.x, 1e-9)
        assertEquals(1.0, n.y, 1e-9) // y is inverted
    }

    @Test
    fun `captureToNormalized maps bottom-right to 1,0`() {
        val container = Bounds(0.0, 0.0, 1920.0, 1080.0, CoordinateSpace.CapturePx)
        val br = capturePx(1920.0, 1080.0)
        val n = captureToNormalized(br, container)
        assertEquals(1.0, n.x, 1e-9)
        assertEquals(0.0, n.y, 1e-9)
    }

    @Test
    fun `captureToNormalized maps centre`() {
        val container = Bounds(0.0, 0.0, 1920.0, 1080.0, CoordinateSpace.CapturePx)
        val centre = capturePx(960.0, 540.0)
        val n = captureToNormalized(centre, container)
        assertEquals(0.5, n.x, 1e-9)
        assertEquals(0.5, n.y, 1e-9)
    }

    @Test
    fun `captureToNormalized and normalizedToCapture round-trip`() {
        val container = Bounds(0.0, 0.0, 1920.0, 1080.0, CoordinateSpace.CapturePx)
        val pt = capturePx(480.0, 270.0)
        val n = captureToNormalized(pt, container)
        val back = normalizedToCapture(n, container)
        assertEquals(pt.x, back.x, 1e-9)
        assertEquals(pt.y, back.y, 1e-9)
    }

    @Test
    fun `captureToNormalized with non-zero container origin`() {
        val container = Bounds(100.0, 200.0, 500.0, 600.0, CoordinateSpace.CapturePx)
        val pt = capturePx(100.0, 200.0) // top-left of container
        val n = captureToNormalized(pt, container)
        assertEquals(0.0, n.x, 1e-9)
        assertEquals(1.0, n.y, 1e-9)
    }

    @Test
    fun `captureToEditor and editorToCapture round-trip`() {
        val capturePt = capturePx(100.0, 200.0)
        val editorPt = captureToEditor(capturePt, zoom = 2.0, panX = 50.0, panY = -10.0)
        assertEquals(250.0, editorPt.x, 1e-9)
        assertEquals(390.0, editorPt.y, 1e-9)

        val back = editorToCapture(editorPt, zoom = 2.0, panX = 50.0, panY = -10.0)
        assertEquals(capturePt.x, back.x, 1e-9)
        assertEquals(capturePt.y, back.y, 1e-9)
    }

    @Test
    fun `screenToWindow with left inset`() {
        val screenPt = screenPx(50.0, 100.0)
        val windowPt = screenToWindow(screenPt, statusBarHeight = 48.0, leftInset = 20.0)
        assertEquals(30.0, windowPt.x, 1e-9)
        assertEquals(52.0, windowPt.y, 1e-9)
    }

    @Test
    fun `edge case zero scale screenToCapture`() {
        val screenPt = screenPx(0.0, 0.0)
        val capturePt = screenToCapture(screenPt, scaleX = 0.5, scaleY = 0.5)
        assertEquals(0.0, capturePt.x, 1e-9)
        assertEquals(0.0, capturePt.y, 1e-9)
    }
}
