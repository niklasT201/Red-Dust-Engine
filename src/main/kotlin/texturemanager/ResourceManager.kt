package texturemanager

import ImageEntry
import ObjectType
import java.awt.Image
import java.io.File
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ResourceManager {
    private val images = ConcurrentHashMap<String, ImageEntry>()
    private val textureCache = ConcurrentHashMap<String, BufferedImage>()
    private val textureMetadata = ConcurrentHashMap<String, ObjectType>()

    // Directory where textures will be stored locally
    private val texturesDirectory = "assets/textures"

    init {
        // Create textures directory if it doesn't exist
        val directory = File(texturesDirectory)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        // Load metadata first
        loadMetadataFromFile()

        // Load all textures from the textures directory on startup
        loadAllLocalTextures()
    }

    fun addImage(name: String, path: String, image: Image): String {
        // Generate unique ID for the image
        val id = "img_${System.currentTimeMillis()}_${images.size}"

        // Copy the image to our local textures directory
        val localPath = copyImageToLocalStorage(path, name)

        // Store the entry with the local path
        images[id] = ImageEntry(name, localPath, image)
        return id
    }

    /**
     * Copies an image file to the local textures directory
     * @return The path to the local copy
     */
    private fun copyImageToLocalStorage(originalPath: String, originalName: String): String {
        try {
            val sourceFile = File(originalPath)
            if (!sourceFile.exists()) return originalPath

            // Extract file extension and base name
            val extension = originalPath.substringAfterLast(".", "png")
            val baseName = originalName.substringBeforeLast(".")

            // Check if file with same base name exists
            val directory = File(texturesDirectory)
            val existingFiles = directory.listFiles { file ->
                file.nameWithoutExtension.startsWith(baseName) &&
                        file.extension.lowercase() == extension.lowercase()
            }

            if (existingFiles?.isNotEmpty() == true) {
                // Return the path to the first matching file
                return existingFiles[0].absolutePath
            }

            // If no matching file exists, create with a unique ID instead of timestamp
            val uniqueID = UUID.randomUUID().toString().substring(0, 8)
            val uniqueFileName = "${baseName}_${uniqueID}.$extension"
            val targetPath = Paths.get(texturesDirectory, uniqueFileName)

            // Copy the file
            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING)

            return targetPath.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            // Return original path if copy fails
            return originalPath
        }
    }

    /**
     * Loads an image from a file and adds it to the resource manager
     */
    fun loadImageFromFile(file: File): ImageEntry? {
        try {
            val image = ImageIO.read(file)
            if (image != null) {
                val localPath = copyImageToLocalStorage(file.absolutePath, file.name)
                val entry = ImageEntry(file.name, localPath, image)
                // Add to our resource manager
                addImage(entry.name, entry.path, entry.image)
                return entry
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Loads all textures from a directory and adds them to the resource manager
     */
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

    /**
     * Loads all textures from the local textures directory
     */
    private fun loadAllLocalTextures() {
        val directory = File(texturesDirectory)
        if (!directory.exists()) return

        val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp")

        directory.listFiles()?.forEach { file ->
            if (file.isFile && imageExtensions.contains(file.extension.lowercase(Locale.getDefault()))) {
                try {
                    val image = ImageIO.read(file)
                    if (image != null) {
                        val id = "img_${System.currentTimeMillis()}_${images.size}"
                        images[id] = ImageEntry(file.name, file.absolutePath, image)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
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

    /**
     * Gets a cached texture or loads it from the local path if needed
     */
    fun getTexture(path: String): BufferedImage? {
        return textureCache.getOrPut(path) {
            try {
                // First try to load directly from the path
                val file = File(path)
                if (file.exists()) {
                    ImageIO.read(file)
                } else {
                    // If not found, try to find it in the textures directory
                    val fileName = path.substringAfterLast(File.separator)
                    val localFile = File(texturesDirectory, fileName)
                    if (localFile.exists()) {
                        ImageIO.read(localFile)
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun removeImage(id: String): Boolean {
        val imageEntry = images[id] ?: return false

        try {
            // Remove from memory
            images.remove(id)

            // Remove from cache if it exists
            textureCache.remove(imageEntry.path)

            // Delete the physical file
            val file = File(imageEntry.path)
            if (file.exists() && file.isFile) {
                Files.delete(file.toPath())
                println("Deleted texture file: ${file.absolutePath}")
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to delete texture file: ${e.message}")
        }

        return false
    }

    // Clear texture cache
    fun clearCache() {
        textureCache.clear()
    }

    // Save texture's object type association
    fun saveTextureObjectType(imagePath: String, objectType: ObjectType) {
        textureMetadata[imagePath] = objectType
        saveMetadataToFile()
    }

    // Get texture's associated object type
    fun getTextureObjectType(imagePath: String): ObjectType? {
        return textureMetadata[imagePath]
    }

    // Save metadata to a file
    private fun saveMetadataToFile() {
        try {
            val metadataFile = File("$texturesDirectory/texture_metadata.properties")
            val properties = Properties()

            textureMetadata.forEach { (path, type) ->
                // Store just the filename part, not the full path
                val filename = File(path).name
                properties.setProperty(filename, type.name)
            }

            properties.store(FileOutputStream(metadataFile), "Texture object type associations")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Load metadata from file
    private fun loadMetadataFromFile() {
        try {
            val metadataFile = File("$texturesDirectory/texture_metadata.properties")
            if (metadataFile.exists()) {
                val properties = Properties()
                properties.load(FileInputStream(metadataFile))

                properties.forEach { (filename, typeName) ->
                    // Find the full path for the filename
                    val files = File(texturesDirectory).listFiles { file ->
                        file.name == filename
                    }

                    if (files?.isNotEmpty() == true) {
                        try {
                            val objectType = ObjectType.valueOf(typeName.toString())
                            textureMetadata[files[0].absolutePath] = objectType
                        } catch (e: IllegalArgumentException) {
                            // Handle case where the saved type doesn't exist anymore
                            println("Warning: Unknown object type '$typeName' for texture '$filename'")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}