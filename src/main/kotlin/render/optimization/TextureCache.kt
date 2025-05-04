package render.optimization

import ImageEntry
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap

object TextureCache {
    private val transformedTextures = ConcurrentHashMap<String, BufferedImage>()
    private const val MAX_CACHE_SIZE = 100

    // Get a cached texture or create and cache a new one
    fun getTransformedTexture(
        textureEntry: ImageEntry,
        transformParams: String // Unique identifier for this transformation
    ): BufferedImage {
        val cacheKey = "${textureEntry.name}:$transformParams"

        return transformedTextures.getOrPut(cacheKey) {
            // Manage cache size
            if (transformedTextures.size >= MAX_CACHE_SIZE) {
                // Remove a random entry if cache is full
                val keyToRemove = transformedTextures.keys.firstOrNull()
                keyToRemove?.let { transformedTextures.remove(it) }
            }

            // Create and return the transformed texture
            createTransformedTexture(textureEntry)
        }
    }

    // Create a transformed texture from the original
    private fun createTransformedTexture(textureEntry: ImageEntry): BufferedImage {
        // Get the original image
        val srcImage = textureEntry.image

        // Convert to BufferedImage if not already
        val bufferedImage = if (srcImage is BufferedImage) {
            srcImage
        } else {
            val bImg = BufferedImage(
                srcImage.getWidth(null),
                srcImage.getHeight(null),
                BufferedImage.TYPE_INT_ARGB
            )
            val g = bImg.createGraphics()
            g.drawImage(srcImage, 0, 0, null)
            g.dispose()
            bImg
        }

        return bufferedImage
    }

    // Clear the cache
    fun clearCache() {
        transformedTextures.clear()
    }
}