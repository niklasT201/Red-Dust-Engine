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

        dumpResourceState()

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
            val normalizedAbsPath = targetPath.toAbsolutePath().normalize().toString() // <-- Normalize
            println("Copied texture to project: $normalizedAbsPath") // <-- Log normalized path

            return normalizedAbsPath // <-- Return normalized path
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
            println("DEBUG: Cannot load texture from file: No active project.")
            return null
        }

        try {
            println("Loading image from: ${file.absolutePath}")
            val image = ImageIO.read(file)
            if (image != null) {
                val localPath = copyImageToLocalStorage(file.absolutePath, file.name) // This now returns normalized path
                // We need the image entry to store the normalized path regardless if copy happened or not
                val finalPath = Paths.get(localPath).toAbsolutePath().normalize().toString() // Ensure it's absolute normalized
                val entry = ImageEntry(file.name, finalPath, image) // <-- Use finalPath
                val id = "img_${System.currentTimeMillis()}_${images.size}"
                images[id] = entry
                println("Successfully loaded image: ${file.name}")
                println("  - Original path: ${file.absolutePath}")
                println("  - Stored path: $finalPath") // <-- Log finalPath
                println("  - Size: ${image.width}x${image.height}")
                return entry
            } else {
                println("ERROR: ImageIO returned null for ${file.name}")
            }
        } catch (e: Exception) {
            System.err.println("ERROR loading ${file.name}: ${e.message}")
            e.printStackTrace()
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

    private fun dumpResourceState() {
        println("\n==== RESOURCE MANAGER DEBUG STATE ====")
        println("Total images in memory: ${images.size}")
        println("Total textures in cache: ${textureCache.size}")
        println("Total texture metadata entries: ${textureMetadata.size}")

        // Group by ObjectType to see distribution
        val typeCount = mutableMapOf<ObjectType, Int>()
        textureMetadata.values.forEach { type ->
            typeCount[type] = typeCount.getOrDefault(type, 0) + 1
        }

        println("Texture type distribution:")
        typeCount.forEach { (type, count) ->
            println("  ${type.name}: $count textures")
        }

        // Check for mismatches between collections
        val pathsInImages = images.values.map { it.path }.toSet()
        val pathsInMetadata = textureMetadata.keys.toSet()

        val inImagesNotMetadata = pathsInImages - pathsInMetadata
        val inMetadataNotImages = pathsInMetadata - pathsInImages

        if (inImagesNotMetadata.isNotEmpty()) {
            println("\nWARNING: ${inImagesNotMetadata.size} images don't have metadata:")
            inImagesNotMetadata.take(10).forEach { println("  - $it") }
            if (inImagesNotMetadata.size > 10) println("  ... and ${inImagesNotMetadata.size - 10} more")
        }

        if (inMetadataNotImages.isNotEmpty()) {
            println("\nWARNING: ${inMetadataNotImages.size} metadata entries don't have corresponding images:")
            inMetadataNotImages.take(10).forEach { println("  - $it") }
            if (inMetadataNotImages.size > 10) println("  ... and ${inMetadataNotImages.size - 10} more")
        }

        println("=============================\n")
    }


    /**
     * Loads all textures from the current project's local textures directory
     */
    private fun loadAllLocalTextures() {
        val directory = getTexturesDirectoryFile() ?: return

        if (!directory.exists() || !directory.isDirectory) {
            println("ERROR: Textures directory doesn't exist or isn't a directory: ${directory.absolutePath}")
            return
        }

        println("\nLoading local textures from: ${directory.absolutePath}")
        val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp")
        var loadedCount = 0
        var skippedCount = 0
        var errorCount = 0
        val files = directory.listFiles() ?: emptyArray()

        println("Found ${files.count { it.isFile && imageExtensions.contains(it.extension.lowercase()) }} image files")

        files.forEach { file ->
            if (file.isFile && imageExtensions.contains(file.extension.lowercase(Locale.getDefault()))) {
                try {
                    val image = ImageIO.read(file)
                    if (image != null) {
                        val normalizedAbsPath = file.toPath().toAbsolutePath().normalize().toString() // <-- Normalize
                        // Check if already loaded using the normalized path
                        val existing = images.values.find { it.path == normalizedAbsPath }
                        if (existing == null) {
                            val id = "img_${System.currentTimeMillis()}_${images.size}_${UUID.randomUUID().toString().substring(0,4)}"
                            images[id] = ImageEntry(file.name, normalizedAbsPath, image) // <-- Use normalized path
                            loadedCount++
                            println("  Loaded: ${file.name} (ID: $id) Path: $normalizedAbsPath") // <-- Log normalized path
                        } else {
                            skippedCount++
                            // Optional: Update existing entry's image data if needed?
                            println("  Skipped: ${file.name} (already loaded with path ${existing.path})")
                        }
                    } else {
                        errorCount++
                        System.err.println("  ERROR: Could not read image data from ${file.name}")
                    }
                } catch (e: Exception) {
                    errorCount++
                    System.err.println("  ERROR loading texture ${file.name}: ${e.message}")
                }
            }
        }

        println("Finished loading local textures:")
        println("  - Loaded: $loadedCount")
        println("  - Skipped: $skippedCount")
        println("  - Errors: $errorCount")
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
        // Check cache first
        val cached = textureCache[path]
        if (cached != null) {
            return cached
        }

        try {
            println("Attempting to load texture: $path")
            var fileToLoad = File(path)

            // Path resolution logic
            if (!fileToLoad.isAbsolute || !fileToLoad.exists()) {
                println("  File not found at path, checking project textures...")
                getTexturesDirectoryFile()?.let { texDir ->
                    val potentialFile = File(texDir, fileToLoad.name)
                    if (potentialFile.exists()) {
                        fileToLoad = potentialFile
                        println("  Found in project directory: ${fileToLoad.absolutePath}")
                    } else {
                        println("  Not found in project directory either")
                    }
                }
            }

            if (fileToLoad.exists() && fileToLoad.isFile) {
                println("  Loading texture from: ${fileToLoad.absolutePath}")
                val image = ImageIO.read(fileToLoad)
                if (image != null) {
                    println("  Successfully loaded: ${fileToLoad.name} (${image.width}x${image.height})")
                    textureCache[path] = image
                    return image
                } else {
                    System.err.println("  ERROR: ImageIO returned null for ${fileToLoad.name}")
                }
            } else {
                System.err.println("  ERROR: Texture file not found: $path")
            }
        } catch (e: Exception) {
            System.err.println("  ERROR reading texture file $path: ${e.message}")
            e.printStackTrace()
        }
        return null
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
                val file = File(path) // path is the absolute normalized key
                val keyToSave: String
                if (texturesDir != null && file.toPath().startsWith(texturesDir)) {
                    // Store relative path if inside project textures dir
                    keyToSave = texturesDir.relativize(file.toPath()).toString().replace('\\', '/') // Use forward slashes for consistency
                } else {
                    // Fallback to absolute path (or just name?) if outside. Let's use absolute for now.
                    keyToSave = path // Store the absolute normalized path
                    println("Warning: Saving absolute path for texture outside project: $path")
                }
                properties.setProperty(keyToSave, type.name)
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
        val metadataFile = getMetadataFilePath()?.toFile() ?: return
        val texturesDir = getTexturesDirectoryFile() ?: return

        if (!metadataFile.exists()) {
            println("DEBUG: Metadata file not found: ${metadataFile.absolutePath}")
            return
        }

        println("\nLoading texture metadata from: ${metadataFile.absolutePath}")
        try {
            val properties = Properties()
            FileInputStream(metadataFile).use { inputStream ->
                properties.load(inputStream)
            }

            println("Loaded ${properties.size} entries from metadata file")
            val loadedMeta = ConcurrentHashMap<String, ObjectType>()
            var successCount = 0
            var failedCount = 0
            var missingFileCount = 0

            properties.forEach { key, value ->
                val pathOrName = key.toString()
                val typeName = value.toString()

                try {
                    // First try absolute path resolution
                    val absolutePath = Paths.get(texturesDir.absolutePath, pathOrName).normalize()
                    val file = absolutePath.toFile()

                    if (file.exists()) {
                        try {
                            val objectType = ObjectType.valueOf(typeName)
                            loadedMeta[file.absolutePath] = objectType

                            // Pre-load image if needed
                            if (!images.values.any { it.path == file.absolutePath }) {
                                try {
                                    val image = ImageIO.read(file)
                                    if (image != null) {
                                        val id = "meta_${System.currentTimeMillis()}_${images.size}"
                                        images[id] = ImageEntry(file.name, file.absolutePath, image)
                                        println("  Loaded from metadata: ${file.name} (type: $objectType)")
                                        successCount++
                                    } else {
                                        println("  WARNING: Could not read image data from ${file.name}")
                                        failedCount++
                                    }
                                } catch (readEx: Exception) {
                                    println("  ERROR loading ${file.name}: ${readEx.message}")
                                    failedCount++
                                }
                            } else {
                                println("  Set metadata for already loaded: ${file.name} (type: $objectType)")
                                successCount++
                            }
                        } catch (e: IllegalArgumentException) {
                            println("  WARNING: Unknown object type '$typeName' for '${file.name}'")
                            failedCount++
                        }
                    } else {
                        // Try by name match
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
                                            println("  Loaded from metadata (by name): ${foundFile.name} (type: $objectType)")
                                            successCount++
                                        } else {
                                            println("  WARNING: Could not read image data from ${foundFile.name}")
                                            failedCount++
                                        }
                                    } catch (readEx: Exception) {
                                        println("  ERROR loading ${foundFile.name}: ${readEx.message}")
                                        failedCount++
                                    }
                                } else {
                                    println("  Set metadata for already loaded (by name): ${foundFile.name} (type: $objectType)")
                                    successCount++
                                }
                            } catch (e: IllegalArgumentException) {
                                println("  WARNING: Unknown object type '$typeName' for name match '${foundFile.name}'")
                                failedCount++
                            }
                        } else {
                            println("  WARNING: Missing texture file: '$pathOrName'")
                            missingFileCount++
                        }
                    }
                } catch (e: Exception) {
                    println("  ERROR processing metadata entry '$pathOrName': ${e.message}")
                    failedCount++
                }
            }

            // Replace metadata map
            textureMetadata.clear()
            textureMetadata.putAll(loadedMeta)

            println("Metadata loading complete:")
            println("  - Success: $successCount")
            println("  - Failed: $failedCount")
            println("  - Missing files: $missingFileCount")

        } catch (e: Exception) {
            System.err.println("ERROR loading texture metadata: ${e.message}")
            e.printStackTrace()
        }
    }

    // Inside ResourceManager class
    fun reloadProjectAssets() {
        println("==== Reloading Project Assets ====")
        // Consider a more nuanced merge if needed later.
        images.clear() // Might be too aggressive? Maybe only clear project-related ones?
        textureCache.clear()
        textureMetadata.clear()

        // Re-create textures directory if needed (should exist if project loaded)
        getTexturesDirectoryPath()?.let { path ->
            if (!path.exists()) {
                try {
                    path.createDirectories()
                } catch (e: Exception) { /* handle error */ }
            }

            // Reload metadata first using the current project context
            loadMetadataFromFile()

            // Reload all textures from the project's textures directory
            loadAllLocalTextures()
        } ?: println("ResourceManager reload: No active project.")

        dumpResourceState() // Dump state after reload
        println("==== Project Assets Reload Complete ====")
    }
}