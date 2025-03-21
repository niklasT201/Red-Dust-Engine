import grideditor.GridEditor
import player.Player
import java.io.*
import ui.components.DisplayOptionsPanel
import ui.components.CrosshairShape
import ui.components.SkyRenderer
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

        // Make sure the settings directory exists
        private fun ensureSettingsDir() {
            val dir = File(SETTINGS_DIR)
            if (!dir.exists()) {
                dir.mkdir()
            }
        }
    }

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
            val file = File("$SETTINGS_DIR/$WORLD_SETTINGS_FILE")
            val outputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))

            // Write version for future compatibility
            outputStream.writeInt(2) // Update to Version 2 for sky renderer settings

            // Write timestamp
            outputStream.writeLong(System.currentTimeMillis())

            // --- Renderer settings section ---
            outputStream.writeUTF("RENDERER_SECTION")

            outputStream.writeDouble(renderer.getFov())
            outputStream.writeDouble(renderer.getScale())
            outputStream.writeDouble(renderer.getNearPlane())
            outputStream.writeDouble(renderer.getFarPlane())

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

                // Create unique filename based on timestamp
                val timestamp = System.currentTimeMillis()
                val imgFilename = "sky_image_$timestamp.png"
                val imgFile = File("$SETTINGS_DIR/$imgFilename")

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

                // Save the image
                ImageIO.write(bufferedImage, "png", imgFile)

                // Write the image filename
                outputStream.writeUTF(imgFilename)
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

            // Update version to 3 since we're adding debug options
            outputStream.writeInt(3) // Version 3 of the settings format

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

            // --- Crosshair settings section ---
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
            if (version < 1 || version > 2) {
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
                                val imgFile = File("$SETTINGS_DIR/$imgFilename")

                                if (imgFile.exists()) {
                                    try {
                                        skyImage = ImageIO.read(imgFile)
                                    } catch (e: Exception) {
                                        println("Error loading sky image: ${e.message}")
                                        // Continue with no image if loading fails
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
            if (version < 1 || version > 3) {
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