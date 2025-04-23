package texturemanager

import ImageEntry
import ObjectType
import ui.topbar.FileManager // Import FileManager
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
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.createDirectories

class ResourceManager(private val fileManager: FileManager) {
    private val images = ConcurrentHashMap<String, ImageEntry>()
    private val textureCache = ConcurrentHashMap<String, BufferedImage>()
    private val textureMetadata = ConcurrentHashMap<String, ObjectType>()

    // Function to get the project-specific textures directory
    private fun getTexturesDirectoryPath(): Path? {
        return fileManager.getAssetsDirectory()?.toPath()?.resolve("textures")
    }

    init {
        // Create textures directory if a project is loaded and it doesn't exist
        getTexturesDirectoryPath()?.let { path ->
            if (!path.exists()) {
                try {
                    path.createDirectories()
                    println("Created textures directory at: $path")
                } catch (e: Exception) {
                    System.err.println("Error creating textures directory at $path: ${e.message}")
                }
            } else if (!path.isDirectory()) {
                System.err.println("Error: Expected directory but found file at $path")
            }

            // Load metadata first (only if project exists)
            loadMetadataFromFile()

            // Load all textures from the project's textures directory on startup
            loadAllLocalTextures()
        } ?: println("ResourceManager init: No active project. Local texture loading skipped.")

    }

    // Helper to get the textures directory as a File, returns null if no project
    private fun getTexturesDirectoryFile(): File? {
        return getTexturesDirectoryPath()?.toFile()
    }

    fun addImage(name: String, path: String, image: Image): String {
        // Generate unique ID for the image
        val id = "img_${System.currentTimeMillis()}_${images.size}"

        // Try copy image to local storage ONLY if a project is active
        val localPath = getTexturesDirectoryPath()?.let {
            copyImageToLocalStorage(path, name)
        } ?: path // If no project, keep original path

        // Store the entry with the local path
        images[id] = ImageEntry(name, localPath, image)
        println("Added image: $name (ID: $id) - Path stored: $localPath")
        return id
    }

    /**
     * Copies an image file to the local textures directory OF THE CURRENT PROJECT.
     * Returns the path to the local copy, or the original path if copy fails or no project is active.
     */
    private fun copyImageToLocalStorage(originalPath: String, originalName: String): String {
        val texturesDir = getTexturesDirectoryPath() ?: return originalPath // No project, return original

        try {
            val sourceFile = File(originalPath)
            if (!sourceFile.exists() || !sourceFile.isFile) {
                println("Source file not found or not a file: $originalPath")
                return originalPath // Source doesn't exist
            }

            // Ensure textures directory exists
            if (!texturesDir.exists()) {
                texturesDir.createDirectories()
            }

            val extension = originalPath.substringAfterLast(".", "png")
            val baseName = originalName.substringBeforeLast(".")

            // Check if file with same base name exists (more robust check)
            val potentialExisting = texturesDir.resolve(sourceFile.name) // Check exact name first
            if (potentialExisting.exists()) {
                println("Texture already exists in project: ${potentialExisting}")
                return potentialExisting.toString()
            }

            // Use original filename if unique, otherwise add UUID
            var targetFileName = sourceFile.name
            var targetPath = texturesDir.resolve(targetFileName)
            var attempts = 0
            val maxAttempts = 10 // Prevent infinite loops in unlikely scenarios

            // Handle potential name collisions more robustly
            while (targetPath.exists() && attempts < maxAttempts) {
                println("Collision detected for ${targetFileName}. Attempting rename.")
                val uniqueID = UUID.randomUUID().toString().substring(0, 8)
                targetFileName = "${baseName}_${uniqueID}.$extension"
                targetPath = texturesDir.resolve(targetFileName)
                attempts++
            }

            if (targetPath.exists()) {
                System.err.println("Failed to find unique name for $originalName after $maxAttempts attempts.")
                return originalPath // Failed to create unique name
            }


            // Copy the file
            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING)
            println("Copied texture to project: $targetPath")

            return targetPath.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            System.err.println("Error copying image to project storage: ${e.message}")
            return originalPath // Return original path if copy fails
        }
    }

    /**
     * Loads an image from a file and adds it to the resource manager.
     * Copies the image to the project's texture directory if a project is active.
     */
    fun loadImageFromFile(file: File): ImageEntry? {
        // Only proceed if a project is active
        if (fileManager.getCurrentProjectName() == null) {
            println("Cannot load texture from file: No active project.")
            return null
        }

        try {
            val image = ImageIO.read(file)
            if (image != null) {
                // copyImageToLocalStorage handles the project check internally now
                val localPath = copyImageToLocalStorage(file.absolutePath, file.name)
                val entry = ImageEntry(file.name, localPath, image)
                // Add to our resource manager map (in-memory)
                val id = "img_${System.currentTimeMillis()}_${images.size}"
                images[id] = entry
                println("Loaded image from file: ${file.name}. Stored path: $localPath")
                return entry
            }
        } catch (e: Exception) {
            e.printStackTrace()
            System.err.println("Error loading image from file ${file.absolutePath}: ${e.message}")
        }
        return null
    }

    /**
     * Loads all textures from an external directory and adds them to the resource manager,
     * copying them to the project's texture directory.
     */
    fun loadTexturesFromDirectory(directory: File): List<ImageEntry> {
        // Only proceed if a project is active
        if (fileManager.getCurrentProjectName() == null) {
            println("Cannot load textures from directory: No active project.")
            return emptyList()
        }
        if (!directory.isDirectory) {
            println("Cannot load textures: Provided path is not a directory: ${directory.absolutePath}")
            return emptyList()
        }

        val result = mutableListOf<ImageEntry>()
        val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp")

        directory.listFiles()?.forEach { file ->
            if (file.isFile && imageExtensions.contains(file.extension.lowercase(Locale.getDefault()))) {
                // loadImageFromFile handles the copying and adding to the map
                loadImageFromFile(file)?.let {
                    result.add(it)
                }
            }
        }

        return result
    }

    /**
     * Loads all textures from the current project's local textures directory
     */
    private fun loadAllLocalTextures() {
        val directory = getTexturesDirectoryFile() ?: return // No project, nothing to load

        if (!directory.exists() || !directory.isDirectory) return

        println("Loading local textures from: ${directory.absolutePath}")
        val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp")
        var loadedCount = 0

        directory.listFiles()?.forEach { file ->
            if (file.isFile && imageExtensions.contains(file.extension.lowercase(Locale.getDefault()))) {
                try {
                    val image = ImageIO.read(file)
                    if (image != null) {
                        // Check if already loaded by path to avoid duplicates if metadata loaded it first
                        val existing = images.values.find { it.path == file.absolutePath }
                        if (existing == null) {
                            val id = "img_${System.currentTimeMillis()}_${images.size}_${UUID.randomUUID().toString().substring(0,4)}" // Make ID more unique
                            images[id] = ImageEntry(file.name, file.absolutePath, image)
                            loadedCount++
                            // println("Loaded local texture: ${file.name} (ID: $id)")
                        } else {
                            // println("Skipping local texture already loaded: ${file.name}")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    System.err.println("Error loading local texture ${file.absolutePath}: ${e.message}")
                }
            }
        }
        println("Finished loading $loadedCount local textures.")
    }

    fun getImage(id: String): ImageEntry? {
        return images[id]
    }

    fun getImageByName(name: String): ImageEntry? {
        // Prioritize textures from the current project's directory if multiple have same name
        val projectTextureDir = getTexturesDirectoryPath()
        return images.values.firstOrNull {
            it.name == name && (projectTextureDir == null || Paths.get(it.path).startsWith(projectTextureDir))
        } ?: images.values.firstOrNull { it.name == name } // Fallback to any match
    }


    fun getAllImages(): Map<String, ImageEntry> {
        return images.toMap() // Return a copy
    }

    /**
     * Gets a cached texture or loads it from the path (tries project dir first).
     */
    fun getTexture(path: String): BufferedImage? {
        return textureCache.getOrPut(path) {
            try {
                var fileToLoad = File(path)

                // If the path isn't absolute or doesn't exist, check the project's texture dir
                if (!fileToLoad.isAbsolute || !fileToLoad.exists()) {
                    getTexturesDirectoryFile()?.let { texDir ->
                        val potentialFile = File(texDir, fileToLoad.name) // Look for file by name in project dir
                        if (potentialFile.exists()) {
                            fileToLoad = potentialFile
                            // println("Texture found in project directory: ${fileToLoad.absolutePath}")
                        }
                    }
                }

                if (fileToLoad.exists() && fileToLoad.isFile) {
                    // println("Loading texture: ${fileToLoad.absolutePath}")
                    ImageIO.read(fileToLoad)
                } else {
                    System.err.println("Texture file not found: $path (and not found in project textures)")
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                System.err.println("Error reading texture file $path: ${e.message}")
                null
            }
        }
    }

    fun removeImage(id: String): Boolean {
        val imageEntry = images[id] ?: return false
        val texturesDir = getTexturesDirectoryPath() ?: run {
            println("Cannot remove image file: No active project.")
            // Still remove from memory caches
            images.remove(id)
            textureCache.remove(imageEntry.path)
            textureMetadata.remove(imageEntry.path) // Remove metadata too
            return true // Indicate removal from manager, even if file wasn't touched
        }

        try {
            // Remove from memory
            images.remove(id)

            // Remove from cache if it exists
            textureCache.remove(imageEntry.path)
            val removedMeta = textureMetadata.remove(imageEntry.path) // Remove metadata too

            // Delete the physical file ONLY if it's within the project's textures directory
            val file = File(imageEntry.path)
            if (file.exists() && file.isFile && file.toPath().startsWith(texturesDir)) {
                Files.delete(file.toPath())
                println("Deleted texture file: ${file.absolutePath}")
                // If metadata was associated, save the updated metadata file
                if (removedMeta != null) {
                    saveMetadataToFile()
                }
                return true
            } else if (file.exists() && !file.toPath().startsWith(texturesDir)) {
                println("Texture file ${file.absolutePath} is outside the project directory. Not deleting file, removing entry only.")
                return true // Removed from manager successfully
            } else {
                println("Texture file not found or already deleted: ${imageEntry.path}")
                return true // Removed from manager successfully
            }
        } catch (e: Exception) {
            e.printStackTrace()
            System.err.println("Failed to delete texture file: ${e.message}")
            // Attempt to put it back in the map if deletion failed? Maybe not.
            // Assume it's gone from the manager even if file deletion fails.
        }

        return false // Should ideally not be reached if exceptions are caught
    }

    // Clear texture cache
    fun clearCache() {
        textureCache.clear()
    }

    // Save texture's object type association
    fun saveTextureObjectType(imagePath: String, objectType: ObjectType) {
        textureMetadata[imagePath] = objectType
        saveMetadataToFile() // Save whenever metadata changes
    }

    // Get texture's associated object type
    fun getTextureObjectType(imagePath: String): ObjectType? {
        return textureMetadata[imagePath]
    }

    private fun getMetadataFilePath(): Path? {
        return getTexturesDirectoryPath()?.resolve("texture_metadata.properties")
    }

    private fun saveMetadataToFile() {
        val metadataFile = getMetadataFilePath()?.toFile() ?: return // No project, nowhere to save

        try {
            val properties = Properties()
            val texturesDir = getTexturesDirectoryPath() // Get base path for relative check

            textureMetadata.forEach { (path, type) ->
                // Store filename relative to textures dir if possible, otherwise full path/name
                val file = File(path)
                val filename = if (texturesDir != null && file.toPath().startsWith(texturesDir)) {
                    texturesDir.relativize(file.toPath()).toString() // Store relative path
                } else {
                    file.name // Fallback to just the name if outside project
                }
                properties.setProperty(filename, type.name)
            }

            FileOutputStream(metadataFile).use { outputStream ->
                properties.store(outputStream, "Texture object type associations (Paths relative to project textures dir if possible)")
            }
            // println("Saved texture metadata to ${metadataFile.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            System.err.println("Error saving texture metadata: ${e.message}")
        }
    }

    // Load metadata from file
    private fun loadMetadataFromFile() {
        val metadataFile = getMetadataFilePath()?.toFile() ?: return // No project, nothing to load
        val texturesDir = getTexturesDirectoryFile() ?: return // Need this to resolve relative paths

        if (!metadataFile.exists()) {
            //println("Metadata file not found: ${metadataFile.absolutePath}")
            return
        }

        println("Loading texture metadata from: ${metadataFile.absolutePath}")
        try {
            val properties = Properties()
            FileInputStream(metadataFile).use { inputStream ->
                properties.load(inputStream)
            }

            val loadedMeta = ConcurrentHashMap<String, ObjectType>()

            properties.forEach { key, value ->
                val pathOrName = key.toString()
                val typeName = value.toString()

                // Try to resolve the path relative to the textures directory first
                val absolutePath = Paths.get(texturesDir.absolutePath, pathOrName).normalize()
                val file = absolutePath.toFile()

                if (file.exists()) {
                    try {
                        val objectType = ObjectType.valueOf(typeName)
                        loadedMeta[file.absolutePath] = objectType
                        // Optionally pre-load the image entry if not already loaded
                        if (!images.values.any { it.path == file.absolutePath }) {
                            try {
                                val image = ImageIO.read(file)
                                if (image != null) {
                                    val id = "meta_${System.currentTimeMillis()}_${images.size}"
                                    images[id] = ImageEntry(file.name, file.absolutePath, image)
                                    // println("Pre-loaded image from metadata: ${file.name}")
                                }
                            } catch (readEx: Exception) {
                                System.err.println("Error pre-loading image from metadata ref ${file.absolutePath}: ${readEx.message}")
                            }
                        }
                    } catch (e: IllegalArgumentException) {
                        System.err.println("Warning: Unknown object type '$typeName' in metadata for '$pathOrName'")
                    }
                } else {
                    // If relative path fails, maybe it was stored as just a name? Look for it.
                    val foundFile = texturesDir.listFiles { f -> f.name == pathOrName }?.firstOrNull()
                    if (foundFile != null) {
                        try {
                            val objectType = ObjectType.valueOf(typeName)
                            loadedMeta[foundFile.absolutePath] = objectType
                            if (!images.values.any { it.path == foundFile.absolutePath }) {
                                try {
                                    val image = ImageIO.read(foundFile)
                                    if (image != null) {
                                        val id = "meta_${System.currentTimeMillis()}_${images.size}"
                                        images[id] = ImageEntry(foundFile.name, foundFile.absolutePath, image)
                                        //println("Pre-loaded image from metadata (name match): ${foundFile.name}")
                                    }
                                } catch (readEx: Exception) {
                                    System.err.println("Error pre-loading image from metadata ref (name match) ${foundFile.absolutePath}: ${readEx.message}")
                                }
                            }
                        } catch (e: IllegalArgumentException) {
                            System.err.println("Warning: Unknown object type '$typeName' in metadata for name match '$pathOrName'")
                        }
                    } else {
                        System.err.println("Warning: Could not find texture file referenced in metadata: '$pathOrName' (resolved as '${file.absolutePath}')")
                    }
                }
            }
            // Replace the current metadata map atomically
            textureMetadata.clear()
            textureMetadata.putAll(loadedMeta)
            println("Finished loading ${textureMetadata.size} metadata entries.")

        } catch (e: Exception) {
            e.printStackTrace()
            System.err.println("Error loading texture metadata: ${e.message}")
        }
    }
}