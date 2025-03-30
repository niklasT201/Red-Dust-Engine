package render.filter

import java.awt.image.BufferedImage

interface RenderFilter {
    fun apply(image: BufferedImage): BufferedImage
    val name: String
}