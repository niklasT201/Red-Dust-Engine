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

    // Function to get the project-specific textures/objects directory
    private fun getTexturesDirectoryPath(): Path? {
        return fileManager.getAssetsDirectory()?.toPath()?.resolve("textures/objects")
    }

    // Function to get the base textures directory (for backward compatibility)
    private fun getBaseTexturesDirectoryPath(): Path? {
        return fileManager.getAssetsDirectory()?.toPath()?.resolve("textures")
    }

    init {
        // Create textures/objects directory if a project is loaded and it doesn't exist
        getTexturesDirectoryPath()?.let { path ->
            if (!path.exists()) {
                try {
                    path.createDirectories()
                    println("Created textures/objects directory at: $path")
                } catch (e: Exception) {
                    System.err.println("Error creating textures/objects directory at $path: ${e.message}")
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

    // Helper to get the textures/objects directory as a File, returns null if no project
    private fun getTexturesDirectoryFile(): File? {
        return getTexturesDirectoryPath()?.toFile()
    }

    // Helper to get the base textures directory as a File
    private fun getBaseTexturesDirectoryFile(): File? {
        return getBaseTexturesDirectoryPath()?.toFile()
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
     * Copies an image file to the local textures/objects directory OF THE CURRENT PROJECT.
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

            // Ensure textures/objects directory exists
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
            println("Copied texture to project objects directory: $normalizedAbsPath") // <-- Log normalized path

            return normalizedAbsPath // <-- Return normalized path
        } catch (e: Exception) {
            e.printStackTrace()
            System.err.println("Error copying image to project storage: ${e.message}")
            return originalPath // Return original path if copy fails
        }
    }

    /**
     * Loads an image from a file and adds it to the resource manager.
     * Copies the image to the project's textures/objects directory if a project is active.
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
     * copying them to the project's textures/objects directory.
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
     * Loads all textures from the current project's textures directories
     * First checks objects subdirectory, then the base textures directory for backward compatibility
     */
    private fun loadAllLocalTextures() {
        // First check the new objects directory
        val objectsDir = getTexturesDirectoryFile()
        if (objectsDir != null && objectsDir.exists() && objectsDir.isDirectory) {
            loadTexturesFromProjectDirectory(objectsDir, "objects")
        }

        // Then check the base textures directory (for backward compatibility)
        val baseDir = getBaseTexturesDirectoryFile()
        if (baseDir != null && baseDir.exists() && baseDir.isDirectory) {
            // Skip the objects subdirectory if it exists in base dir
            val dirContents = baseDir.listFiles() ?: emptyArray()
            val filteredDirs = dirContents.filter { file ->
                file.isFile && file.name != "objects" // Only load files, not the objects directory
            }.toTypedArray()

            loadTexturesFromProjectDirectory(baseDir, "base", filteredDirs)
        }
    }

    /**
     * Helper method to load textures from a specific project directory
     */
    private fun loadTexturesFromProjectDirectory(directory: File, dirType: String, filesOverride: Array<File>? = null) {
        println("\nLoading local textures from ${dirType} directory: ${directory.absolutePath}")
        val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp")
        var loadedCount = 0
        var skippedCount = 0
        var errorCount = 0

        val files = filesOverride ?: directory.listFiles() ?: emptyArray()
        val imageFiles = files.filter { it.isFile && imageExtensions.contains(it.extension.lowercase()) }

        println("Found ${imageFiles.size} image files in ${dirType} directory")

        imageFiles.forEach { file ->
            try {
                val image = ImageIO.read(file)
                if (image != null) {
                    val normalizedAbsPath = file.toPath().toAbsolutePath().normalize().toString()
                    // Check if already loaded using the normalized path
                    val existing = images.values.find { it.path == normalizedAbsPath }
                    if (existing == null) {
                        val id = "img_${System.currentTimeMillis()}_${images.size}_${UUID.randomUUID().toString().substring(0,4)}"
                        images[id] = ImageEntry(file.name, normalizedAbsPath, image)
                        loadedCount++
                        println("  Loaded: ${file.name} (ID: $id) Path: $normalizedAbsPath")
                    } else {
                        skippedCount++
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

        println("Finished loading ${dirType} directory textures:")
        println("  - Loaded: $loadedCount")
        println("  - Skipped: $skippedCount")
        println("  - Errors: $errorCount")
    }

    fun getImage(id: String): ImageEntry? {
        return images[id]
    }

    fun getImageByName(name: String): ImageEntry? {
        // First look in objects directory
        val objectsTextureDir = getTexturesDirectoryPath()
        val objectsMatch = if (objectsTextureDir != null) {
            images.values.firstOrNull {
                it.name == name && Paths.get(it.path).startsWith(objectsTextureDir)
            }
        } else null

        if (objectsMatch != null) return objectsMatch

        // Then look in base textures directory
        val baseTextureDir = getBaseTexturesDirectoryPath()
        val baseMatch = if (baseTextureDir != null) {
            images.values.firstOrNull {
                it.name == name && Paths.get(it.path).startsWith(baseTextureDir)
            }
        } else null

        if (baseMatch != null) return baseMatch

        // Finally, look for any match
        return images.values.firstOrNull { it.name == name }
    }

    fun getAllImages(): Map<String, ImageEntry> {
        return images.toMap() // Return a copy
    }

    /**
     * Gets a cached texture or loads it from the path.
     * Tries objects directory first, then base textures directory.
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

            // Path resolution logic - try objects directory first, then base textures
            if (!fileToLoad.isAbsolute || !fileToLoad.exists()) {
                println("  File not found at path, checking project textures/objects...")
                getTexturesDirectoryFile()?.let { objDir ->
                    val potentialFile = File(objDir, fileToLoad.name)
                    if (potentialFile.exists()) {
                        fileToLoad = potentialFile
                        println("  Found in project objects directory: ${fileToLoad.absolutePath}")
                    } else {
                        println("  Not found in objects directory, checking base textures...")
                        // Try base textures directory
                        getBaseTexturesDirectoryFile()?.let { texDir ->
                            val basePotentialFile = File(texDir, fileToLoad.name)
                            if (basePotentialFile.exists()) {
                                fileToLoad = basePotentialFile
                                println("  Found in base textures directory: ${fileToLoad.absolutePath}")
                            } else {
                                println("  Not found in any project texture directories")
                            }
                        }
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
        val objectsDir = getTexturesDirectoryPath()
        val baseTexturesDir = getBaseTexturesDirectoryPath()

        if (objectsDir == null && baseTexturesDir == null) {
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

            // Delete the physical file ONLY if it's within the project's textures directories
            val file = File(imageEntry.path)
            val inObjectsDir = objectsDir != null && file.toPath().startsWith(objectsDir)
            val inBaseDir = baseTexturesDir != null && file.toPath().startsWith(baseTexturesDir)

            if (file.exists() && file.isFile && (inObjectsDir || inBaseDir)) {
                Files.delete(file.toPath())
                println("Deleted texture file: ${file.absolutePath}")
                // If metadata was associated, save the updated metadata file
                if (removedMeta != null) {
                    saveMetadataToFile()
                }
                return true
            } else if (file.exists() && !(inObjectsDir || inBaseDir)) {
                println("Texture file ${file.absolutePath} is outside the project directories. Not deleting file, removing entry only.")
                return true // Removed from manager successfully
            } else {
                println("Texture file not found or already deleted: ${imageEntry.path}")
                return true // Removed from manager successfully
            }
        } catch (e: Exception) {
            e.printStackTrace()
            System.err.println("Failed to delete texture file: ${e.message}")
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
            val objectsDir = getTexturesDirectoryPath() // Get objects dir path for relative check
            val baseDir = getBaseTexturesDirectoryPath() // Get base dir path for relative check

            textureMetadata.forEach { (path, type) ->
                // Store filename relative to textures dir if possible, otherwise full path/name
                val file = File(path) // path is the absolute normalized key
                val keyToSave: String

                if (objectsDir != null && file.toPath().startsWith(objectsDir)) {
                    // Store relative path if inside project textures/objects dir
                    keyToSave = objectsDir.relativize(file.toPath()).toString().replace('\\', '/') // Use forward slashes for consistency
                } else if (baseDir != null && file.toPath().startsWith(baseDir)) {
                    // Store with "base:" prefix for textures in the base directory
                    keyToSave = "base:" + baseDir.relativize(file.toPath()).toString().replace('\\', '/')
                } else {
                    // Fallback to absolute path if outside
                    keyToSave = path // Store the absolute normalized path
                    println("Warning: Saving absolute path for texture outside project: $path")
                }
                properties.setProperty(keyToSave, type.name)
            }

            FileOutputStream(metadataFile).use { outputStream ->
                properties.store(outputStream, "Texture object type associations (Paths relative to project textures dirs)")
            }
            println("Saved texture metadata to ${metadataFile.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            System.err.println("Error saving texture metadata: ${e.message}")
        }
    }

    // Load metadata from file
    private fun loadMetadataFromFile() {
        val metadataFile = getMetadataFilePath()?.toFile() ?: return
        val objectsDir = getTexturesDirectoryFile()
        val baseDir = getBaseTexturesDirectoryFile()

        if (objectsDir == null && baseDir == null) return

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
                    // Check if it's a base texture (prefixed with "base:")
                    if (pathOrName.startsWith("base:") && baseDir != null) {
                        val relativePath = pathOrName.substring(5) // Remove "base:" prefix
                        val absolutePath = Paths.get(baseDir.toString(), relativePath).normalize()
                        loadTextureMetadataEntry(absolutePath.toFile(), typeName, loadedMeta)?.let {
                            successCount++
                        } ?: run { failedCount++ }
                    }
                    // Try objects directory first (for non-prefixed paths)
                    else if (objectsDir != null) {
                        val absolutePath = Paths.get(objectsDir.absolutePath, pathOrName).normalize()
                        val file = absolutePath.toFile()

                        if (file.exists()) {
                            loadTextureMetadataEntry(file, typeName, loadedMeta)?.let {
                                successCount++
                            } ?: run { failedCount++ }
                        }
                        // If not found in objects dir and no base: prefix, try base dir
                        else if (baseDir != null) {
                            val baseAbsolutePath = Paths.get(baseDir.absolutePath, pathOrName).normalize()
                            val baseFile = baseAbsolutePath.toFile()

                            if (baseFile.exists()) {
                                loadTextureMetadataEntry(baseFile, typeName, loadedMeta)?.let {
                                    successCount++
                                } ?: run { failedCount++ }
                            } else {
                                // Try by name match across both directories
                                val foundInObjects = objectsDir.listFiles { f -> f.name == pathOrName }?.firstOrNull()
                                val foundInBase = baseDir.listFiles { f -> f.name == pathOrName }?.firstOrNull()

                                when {
                                    foundInObjects != null -> {
                                        loadTextureMetadataEntry(foundInObjects, typeName, loadedMeta)?.let {
                                            successCount++
                                        } ?: run { failedCount++ }
                                    }
                                    foundInBase != null -> {
                                        loadTextureMetadataEntry(foundInBase, typeName, loadedMeta)?.let {
                                            successCount++
                                        } ?: run { failedCount++ }
                                    }
                                    else -> {
                                        println("  WARNING: Missing texture file: '$pathOrName'")
                                        missingFileCount++
                                    }
                                }
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

    /**
     * Helper function to load a single texture metadata entry
     * Returns the object type if successful, null otherwise
     */
    private fun loadTextureMetadataEntry(file: File, typeName: String, loadedMeta: ConcurrentHashMap<String, ObjectType>): ObjectType? {
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
                        return objectType
                    } else {
                        println("  WARNING: Could not read image data from ${file.name}")
                    }
                } catch (readEx: Exception) {
                    println("  ERROR loading ${file.name}: ${readEx.message}")
                }
            } else {
                println("  Set metadata for already loaded: ${file.name} (type: $objectType)")
                return objectType
            }
        } catch (e: IllegalArgumentException) {
            println("  WARNING: Unknown object type '$typeName' for '${file.name}'")
        }
        return null
    }

    // Inside ResourceManager class
    fun reloadProjectAssets() {
        println("==== Reloading Project Assets ====")
        // Consider a more nuanced merge if needed later.
        images.clear() // Might be too aggressive? Maybe only clear project-related ones?
        textureCache.clear()
        textureMetadata.clear()

        // Re-create textures/objects directory if needed
        getTexturesDirectoryPath()?.let { path ->
            if (!path.exists()) {
                try {
                    path.createDirectories()
                    println("Created textures/objects directory at: $path")
                } catch (e: Exception) {
                    System.err.println("Error creating textures/objects directory: ${e.message}")
                }
            }

            // Also ensure base textures directory exists
            getBaseTexturesDirectoryPath()?.let { basePath ->
                if (!basePath.exists()) {
                    try {
                        basePath.createDirectories()
                        println("Created base textures directory at: $basePath")
                    } catch (e: Exception) {
                        System.err.println("Error creating base textures directory: ${e.message}")
                    }
                }
            }

            // Reload metadata first using the current project context
            loadMetadataFromFile()

            // Reload all textures from both directories
            loadAllLocalTextures()
        } ?: println("ResourceManager reload: No active project.")

        dumpResourceState() // Dump state after reload
        println("==== Project Assets Reload Complete ====")
    }
}