import java.io.*
import ui.components.DisplayOptionsPanel
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*

/**
 * Saves and loads application settings to a file.
 * Designed to handle multiple types of settings with an extensible architecture.
 */
class SettingsSaver {
    companion object {
        // Define the directory where settings will be stored
        private const val SETTINGS_DIR = "settings"
        private const val DISPLAY_SETTINGS_FILE = "display_options.settings"

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

    /**
     * Saves additional settings (for future extension)
     * This is a template for adding new settings types
     * @param name Identifier for this settings group
     * @param settings Map of setting names to their values
     */
    fun saveGenericSettings(name: String, settings: Map<String, Any>): Boolean {
        try {
            ensureSettingsDir()
            val file = File("$SETTINGS_DIR/${name}.settings")
            val outputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))

            // Write version
            outputStream.writeInt(1)

            // Write timestamp
            outputStream.writeLong(System.currentTimeMillis())

            // Write number of settings
            outputStream.writeInt(settings.size)

            // Write each setting
            settings.forEach { (key, value) ->
                outputStream.writeUTF(key)

                // Write type identifier
                when (value) {
                    is Boolean -> {
                        outputStream.writeInt(1) // Type 1 = Boolean
                        outputStream.writeBoolean(value)
                    }
                    is Int -> {
                        outputStream.writeInt(2) // Type 2 = Integer
                        outputStream.writeInt(value)
                    }
                    is Double -> {
                        outputStream.writeInt(3) // Type 3 = Double
                        outputStream.writeDouble(value)
                    }
                    is String -> {
                        outputStream.writeInt(4) // Type 4 = String
                        outputStream.writeUTF(value)
                    }
                    is Color -> {
                        outputStream.writeInt(5) // Type 5 = Color
                        outputStream.writeInt(value.red)
                        outputStream.writeInt(value.green)
                        outputStream.writeInt(value.blue)
                        outputStream.writeInt(value.alpha)
                    }
                    else -> {
                        // Skip unsupported types
                        println("Unsupported setting type for key: $key")
                    }
                }
            }

            outputStream.close()
            return true
        } catch (e: Exception) {
            println("Error saving $name settings: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Loads generic settings (for future extension)
     * @param name Identifier for this settings group
     * @return Map of setting names to their values, or null if loading failed
     */
    fun loadGenericSettings(name: String): Map<String, Any>? {
        try {
            val file = File("$SETTINGS_DIR/${name}.settings")
            if (!file.exists()) {
                println("Settings file does not exist: ${name}.settings")
                return null
            }

            val inputStream = DataInputStream(BufferedInputStream(FileInputStream(file)))
            val settings = mutableMapOf<String, Any>()

            // Read and verify version
            val version = inputStream.readInt()
            if (version != 1) {
                println("Unsupported settings file version: $version")
                inputStream.close()
                return null
            }

            // Read timestamp
            val timestamp = inputStream.readLong()

            // Read number of settings
            val settingCount = inputStream.readInt()

            // Read each setting
            for (i in 0..<settingCount) {
                val key = inputStream.readUTF()
                val typeId = inputStream.readInt()

                val value: Any = when (typeId) {
                    1 -> inputStream.readBoolean()  // Boolean
                    2 -> inputStream.readInt()      // Integer
                    3 -> inputStream.readDouble()   // Double
                    4 -> inputStream.readUTF()      // String
                    5 -> {                          // Color
                        val red = inputStream.readInt()
                        val green = inputStream.readInt()
                        val blue = inputStream.readInt()
                        val alpha = inputStream.readInt()
                        Color(red, green, blue, alpha)
                    }
                    else -> {
                        println("Unknown type ID: $typeId for key: $key")
                        continue
                    }
                }

                settings[key] = value
            }

            inputStream.close()
            return settings
        } catch (e: Exception) {
            println("Error loading $name settings: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
}