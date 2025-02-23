package ui

import ImageEntry
import java.awt.Image

class ResourceManager {
    private val images = mutableMapOf<String, ImageEntry>()

    fun addImage(name: String, path: String, image: Image): String {
        val id = "img_${images.size}" // Generate a unique ID
        images[id] = ImageEntry(name, path, image)
        return id
    }

    fun getImage(id: String): ImageEntry? {
        return images[id]
    }

    fun getAllImages(): Map<String, ImageEntry> {
        return images
    }
}