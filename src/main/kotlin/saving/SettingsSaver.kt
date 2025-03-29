package saving

import Game3D
import Renderer
import grideditor.GridEditor
import player.Player
import java.io.*
import ui.components.DisplayOptionsPanel
import ui.components.CrosshairShape
import render.SkyRenderer
import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO

class SettingsSaver(private val gridEditor: GridEditor) {
    companion object {
        // Define the directory where settings will be stored
        private const val SETTINGS_DIR = "settings"
        private const val DISPLAY_SETTINGS_FILE = "display_options.settings"
        private const val WORLD_SETTINGS_FILE = "world_options.settings"
        private const val PLAYER_SETTINGS_FILE = "player_options.settings"

        private const val SKY_IMAGES_DIR = "assets/textures/sky_images"

        // Make sure the settings directory exists
        private fun ensureSettingsDir() {
            val dir = File(SETTINGS_DIR)
            if (!dir.exists()) {
                dir.mkdir()
            }
        }

        // Make sure the sky images directory exists
        private fun ensureSkyImagesDir() {
            val dir = File(SKY_IMAGES_DIR)
            if (!dir.exists()) {
                dir.mkdirs() // Using mkdirs() to create parent directories if needed
            }
        }

        // Calculate image hash for deduplication
        private fun calculateImageHash(image: BufferedImage): String {
            val width = image.width
            val height = image.height
            var hash = 0L

            // Sample the image at intervals to create a simple hash
            for (y in 0 until height step (height / 10).coerceAtLeast(1)) {
                for (x in 0 until width step (width / 10).coerceAtLeast(1)) {
                    hash = hash * 31 + image.getRGB(x, y)
                }
            }

            return hash.toString(16) // Convert to hex string
        }
    }

    private var currentSkyImageFilename: String? = null

    fun saveDisplayOptions(displayOptionsPanel: DisplayOptionsPanel): Boolean {
        try {
            ensureSettingsDir()
            val file = File("$SETTINGS_DIR/$DISPLAY_SETTINGS_FILE")
            val outputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))

            // Write version for future compatibility
            outputStream.writeInt(1) // Version 1 of the settings format

            // Write timestamp
            outputStream.writeLong(System.currentTimeMillis())

            // Get all display options and write them
            val options = displayOptionsPanel.getAllOptions()

            // Write number of options
            outputStream.writeInt(options.size)

            // Write each option
            options.forEach { (key, state) ->
                outputStream.writeUTF(key)
                outputStream.writeBoolean(state)
            }

            // Write the flat walls visualization setting from GridEditor
            outputStream.writeUTF("VISUALIZATION_SECTION")
            outputStream.writeBoolean(gridEditor.showFlatWallsAsLines)

            outputStream.close()
            return true
        } catch (e: Exception) {
            println("Error saving display options: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun saveWorldSettings(renderer: Renderer, game3D: Game3D): Boolean {
        try {
            ensureSettingsDir()
            ensureSkyImagesDir() // Ensure sky images directory exists

            val file = File("$SETTINGS_DIR/$WORLD_SETTINGS_FILE")
            val outputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))

            // Write version for future compatibility
            outputStream.writeInt(6) // Update to version 6 for visibility radius settings

            // Write timestamp
            outputStream.writeLong(System.currentTimeMillis())

            // --- Renderer settings section ---
            outputStream.writeUTF("RENDERER_SECTION")

            outputStream.writeDouble(renderer.getFov())
            outputStream.writeDouble(renderer.getScale())
            outputStream.writeDouble(renderer.getNearPlane())
            outputStream.writeDouble(renderer.getFarPlane())

            // --- Border settings section ---
            outputStream.writeUTF("BORDER_SECTION")

            // Save border visibility
            outputStream.writeBoolean(renderer.drawBorders)

            // Save border thickness
            outputStream.writeFloat(renderer.borderThickness)

            // Save border color
            val borderColor = renderer.borderColor
            outputStream.writeInt(borderColor.red)
            outputStream.writeInt(borderColor.green)
            outputStream.writeInt(borderColor.blue)

            // --- Render Distance section ---
            outputStream.writeUTF("RENDER_DISTANCE_SECTION")

            // Save render distance settings
            outputStream.writeBoolean(renderer.enableRenderDistance)
            outputStream.writeDouble(renderer.maxRenderDistance)

            // --- Shadow settings section ---
            outputStream.writeUTF("SHADOW_SECTION")

            // Save shadow settings
            outputStream.writeBoolean(renderer.enableShadows)
            outputStream.writeDouble(renderer.shadowDistance)
            outputStream.writeDouble(renderer.shadowIntensity)
            outputStream.writeDouble(renderer.ambientLight)

            // NEW: Save shadow color
            val shadowColor = renderer.shadowColor
            outputStream.writeInt(shadowColor.red)
            outputStream.writeInt(shadowColor.green)
            outputStream.writeInt(shadowColor.blue)

            // --- Visibility Radius section (new in version 5) ---
            outputStream.writeUTF("VISIBILITY_RADIUS_SECTION")

            // Save visibility radius settings
            outputStream.writeBoolean(renderer.enableVisibilityRadius)
            outputStream.writeDouble(renderer.visibilityRadius)
            outputStream.writeDouble(renderer.visibilityFalloff)

            // Save outside color
            val outsideColor = renderer.outsideColor
            outputStream.writeInt(outsideColor.red)
            outputStream.writeInt(outsideColor.green)
            outputStream.writeInt(outsideColor.blue)

            // --- Game3D settings section ---
            outputStream.writeUTF("GAME3D_SECTION")

            // Sky color
            val skyColor = game3D.getSkyColor()
            outputStream.writeInt(skyColor.red)
            outputStream.writeInt(skyColor.green)
            outputStream.writeInt(skyColor.blue)

            // --- Sky Renderer settings section ---
            outputStream.writeUTF("SKY_RENDERER_SECTION")

            // Get the sky renderer from game3D
            val skyRenderer = game3D.getSkyRenderer()

            // Write if sky renderer exists
            outputStream.writeBoolean(true)

            // Write display mode (0 = COLOR, 1 = IMAGE_STRETCH, 2 = IMAGE_TILE)
            val displayMode = when {
                skyRenderer.skyImage == null -> 0 // COLOR
                !skyRenderer.tileImage -> 1      // IMAGE_STRETCH
                else -> 2                       // IMAGE_TILE
            }
            outputStream.writeInt(displayMode)

            // Write if an image is used
            val hasImage = skyRenderer.skyImage != null
            outputStream.writeBoolean(hasImage)

            // If there's an image, save it to a file and store the filename
            if (hasImage) {
                val skyImage = skyRenderer.skyImage!!

                // Convert Image to BufferedImage if needed
                val bufferedImage = if (skyImage is BufferedImage) {
                    skyImage
                } else {
                    val width = skyImage.getWidth(null)
                    val height = skyImage.getHeight(null)
                    val newImg = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                    val g2 = newImg.createGraphics()
                    g2.drawImage(skyImage, 0, 0, null)
                    g2.dispose()
                    newImg
                }

                // Calculate image hash for deduplication
                val imageHash = calculateImageHash(bufferedImage)

                // Check if we already have this image
                val imgFilename = "sky_image_$imageHash.png"
                val imgFile = File("$SKY_IMAGES_DIR/$imgFilename")

                // Delete previous sky image if it's different from the current one
                if (currentSkyImageFilename != null && currentSkyImageFilename != imgFilename) {
                    val oldFile = File("$SKY_IMAGES_DIR/$currentSkyImageFilename")
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                }

                // Save the image if it doesn't exist yet
                if (!imgFile.exists()) {
                    ImageIO.write(bufferedImage, "png", imgFile)
                }

                // Update current sky image filename
                currentSkyImageFilename = imgFilename

                // Write the image filename
                outputStream.writeUTF(imgFilename)
            } else {
                // If no image is used, clean up any previous image
                if (currentSkyImageFilename != null) {
                    val oldFile = File("$SKY_IMAGES_DIR/$currentSkyImageFilename")
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                    currentSkyImageFilename = null
                }
            }

            outputStream.close()
            return true
        } catch (e: Exception) {
            println("Error saving world settings: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun savePlayerSettings(player: Player, game3D: Game3D? = null): Boolean {
        try {
            ensureSettingsDir()
            val file = File("$SETTINGS_DIR/$PLAYER_SETTINGS_FILE")
            val outputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))

            // ----> UPDATE VERSION TO 5 <----
            outputStream.writeInt(5) // Version 5 of the settings format

            // Write timestamp
            outputStream.writeLong(System.currentTimeMillis())

            // --- Player settings section ---
            outputStream.writeUTF("PLAYER_SECTION")

            // Write player movement values
            outputStream.writeDouble(player.moveSpeed)
            outputStream.writeDouble(player.playerRadius)
            outputStream.writeDouble(player.playerHeight)
            outputStream.writeDouble(player.headClearance)

            // Add camera rotation speed
            outputStream.writeDouble(player.camera.accessRotationSpeed())

            // ---- INVERT Y SETTING
            outputStream.writeBoolean(player.camera.accessInvertY())

            // ---- GRAVITY SETTINGS ----
            outputStream.writeBoolean(player.gravityEnabled)
            outputStream.writeDouble(player.gravity)
            outputStream.writeDouble(player.jumpStrength)
            outputStream.writeDouble(player.terminalVelocity)

            // --- Crosshair settings section
            outputStream.writeUTF("CROSSHAIR_SECTION")

            // Only write crosshair settings if Game3D is provided
            if (game3D != null) {
                // Write crosshair visibility
                outputStream.writeBoolean(game3D.isCrosshairVisible())

                // Write crosshair size
                outputStream.writeInt(game3D.getCrosshairSize())

                // Write crosshair color
                val crosshairColor = game3D.getCrosshairColor()
                outputStream.writeInt(crosshairColor.red)
                outputStream.writeInt(crosshairColor.green)
                outputStream.writeInt(crosshairColor.blue)

                // Write crosshair shape (as ordinal integer)
                outputStream.writeInt(game3D.getCrosshairShape().ordinal)

            } else {
                // Default values if Game3D isn't available
                outputStream.writeBoolean(true) // Visible by default
                outputStream.writeInt(10) // Default size
                outputStream.writeInt(255) // White color (R)
                outputStream.writeInt(255) // White color (G)
                outputStream.writeInt(255) // White color (B)
                outputStream.writeInt(0) // PLUS shape by default
            }

            // --- Debug Options Section (new in version 3) ---
            outputStream.writeUTF("DEBUG_SECTION")

            if (game3D != null) {
                // Write FPS counter visibility
                outputStream.writeBoolean(game3D.isFpsCounterVisible())

                // Write direction visibility
                outputStream.writeBoolean(game3D.isDirectionVisible())

                // Write position visibility
                outputStream.writeBoolean(game3D.isPositionVisible())
            } else {
                // Default values if Game3D isn't available
                outputStream.writeBoolean(true) // FPS counter visible by default
                outputStream.writeBoolean(true) // Direction visible by default
                outputStream.writeBoolean(true) // Position visible by default
            }

            outputStream.close()
            return true
        } catch (e: Exception) {
            println("Error saving player settings: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun loadDisplayOptions(displayOptionsPanel: DisplayOptionsPanel): Boolean {
        try {
            val file = File("$SETTINGS_DIR/$DISPLAY_SETTINGS_FILE")
            if (!file.exists()) {
                println("Display settings file does not exist. Using defaults.")
                return false
            }

            val inputStream = DataInputStream(BufferedInputStream(FileInputStream(file)))

            // Read and verify version
            val version = inputStream.readInt()
            if (version != 1) {
                println("Unsupported settings file version: $version")
                inputStream.close()
                return false
            }

            // Read timestamp (not used now but available for future features)
            val timestamp = inputStream.readLong()

            // Read number of options
            val optionCount = inputStream.readInt()

            // Read each option and apply it
            for (i in 0..<optionCount) {
                val key = inputStream.readUTF()
                val state = inputStream.readBoolean()

                // Apply the setting
                displayOptionsPanel.setCheckboxState(key, state)
            }

            // Try to read visualization setting
            try {
                val section = inputStream.readUTF()
                if (section == "VISUALIZATION_SECTION") {
                    val showFlatWallsAsLines = inputStream.readBoolean()
                    gridEditor.setFlatWallVisualization(showFlatWallsAsLines)
                }
            } catch (e: Exception) {
                println("No visualization settings found, using defaults")
            }

            inputStream.close()
            return true
        } catch (e: Exception) {
            println("Error loading display options: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun loadWorldSettings(renderer: Renderer, game3D: Game3D): Boolean {
        try {
            val file = File("$SETTINGS_DIR/$WORLD_SETTINGS_FILE")
            if (!file.exists()) {
                println("World settings file does not exist")
                return false
            }

            val inputStream = DataInputStream(BufferedInputStream(FileInputStream(file)))

            // Read and verify version
            val version = inputStream.readInt()
            if (version < 1 || version > 6) {
                println("Unsupported settings file version: $version")
                inputStream.close()
                return false
            }

            // Read timestamp (not used now but available for future features)
            val timestamp = inputStream.readLong()

            // Read renderer section
            val rendererSection = inputStream.readUTF()
            if (rendererSection != "RENDERER_SECTION") {
                println("Invalid world settings file format - missing renderer section")
                inputStream.close()
                return false
            }

            // Read renderer settings
            val fov = inputStream.readDouble()
            val scale = inputStream.readDouble()
            val nearPlane = inputStream.readDouble()
            val farPlane = inputStream.readDouble()

            // Apply settings to renderer
            renderer.setFov(fov)
            renderer.setScale(scale)
            renderer.setNearPlane(nearPlane)
            renderer.setFarPlane(farPlane)

            // If version 3 or higher, read border settings
            if (version >= 3) {
                try {
                    val borderSection = inputStream.readUTF()
                    if (borderSection == "BORDER_SECTION") {
                        // Read border visibility
                        val drawBorders = inputStream.readBoolean()

                        // Read border thickness
                        val borderThickness = inputStream.readFloat()

                        // Read border color
                        val red = inputStream.readInt()
                        val green = inputStream.readInt()
                        val blue = inputStream.readInt()

                        // Apply border settings to renderer
                        renderer.drawBorders = drawBorders
                        renderer.borderThickness = borderThickness
                        renderer.borderColor = Color(red, green, blue)
                    }
                } catch (e: Exception) {
                    println("Error loading border settings: ${e.message}")
                    // Continue with defaults if border settings can't be loaded
                }
            }

            // If version 4 or higher, read render distance settings
            if (version >= 4) {
                try {
                    val renderDistanceSection = inputStream.readUTF()
                    if (renderDistanceSection == "RENDER_DISTANCE_SECTION") {
                        // Read render distance settings
                        val enableRenderDistance = inputStream.readBoolean()
                        val maxRenderDistance = inputStream.readDouble()

                        // Apply render distance settings to renderer
                        renderer.enableRenderDistance = enableRenderDistance
                        renderer.maxRenderDistance = maxRenderDistance
                    }
                } catch (e: Exception) {
                    println("Error loading render distance settings: ${e.message}")
                    // Continue with defaults if render distance settings can't be loaded
                }

                // Try to read shadow settings
                try {
                    val shadowSection = inputStream.readUTF()
                    if (shadowSection == "SHADOW_SECTION") {
                        // Read shadow settings
                        val enableShadows = inputStream.readBoolean()
                        val shadowDistance = inputStream.readDouble()
                        val shadowIntensity = inputStream.readDouble()
                        val ambientLight = inputStream.readDouble()

                        // NEW in version 6: Read shadow color if available
                        val shadowColor = if (version >= 6) {
                            val red = inputStream.readInt()
                            val green = inputStream.readInt()
                            val blue = inputStream.readInt()
                            Color(red, green, blue)
                        } else {
                            Color.BLACK // Default shadow color
                        }

                        // Apply shadow settings to renderer
                        renderer.enableShadows = enableShadows
                        renderer.shadowDistance = shadowDistance
                        renderer.shadowIntensity = shadowIntensity
                        renderer.ambientLight = ambientLight
                        renderer.shadowColor = shadowColor
                    }
                } catch (e: Exception) {
                    println("Error loading shadow settings: ${e.message}")
                    // Continue with defaults if shadow settings can't be loaded
                }
            }

            // If version 5 or higher, read visibility radius settings
            if (version >= 5) {
                try {
                    val visibilitySection = inputStream.readUTF()
                    if (visibilitySection == "VISIBILITY_RADIUS_SECTION") {
                        // Read visibility radius settings
                        val enableVisibilityRadius = inputStream.readBoolean()
                        val visibilityRadius = inputStream.readDouble()
                        val visibilityFalloff = inputStream.readDouble()

                        // Read outside color
                        val red = inputStream.readInt()
                        val green = inputStream.readInt()
                        val blue = inputStream.readInt()

                        // Apply visibility radius settings to renderer
                        renderer.enableVisibilityRadius = enableVisibilityRadius
                        renderer.visibilityRadius = visibilityRadius
                        renderer.visibilityFalloff = visibilityFalloff
                        renderer.outsideColor = Color(red, green, blue)
                    }
                } catch (e: Exception) {
                    println("Error loading visibility radius settings: ${e.message}")
                    // Continue with defaults if visibility radius settings can't be loaded
                }
            }

            // Read Game3D section
            val game3DSection = inputStream.readUTF()
            if (game3DSection != "GAME3D_SECTION") {
                println("Invalid world settings file format - missing Game3D section")
                inputStream.close()
                return false
            }

            // Read sky color
            val red = inputStream.readInt()
            val green = inputStream.readInt()
            val blue = inputStream.readInt()
            game3D.setSkyColor(Color(red, green, blue))

            // If version 2 or higher, read Sky Renderer settings
            if (version >= 2) {
                try {
                    val skySection = inputStream.readUTF()
                    if (skySection == "SKY_RENDERER_SECTION") {
                        // Read if sky renderer exists
                        val hasSkyRenderer = inputStream.readBoolean()

                        if (hasSkyRenderer) {
                            // Read display mode
                            val displayMode = inputStream.readInt()

                            // Read if image is used
                            val hasImage = inputStream.readBoolean()

                            // Initialize variables
                            var skyImage: Image? = null
                            var tileImage = false

                            // Load image if needed
                            if (hasImage) {
                                val imgFilename = inputStream.readUTF()
                                val imgFile = File("$SKY_IMAGES_DIR/$imgFilename")

                                // Update current sky image filename
                                currentSkyImageFilename = imgFilename

                                if (imgFile.exists()) {
                                    try {
                                        skyImage = ImageIO.read(imgFile)
                                    } catch (e: Exception) {
                                        println("Error loading sky image: ${e.message}")
                                        // Continue with no image if loading fails
                                    }
                                } else {
                                    // Try the old path as fallback for backward compatibility
                                    val oldImgFile = File("$SETTINGS_DIR/$imgFilename")
                                    if (oldImgFile.exists()) {
                                        try {
                                            skyImage = ImageIO.read(oldImgFile)
                                            // Migrate the image to the new location
                                            ensureSkyImagesDir()
                                            oldImgFile.copyTo(imgFile, overwrite = true)
                                            oldImgFile.delete() // Clean up old file
                                        } catch (e: Exception) {
                                            println("Error loading sky image from old path: ${e.message}")
                                        }
                                    }
                                }
                            }

                            // Set tiling based on display mode
                            tileImage = displayMode == 2

                            // Create and set the sky renderer
                            val skyColor = game3D.getSkyColor() // Use the already loaded color
                            val skyRenderer = SkyRenderer(skyColor, skyImage, tileImage)
                            game3D.setSkyRenderer(skyRenderer)
                        }
                    }
                } catch (e: Exception) {
                    println("Error loading sky renderer settings: ${e.message}")
                    // Continue with defaults if sky renderer settings can't be loaded
                }
            }

            inputStream.close()
            return true
        } catch (e: Exception) {
            println("Error loading world settings: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun loadPlayerSettings(player: Player, game3D: Game3D? = null): Boolean {
        try {
            val file = File("$SETTINGS_DIR/$PLAYER_SETTINGS_FILE")
            if (!file.exists()) {
                println("Player settings file does not exist")
                return false
            }

            val inputStream = DataInputStream(BufferedInputStream(FileInputStream(file)))

            // Read and verify version
            val version = inputStream.readInt()
            if (version < 1 || version > 5) {
                println("Unsupported settings file version: $version")
                inputStream.close()
                return false
            }

            // Read timestamp (not used now but available for future features)
            val timestamp = inputStream.readLong()

            // Read player section
            val playerSection = inputStream.readUTF()
            if (playerSection != "PLAYER_SECTION") {
                println("Invalid player settings file format - missing player section")
                inputStream.close()
                return false
            }

            // Read player settings
            val moveSpeed = inputStream.readDouble()
            val playerRadius = inputStream.readDouble()
            val playerHeight = inputStream.readDouble()
            val headClearance = inputStream.readDouble()

            // Read camera rotation speed
            val rotationSpeed = inputStream.readDouble()

            // Apply settings to player
            player.setMovementSettings(moveSpeed, playerRadius, playerHeight, headClearance)

            // Apply camera settings
            player.camera.changeRotationSpeed(rotationSpeed)

            if (version >= 5) {
                try {
                    val invertY = inputStream.readBoolean()
                    player.camera.changeInvertY(invertY) // Apply the loaded setting
                } catch (e: EOFException) {
                    println("Reached end of file unexpectedly while reading invert Y setting (v5). Using default.")
                    player.camera.changeInvertY(false) // Default if reading fails
                } catch (e: IOException) {
                    println("Error reading invert Y setting (v5): ${e.message}. Using default.")
                    player.camera.changeInvertY(false) // Default if reading fails
                }
            } else {
                // If loading older version, ensure default is set
                player.camera.changeInvertY(false)
            }

            // ---- GRAVITY SETTINGS ----
            if (version >= 4) {
                try {
                    // Read the values from the file
                    val loadedGravityEnabled = inputStream.readBoolean()
                    val gravity = inputStream.readDouble()
                    val jumpStrength = inputStream.readDouble()
                    val terminalVelocity = inputStream.readDouble()

                    // Apply gravity parameters directly
                    player.gravity = gravity
                    player.jumpStrength = jumpStrength
                    player.terminalVelocity = terminalVelocity
                    player.setGravity(loadedGravityEnabled)

                    game3D?.isGravityEnabled = loadedGravityEnabled

                } catch (e: EOFException) {
                    println("Reached end of file unexpectedly while reading gravity settings (file version $version). Using defaults.")
                    // Apply default gravity state if reading failed
                    player.setGravity(false) // Or your default value
                    game3D?.isGravityEnabled = false
                }
                catch (e: Exception) {
                    println("Error loading gravity settings (file version $version): ${e.message}")
                    // Continue with default gravity settings if loading fails
                    player.setGravity(false) // Or your default value
                    game3D?.isGravityEnabled = false
                }
            } else {
                // If loading an older version without gravity settings, ensure defaults are set
                player.setGravity(false) // Or your default value
                game3D?.isGravityEnabled = false
            }

            // Read crosshair settings if version is 2 or higher and if Game3D is provided
            if (version >= 2 && game3D != null) {
                try {
                    val crosshairSection = inputStream.readUTF()
                    if (crosshairSection == "CROSSHAIR_SECTION") {
                        // Read crosshair visibility
                        val isCrosshairVisible = inputStream.readBoolean()

                        // Read crosshair size
                        val crosshairSize = inputStream.readInt()

                        // Read crosshair color
                        val red = inputStream.readInt()
                        val green = inputStream.readInt()
                        val blue = inputStream.readInt()

                        // Read crosshair shape ordinal
                        val shapeOrdinal = inputStream.readInt()
                        val crosshairShape = CrosshairShape.entries.getOrElse(shapeOrdinal) { CrosshairShape.PLUS }

                        // Apply settings to Game3D
                        game3D.setCrosshairVisible(isCrosshairVisible)
                        game3D.setCrosshairSize(crosshairSize)
                        game3D.setCrosshairColor(Color(red, green, blue))
                        game3D.setCrosshairShape(crosshairShape)
                    }
                } catch (e: Exception) {
                    println("Error loading crosshair settings: ${e.message}")
                    // Continue with defaults if crosshair settings can't be loaded
                }
            }

            // Read debug options if version is 3 or higher and if Game3D is provided
            if (version >= 3 && game3D != null) {
                try {
                    val debugSection = inputStream.readUTF()
                    if (debugSection == "DEBUG_SECTION") {
                        // Read debug visibility options
                        val isFpsVisible = inputStream.readBoolean()
                        val isDirectionVisible = inputStream.readBoolean()
                        val isPositionVisible = inputStream.readBoolean()

                        // Apply debug settings to Game3D
                        game3D.setFpsCounterVisible(isFpsVisible)
                        game3D.setDirectionVisible(isDirectionVisible)
                        game3D.setPositionVisible(isPositionVisible)
                    }
                } catch (e: Exception) {
                    println("Error loading debug settings: ${e.message}")
                    // Continue with defaults if debug settings can't be loaded
                }
            }

            inputStream.close()
            return true
        } catch (e: Exception) {
            println("Error loading player settings: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun backupSettings(): Boolean {
        try {
            ensureSettingsDir()
            val settingsDir = File(SETTINGS_DIR)
            val backupDir = File("$SETTINGS_DIR/backup")

            if (!backupDir.exists()) {
                backupDir.mkdir()
            }

            // Create timestamp for backup
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

            // Copy all settings files to backup directory with timestamp
            settingsDir.listFiles { file -> file.isFile && file.name.endsWith(".settings") }?.forEach { file ->
                val backupFile = File(backupDir, "${file.nameWithoutExtension}_$timestamp.settings")
                file.copyTo(backupFile, overwrite = true)
            }

            return true
        } catch (e: Exception) {
            println("Error creating settings backup: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}