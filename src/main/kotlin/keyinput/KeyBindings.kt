package controls

import java.awt.event.KeyEvent
import java.io.*

/**
 * Manages configurable key bindings for the game
 * Provides default values and persistence for user-defined key bindings
 */
class KeyBindingManager {
    companion object {
        private const val SETTINGS_DIR = "settings"
        private const val KEY_BINDINGS_FILE = "key_bindings.settings"

        // Fixed keys for undo/redo operations
        const val UNDO_KEY = KeyEvent.VK_Z
        const val REDO_KEY = KeyEvent.VK_Y
        const val KEY_UNBOUND = -1

        // Make sure the settings directory exists
        private fun ensureSettingsDir() {
            val dir = File(SETTINGS_DIR)
            if (!dir.exists()) {
                dir.mkdir()
            }
        }
    }

    // Maps key names to their current key codes
    private val keyBindings = mutableMapOf<String, Int>()
    private val defaultBindings = mutableMapOf<String, Int>()

    // List of configurable keys (the ones users are allowed to change)
    private val configurableKeys = listOf(
        "MOVE_FORWARD", "MOVE_BACKWARD", "MOVE_LEFT", "MOVE_RIGHT",
        "FLY_UP", "FLY_DOWN", "JUMP", "TOGGLE_EDITOR"
    )

    // List of fixed keys (the ones users cannot change)
    private val fixedKeys = listOf(
        "ROTATE_NORTH", "ROTATE_SOUTH", "ROTATE_EAST", "ROTATE_WEST",
        "ROTATE_WALL", "WALL_SHORTCUT", "FLOOR_SHORTCUT", "PILLAR_SHORTCUT","PLAYER_SPAWN_SHORTCUT",
        "UNDO", "REDO"
    )

    init {
        // Initialize with the default bindings
        initializeDefaultBindings()

        // Copy defaults to current bindings
        keyBindings.putAll(defaultBindings)

        // Try to load saved bindings
        loadKeyBindings()
    }

    private fun initializeDefaultBindings() {
        // Movement Keys (configurable)
        defaultBindings["MOVE_FORWARD"] = KeyEvent.VK_W
        defaultBindings["MOVE_BACKWARD"] = KeyEvent.VK_S
        defaultBindings["MOVE_LEFT"] = KeyEvent.VK_A
        defaultBindings["MOVE_RIGHT"] = KeyEvent.VK_D
        defaultBindings["FLY_UP"] = KeyEvent.VK_SPACE
        defaultBindings["FLY_DOWN"] = KeyEvent.VK_SHIFT
        defaultBindings["JUMP"] = KeyEvent.VK_J
        defaultBindings["TOGGLE_EDITOR"] = KeyEvent.VK_E

        // Direction Keys (fixed)
        defaultBindings["ROTATE_NORTH"] = KeyEvent.VK_N
        defaultBindings["ROTATE_SOUTH"] = KeyEvent.VK_S
        defaultBindings["ROTATE_EAST"] = KeyEvent.VK_O
        defaultBindings["ROTATE_WEST"] = KeyEvent.VK_W

        // Shortcuts (fixed)
        defaultBindings["ROTATE_WALL"] = KeyEvent.VK_R
        defaultBindings["WALL_SHORTCUT"] = KeyEvent.VK_1
        defaultBindings["FLOOR_SHORTCUT"] = KeyEvent.VK_2
        defaultBindings["PILLAR_SHORTCUT"] = KeyEvent.VK_3
        defaultBindings["PLAYER_SPAWN_SHORTCUT"] = KeyEvent.VK_4

        defaultBindings["WEAPON_UI_TOGGLE"] = KeyEvent.VK_U

        // Undo/Redo Keys (fixed)
        defaultBindings["UNDO"] = UNDO_KEY // Ctrl+Z
        defaultBindings["REDO"] = REDO_KEY // Ctrl+Y
    }

    private val disabledActions = mutableSetOf<String>()

    // Add methods to disable/enable actions
    fun disableAction(name: String): Boolean {
        if (name in configurableKeys) {
            disabledActions.add(name)
            // Also unbind the key
            keyBindings[name] = KEY_UNBOUND
            return true
        }
        return false
    }

    fun enableAction(name: String): Boolean {
        return disabledActions.remove(name)
    }

    fun isActionDisabled(name: String): Boolean {
        return name in disabledActions
    }

    // Update the getKeyBinding method to handle unbound actions
    fun getKeyBinding(name: String): Int {
        // For disabled actions, always return KEY_UNBOUND
        if (name in disabledActions) {
            return KEY_UNBOUND
        }

        // Otherwise return the current binding or the default
        return keyBindings[name] ?: defaultBindings[name] ?: KeyEvent.VK_UNDEFINED
    }

    fun setKeyBinding(name: String, keyCode: Int): Boolean {
        // Only allow changing configurable keys
        if (name in configurableKeys) {
            keyBindings[name] = keyCode
            return true
        }
        return false
    }

    fun resetKeyBinding(name: String): Boolean {
        if (name in configurableKeys) {
            val defaultValue = defaultBindings[name]
            if (defaultValue != null) {
                keyBindings[name] = defaultValue
                return true
            }
        }
        return false
    }

    fun resetAllKeyBindings() {
        configurableKeys.forEach { key ->
            defaultBindings[key]?.let { defaultValue ->
                keyBindings[key] = defaultValue
            }
        }
    }

    fun getConfigurableBindings(): Map<String, Int> {
        return configurableKeys.associateWith { keyBindings[it] ?: KeyEvent.VK_UNDEFINED }
    }

    fun getKeyName(keyCode: Int): String {
        return KeyEvent.getKeyText(keyCode)
    }

    fun getConflictingBinding(keyCode: Int, excludeKey: String): String? {
        // Only check for conflicts in configurable keys
        configurableKeys.forEach { name ->
            if (name != excludeKey && keyBindings[name] == keyCode) {
                return name
            }
        }
        return null
    }

    fun getBindingDisplayName(name: String): String {
        return when(name) {
            "MOVE_FORWARD" -> "Move Forward"
            "MOVE_BACKWARD" -> "Move Backward"
            "MOVE_LEFT" -> "Strafe Left"
            "MOVE_RIGHT" -> "Strafe Right"
            "FLY_UP" -> "Fly Up"
            "FLY_DOWN" -> "Fly Down"
            "JUMP" -> "Jump"
            "TOGGLE_EDITOR" -> "Toggle Editor Mode"
            "UNDO" -> "Undo"
            "REDO" -> "Redo"
            else -> name.replace("_", " ").toLowerCase().capitalize()
        }
    }

    fun saveKeyBindings(): Boolean {
        try {
            ensureSettingsDir()
            val file = File("$SETTINGS_DIR/$KEY_BINDINGS_FILE")
            val outputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))

            // Write version for future compatibility
            outputStream.writeInt(1) // Version 1

            // Write timestamp
            outputStream.writeLong(System.currentTimeMillis())

            // Write number of configurable bindings
            val configurableBindingsToSave = configurableKeys.filter { keyBindings.containsKey(it) }
            outputStream.writeInt(configurableBindingsToSave.size)

            // Write each binding
            configurableBindingsToSave.forEach { key ->
                outputStream.writeUTF(key)
                outputStream.writeInt(keyBindings[key] ?: defaultBindings[key] ?: KeyEvent.VK_UNDEFINED)
            }

            outputStream.close()
            return true
        } catch (e: Exception) {
            println("Error saving key bindings: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun loadKeyBindings(): Boolean {
        try {
            val file = File("$SETTINGS_DIR/$KEY_BINDINGS_FILE")
            if (!file.exists()) {
                println("Key bindings file does not exist, using defaults")
                return false
            }

            val inputStream = DataInputStream(BufferedInputStream(FileInputStream(file)))

            // Read and verify version
            val version = inputStream.readInt()
            if (version != 1) {
                println("Unsupported key bindings file version: $version")
                inputStream.close()
                return false
            }

            // Read timestamp (not used currently)
            val timestamp = inputStream.readLong()

            // Read number of bindings
            val bindingCount = inputStream.readInt()

            // Read each binding
            for (i in 0 until bindingCount) {
                val key = inputStream.readUTF()
                val value = inputStream.readInt()

                // Only set if it's a configurable key
                if (key in configurableKeys) {
                    keyBindings[key] = value
                }
            }

            inputStream.close()
            return true
        } catch (e: Exception) {
            println("Error loading key bindings: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}

object KeyBindings {
    private val keyBindingManager = KeyBindingManager()

    // Movement Keys
    val MOVE_FORWARD get() = keyBindingManager.getKeyBinding("MOVE_FORWARD")
    val MOVE_BACKWARD get() = keyBindingManager.getKeyBinding("MOVE_BACKWARD")
    val MOVE_LEFT get() = keyBindingManager.getKeyBinding("MOVE_LEFT")
    val MOVE_RIGHT get() = keyBindingManager.getKeyBinding("MOVE_RIGHT")
    val FLY_UP get() = keyBindingManager.getKeyBinding("FLY_UP")
    val FLY_DOWN get() = keyBindingManager.getKeyBinding("FLY_DOWN")
    val JUMP get() = keyBindingManager.getKeyBinding("JUMP")

    // Mode Toggle
    val TOGGLE_EDITOR get() = keyBindingManager.getKeyBinding("TOGGLE_EDITOR")

    // Direction Keys (fixed)
    const val ROTATE_NORTH = KeyEvent.VK_N
    const val ROTATE_SOUTH = KeyEvent.VK_S
    const val ROTATE_EAST = KeyEvent.VK_O
    const val ROTATE_WEST = KeyEvent.VK_W
    const val TOGGLE_WEAPON_UI = KeyEvent.VK_U
    const val TOGGLE_UI_BUILDER = KeyEvent.VK_G

    // Shortcuts (fixed)
    const val ROTATE_WALL = KeyEvent.VK_R
    const val WALL_SHORTCUT = KeyEvent.VK_1
    const val FLOOR_SHORTCUT = KeyEvent.VK_2
    const val PILLAR_SHORTCUT = KeyEvent.VK_3
    const val WATER_SHORTCUT = KeyEvent.VK_4
    const val RAMP_SHORTCUT = KeyEvent.VK_5
    const val PLAYER_SPAWN_SHORTCUT = KeyEvent.VK_6

    // Undo/Redo keys (fixed)
    const val UNDO = KeyBindingManager.UNDO_KEY
    const val REDO = KeyBindingManager.REDO_KEY

    // Expose the manager for configuration UI
    fun getManager(): KeyBindingManager = keyBindingManager
}