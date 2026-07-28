package com.androidvisualqa.pixels

import com.androidvisualqa.capture.api.CaptureFailure
import com.androidvisualqa.geometry.Rotation
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PixelCaptureRouterTest {

    private val now = Instant.parse("2026-07-28T10:00:00Z")
    private val samplePixels = CapturedPixels(
        displayId = 0,
        widthPx = 1080,
        heightPx = 2400,
        rotation = Rotation.ROTATION_0,
        rgba8888 = byteArrayOf(0, 0, 0, 0),
        capturedAt = now,
    )

    @Test
    fun `happy path window succeeds`() = runTest {
        val windowSource = PixelCaptureSource { request ->
            assertEquals(42L, request.windowId)
            PixelCaptureResult.Success(samplePixels)
        }
        val displaySource = DisplayCaptureSource { _ ->
            error("should not be called")
        }

        val router = PixelCaptureRouter(windowSource = windowSource, displaySource = displaySource)
        val result = router.capture(PixelCaptureRequest(displayId = 0, windowId = 42L))

        assertTrue(result is PixelCaptureResult.Success)
        assertEquals(samplePixels, (result as PixelCaptureResult.Success).pixels)
    }

    @Test
    fun `window unavailable falls through to display`() = runTest {
        var displayCalled = false
        val windowSource = PixelCaptureSource { _ ->
            PixelCaptureResult.Failure(CaptureFailure.WindowUnavailable)
        }
        val displaySource = DisplayCaptureSource { _ ->
            displayCalled = true
            PixelCaptureResult.Success(samplePixels)
        }

        val router = PixelCaptureRouter(windowSource = windowSource, displaySource = displaySource)
        val result = router.capture(PixelCaptureRequest(displayId = 0, windowId = 42L))

        assertTrue(displayCalled)
        assertTrue(result is PixelCaptureResult.Success)
    }

    @Test
    fun `no windowSource provided goes straight to display`() = runTest {
        var displayCalled = false
        val displaySource = DisplayCaptureSource { _ ->
            displayCalled = true
            PixelCaptureResult.Success(samplePixels)
        }

        val router = PixelCaptureRouter(windowSource = null, displaySource = displaySource)
        val result = router.capture(PixelCaptureRequest(displayId = 0, windowId = 42L))

        assertTrue(displayCalled)
        assertTrue(result is PixelCaptureResult.Success)
    }

    @Test
    fun `both fail returns window failure`() = runTest {
        val windowSource = PixelCaptureSource { _ ->
            PixelCaptureResult.Failure(CaptureFailure.WindowUnavailable)
        }
        val displaySource = DisplayCaptureSource { _ ->
            PixelCaptureResult.Failure(CaptureFailure.ScreenshotUnavailable)
        }

        val router = PixelCaptureRouter(windowSource = windowSource, displaySource = displaySource)
        val result = router.capture(PixelCaptureRequest(displayId = 0, windowId = 42L))

        assertTrue(result is PixelCaptureResult.Failure)
        assertEquals(CaptureFailure.WindowUnavailable, (result as PixelCaptureResult.Failure).reason)
    }

    @Test
    fun `window fails with non-window error still falls through but returns window failure`() = runTest {
        // If window fails with a non-WindowUnavailable error, the router still
        // falls through to display, but returns the window failure if display also fails
        val windowSource = PixelCaptureSource { _ ->
            PixelCaptureResult.Failure(CaptureFailure.PermissionDenied)
        }
        val displaySource = DisplayCaptureSource { _ ->
            PixelCaptureResult.Failure(CaptureFailure.ScreenshotUnavailable)
        }

        val router = PixelCaptureRouter(windowSource = windowSource, displaySource = displaySource)
        val result = router.capture(PixelCaptureRequest(displayId = 0, windowId = 42L))

        assertTrue(result is PixelCaptureResult.Failure)
        assertEquals(CaptureFailure.PermissionDenied, (result as PixelCaptureResult.Failure).reason)
    }
}
