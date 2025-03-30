package render.filter

import java.awt.Color
import java.awt.image.BufferedImage

class BlackAndWhiteFilter : RenderFilter {
    override val name: String = "Black & White"

    override fun apply(image: BufferedImage): BufferedImage {
        val width = image.width
        val height = image.height
        val result = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val rgb = image.getRGB(x, y)
                val color = Color(rgb)

                // Convert to grayscale using standard luminance formula
                val gray = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue).toInt()
                val grayColor = Color(gray, gray, gray)

                result.setRGB(x, y, grayColor.rgb)
            }
        }

        return result
    }
}