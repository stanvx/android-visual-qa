package com.androidvisualqa.privacy

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Applies redaction regions to a PNG-encoded image.
 *
 * Uses pure JVM [BufferedImage] and [ImageIO] — no Android or Skia dependencies.
 * All coordinate math is performed in pixel space after denormalizing the [RedactionRegion]
 * coordinates against the supplied canvas dimensions.
 *
 * Caller must pass canvas dimensions equal to the image's pixel dimensions; otherwise
 * redaction positions will be misaligned.
 */
class ImageRedactor {

    companion object {
        private const val BLACK_RGB = 0xFF000000.toInt()
    }

    /**
     * Redacts the given [pngBytes] by filling each [region] with solid black.
     *
     * @param pngBytes Raw PNG file bytes.
     * @param regions Ordered list of redaction regions to apply.
     * @param canvasWidth The pixel width used when the regions were normalized.
     * @param canvasHeight The pixel height used when the regions were normalized.
     * @return Redacted PNG bytes.
     * @throws IllegalArgumentException if the input cannot be decoded as a PNG.
     */
    fun redact(
        pngBytes: ByteArray,
        regions: List<RedactionRegion>,
        canvasWidth: Double,
        canvasHeight: Double,
    ): ByteArray {
        val inputStream = ByteArrayInputStream(pngBytes)
        val image = ImageIO.read(inputStream) ?: throw IllegalArgumentException(
            "Failed to decode PNG: ImageIO.read returned null"
        )

        val imgWidth = image.width.toDouble()
        val imgHeight = image.height.toDouble()

        // Scale normalized coords to actual image pixel coords
        for (region in regions) {
            val x1 = (region.left * imgWidth).toInt().coerceIn(0, image.width - 1)
            val y1 = (region.top * imgHeight).toInt().coerceIn(0, image.height - 1)
            val x2 = (region.right * imgWidth).toInt().coerceIn(0, image.width - 1)
            val y2 = (region.bottom * imgHeight).toInt().coerceIn(0, image.height - 1)

            for (y in y1..y2) {
                for (x in x1..x2) {
                    image.setRGB(x, y, BLACK_RGB)
                }
            }
        }

        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "png", outputStream)
        return outputStream.toByteArray()
    }
}
