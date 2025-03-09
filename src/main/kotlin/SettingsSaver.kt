import java.io.*
import ui.components.DisplayOptionsPanel
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI

/**
 * Saves and loads application settings to a file.
 * Designed to handle multiple types of settings with an extensible architecture.
 */
class SettingsSaver {
    companion object {
        // Define the directory where settings will be stored
        private const val SETTINGS_DIR = "settings"
        private const val DISPLAY_SETTINGS_FILE = "display_options.settings"
        private const val RENDERER_SETTINGS_FILE = "renderer_options.settings"

        // Make sure the settings directory exists
        private fun ensureSettingsDir() {
            val dir = File(SETTINGS_DIR)
            if (!dir.exists()) {
                dir.mkdir()
            }
        }
    }

    /**
     * Saves display options to a file
     * @param displayOptionsPanel The panel containing display options
     * @return True if save was successful, false otherwise
     */
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
     * @return True if save was successful, false otherwise
     */
    fun saveRendererSettings(renderer: Renderer): Boolean {
        try {
            ensureSettingsDir()
            val file = File("$SETTINGS_DIR/$RENDERER_SETTINGS_FILE")
            val outputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))

            // Write version for future compatibility
            outputStream.writeInt(1) // Version 1 of the settings format

            // Write timestamp
            outputStream.writeLong(System.currentTimeMillis())

            // Write renderer settings
            outputStream.writeDouble(renderer.getFov())
            outputStream.writeDouble(renderer.getScale())
            outputStream.writeDouble(renderer.getNearPlane())
            outputStream.writeDouble(renderer.getFarPlane())

            outputStream.close()
            return true
        } catch (e: Exception) {
            println("Error saving renderer settings: ${e.message}")
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
                println("Display settings file does not exist")
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
            for (i in 0 until optionCount) {
                val key = inputStream.readUTF()
                val state = inputStream.readBoolean()

                // Apply the setting
                displayOptionsPanel.setCheckboxState(key, state)
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
    fun loadRendererSettings(renderer: Renderer): Boolean {
        try {
            val file = File("$SETTINGS_DIR/$RENDERER_SETTINGS_FILE")
            if (!file.exists()) {
                println("Renderer settings file does not exist")
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

            inputStream.close()
            return true
        } catch (e: Exception) {
            println("Error loading renderer settings: ${e.message}")
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