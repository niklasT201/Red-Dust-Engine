package render

import java.awt.*
import java.awt.image.BufferedImage

/**
 * Handles rendering of the sky - can be a solid color or an image (tiled or stretched)
 */
class SkyRenderer(
    var skyColor: Color,
    var skyImage: Image? = null,
    var tileImage: Boolean = false
) {
    private var cachedPattern: TexturePaint? = null
    private var lastImageWidth = 0
    private var lastImageHeight = 0

    /**
     * Renders the sky to the given graphics context
     */
    fun render(g: Graphics2D, width: Int, height: Int) {
        if (skyImage == null) {
            // Solid color mode
            g.color = skyColor
            g.fillRect(0, 0, width, height)
        } else {
            if (tileImage) {
                // Tiled image mode
                drawTiledImage(g, width, height)
            } else {
                // Stretched image mode
                g.drawImage(skyImage, 0, 0, width, height, null)
            }
        }
    }

    private fun drawTiledImage(g: Graphics2D, width: Int, height: Int) {
        val img = skyImage ?: return
        val imgWidth = img.getWidth(null)
        val imgHeight = img.getHeight(null)

        if (imgWidth <= 0 || imgHeight <= 0) return

        // Create a texture paint for efficient tiling
        if (cachedPattern == null || imgWidth != lastImageWidth || imgHeight != lastImageHeight) {
            // Convert Image to BufferedImage if needed
            val bufferedImage = if (img is BufferedImage) {
                img
            } else {
                val newImg = BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB)
                val g2 = newImg.createGraphics()
                g2.drawImage(img, 0, 0, null)
                g2.dispose()
                newImg
            }

            cachedPattern = TexturePaint(bufferedImage, Rectangle(0, 0, imgWidth, imgHeight))
            lastImageWidth = imgWidth
            lastImageHeight = imgHeight
        }

        // Use the texture paint for efficient tiling
        val oldPaint = g.paint
        g.paint = cachedPattern
        g.fillRect(0, 0, width, height)
        g.paint = oldPaint
    }
}