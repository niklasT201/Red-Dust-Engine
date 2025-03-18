import grideditor.GridEditor
import player.Player
import java.io.*
import ui.components.DisplayOptionsPanel
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*

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

    /**
     * Saves renderer settings to a file
     * @param renderer The renderer containing settings to save
     */
    fun saveWorldSettings(renderer: Renderer, game3D: Game3D): Boolean {
        try {
            ensureSettingsDir()
            val file = File("$SETTINGS_DIR/$WORLD_SETTINGS_FILE")
            val outputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))

            // Write version for future compatibility
            outputStream.writeInt(1) // Version 1 of the settings format

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

            outputStream.close()
            return true
        } catch (e: Exception) {
            println("Error saving world settings: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun savePlayerSettings(player: Player): Boolean {
        try {
            ensureSettingsDir()
            val file = File("$SETTINGS_DIR/$PLAYER_SETTINGS_FILE")
            val outputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))

            // Write version for future compatibility
            outputStream.writeInt(1) // Version 1 of the settings format

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

            outputStream.close()
            return true
        } catch (e: Exception) {
            println("Error saving player settings: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Loads display options from a file
     * @param displayOptionsPanel The panel to load the settings into
     * @return True if load was successful, false otherwise
     */
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

    /**
     * Loads renderer settings from a file
     * @param renderer The renderer to load the settings into
     * @return True if load was successful, false otherwise
     */
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
            if (version != 1) {
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

            inputStream.close()
            return true
        } catch (e: Exception) {
            println("Error loading world settings: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun loadPlayerSettings(player: Player): Boolean {
        try {
            val file = File("$SETTINGS_DIR/$PLAYER_SETTINGS_FILE")
            if (!file.exists()) {
                println("Player settings file does not exist")
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

            inputStream.close()
            return true
        } catch (e: Exception) {
            println("Error loading player settings: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Creates a backup of the current settings
     * @return True if backup was successful, false otherwise
     */
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