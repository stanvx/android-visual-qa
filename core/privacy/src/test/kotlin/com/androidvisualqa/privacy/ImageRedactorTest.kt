package com.androidvisualqa.privacy

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ImageRedactorTest {

    private val redactor = ImageRedactor()

    /**
     * Creates a solid-color PNG as a byte array for testing.
     */
    private fun createTestPng(width: Int, height: Int, color: Int = Color.WHITE.rgb): ByteArray {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                img.setRGB(x, y, color)
            }
        }
        val baos = ByteArrayOutputStream()
        ImageIO.write(img, "png", baos)
        return baos.toByteArray()
    }

    /**
     * Reads a PNG byte array back into a [BufferedImage] for assertion.
     */
    private fun decodePng(bytes: ByteArray): BufferedImage {
        return ImageIO.read(java.io.ByteArrayInputStream(bytes))
    }

    @Test
    fun `redact bottom-right quadrant of 4x4 white PNG`() {
        val png = createTestPng(4, 4, Color.WHITE.rgb)
        val regions = listOf(
            RedactionRegion(
                left = 0.5, top = 0.5, right = 1.0, bottom = 1.0,
                sensitivity = Sensitivity.Pii,
                reason = "Test redaction"
            )
        )

        val result = redactor.redact(png, regions, canvasWidth = 4.0, canvasHeight = 4.0)
        val img = decodePng(result)

        assertEquals(4, img.width)
        assertEquals(4, img.height)

        // Top-left quadrant: should stay white
        assertEquals(Color.WHITE.rgb, img.getRGB(0, 0))
        assertEquals(Color.WHITE.rgb, img.getRGB(1, 0))
        assertEquals(Color.WHITE.rgb, img.getRGB(1, 1))

        // Bottom-right quadrant: should be black
        val blackRgb = 0xFF000000.toInt()
        assertEquals(blackRgb, img.getRGB(2, 2))
        assertEquals(blackRgb, img.getRGB(3, 3))
        assertEquals(blackRgb, img.getRGB(2, 3))
        assertEquals(blackRgb, img.getRGB(3, 2))
    }

    @Test
    fun `out-of-bounds region is clipped not rejected`() {
        // Use a canvas wider than the image to trigger pixel-level clipping
        val png = createTestPng(2, 2, Color.WHITE.rgb)
        val regions = listOf(
            RedactionRegion(
                left = 0.5, top = 0.5, right = 1.0, bottom = 1.0,
                sensitivity = Sensitivity.Pii,
                reason = "Clipped region"
            )
        )

        // Canvas is 4x4 but image is 2x2 — redactor clips pixel coords to image bounds
        val result = redactor.redact(png, regions, canvasWidth = 4.0, canvasHeight = 4.0)
        val img = decodePng(result)
        assertEquals(2, img.width)
        assertEquals(2, img.height)
        // Pixel (0,0) is top-left of image, half of canvas => pixel (1,1) is the only one covered
        val blackRgb = 0xFF000000.toInt()
        assertEquals(Color.WHITE.rgb, img.getRGB(0, 0))
        assertEquals(blackRgb, img.getRGB(1, 1))
    }

    @Test
    fun `invalid PNG throws IllegalArgumentException`() {
        val badBytes = byteArrayOf(0x00, 0x01, 0x02)
        assertThrows(IllegalArgumentException::class.java) {
            redactor.redact(badBytes, emptyList(), canvasWidth = 100.0, canvasHeight = 100.0)
        }
    }
}
