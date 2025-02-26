package texturemanager

import ImageEntry
import java.awt.Image
import java.io.File
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ResourceManager {
    private val images = ConcurrentHashMap<String, ImageEntry>()
    private val textureCache = ConcurrentHashMap<String, BufferedImage>()

    fun addImage(name: String, path: String, image: Image): String {
        val id = "img_${System.currentTimeMillis()}_${images.size}" // Generate a unique ID
        images[id] = ImageEntry(name, path, image)
        return id
    }

    fun loadImageFromFile(file: File): ImageEntry? {
        try {
            val image = ImageIO.read(file)
            if (image != null) {
                return ImageEntry(file.name, file.absolutePath, image)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun loadTexturesFromDirectory(directory: File): List<ImageEntry> {
        val result = mutableListOf<ImageEntry>()
        if (!directory.isDirectory) return result

        val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp")

        directory.listFiles()?.forEach { file ->
            if (file.isFile && imageExtensions.contains(file.extension.lowercase(Locale.getDefault()))) {
                loadImageFromFile(file)?.let {
                    result.add(it)
                    // Also add to our map
                    addImage(it.name, it.path, it.image)
                }
            }
        }

        return result
    }

    fun getImage(id: String): ImageEntry? {
        return images[id]
    }

    fun getImageByName(name: String): ImageEntry? {
        return images.values.firstOrNull { it.name == name }
    }

    fun getAllImages(): Map<String, ImageEntry> {
        return images
    }

    // Get a cached texture or load it if needed
    fun getTexture(path: String): BufferedImage? {
        return textureCache.getOrPut(path) {
            try {
                ImageIO.read(File(path))
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }

    // Clear texture cache
    fun clearCache() {
        textureCache.clear()
    }
}