package player.uis.components

import player.uis.UIComponent
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

// A standalone image component that can be placed anywhere
class ImageComponent(x: Int, y: Int, width: Int, height: Int) : UIComponent(x, y, width, height) {
    private var image: BufferedImage? = null
    var imageType: String = "face" // Default to face, could be "custom" for user images
    var scale: Double = 1.0
    var preserveAspectRatio: Boolean = true
    var imagePath: String = "" // Path to custom image file

    init {
        updateImage()
    }

    fun updateImage() {
        image = when {
            imageType == "custom" && imagePath.isNotEmpty() -> {
                try {
                    ImageIO.read(File(imagePath))
                } catch (e: Exception) {
                    createFaceImage(64) // Fallback if loading fails
                }
            }
            imageType == "face" -> createFaceImage(64)
            // Add other image types as needed
            else -> createFaceImage(64) // Default
        }
    }

    private fun createFaceImage(size: Int): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)

        // Face background
        g.color = Color(231, 181, 131) // Skin tone
        g.fillOval(2, 2, size-4, size-4)

        // Black border
        g.color = Color.BLACK
        g.drawOval(2, 2, size-4, size-4)

        // Eyes
        g.color = Color.WHITE
        g.fillOval(size/4, size/3, size/5, size/6)
        g.fillOval(size*9/16, size/3, size/5, size/6)

        g.color = Color(30, 50, 210) // Blue eyes
        g.fillOval(size*9/32, size*11/30, size/8, size/8)
        g.fillOval(size*19/32, size*11/30, size/8, size/8)

        // Mouth
        g.color = Color(180, 50, 50) // Reddish mouth
        g.fillArc(size/3, size*19/32, size/3, size/5, 0, 180)

        g.dispose()
        return image
    }

    override fun render(g2: Graphics2D, screenWidth: Int, screenHeight: Int) {
        if (!visible) return

        image?.let {
            if (preserveAspectRatio) {
                val drawWidth = (it.width * scale).toInt()
                val drawHeight = (it.height * scale).toInt()
                g2.drawImage(it, x, y, drawWidth, drawHeight, null)
            } else {
                // Use component's width and height directly
                g2.drawImage(it, x, y, width, height, null)
            }
        }
    }

    override fun clone(): UIComponent {
        val clone = ImageComponent(x, y, width, height)
        clone.visible = visible
        clone.id = id
        clone.imageType = imageType
        clone.scale = scale
        clone.preserveAspectRatio = preserveAspectRatio
        clone.imagePath = imagePath
        clone.updateImage()
        return clone
    }
}