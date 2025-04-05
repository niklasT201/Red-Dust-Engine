package ui.topbar

import saving.WorldSaver
import grideditor.GridEditor
import java.awt.Component
import java.io.File
import java.util.*
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import ui.MenuSystem
import ui.GameType

class FileManager(
    private val gridEditor: GridEditor,
    private val menuSystem: MenuSystem? = null
) {
    private val worldSaver = WorldSaver()
    private var currentSaveFile: File? = null
    private var gameType: GameType = GameType.LEVEL_BASED // Default game type
    private var currentLevelNumber: Int = 1 // For level-based games

    companion object {
        private const val WORLD_DIR = "World"
        private const val SAVES_DIR = "saves"
        private const val OPEN_WORLD_DIR = "open_world"
        private const val LEVELS_DIR = "levels"
        private const val QUICKSAVES_DIR = "quicksaves"
        private const val DEFAULT_OPEN_WORLD_FILE = "open_world.world"
        private const val DEFAULT_LEVEL_PREFIX = "level_"
        private const val WORLD_EXTENSION = "world"
    }

    init {
        // Create directory structure if it doesn't exist
        createDirectoryStructure()
    }

    fun setGameType(type: GameType) {
        gameType = type
    }

    fun getGameType(): GameType = gameType

    fun saveWorld(parentComponent: Component, saveAs: Boolean): Boolean {
        when (gameType) {
            GameType.OPEN_WORLD -> {
                return saveOpenWorld()
            }
            GameType.LEVEL_BASED -> {
                // For level-based, we either save to the current file or prompt for a new one
                if (currentSaveFile == null || saveAs) {
                    // For first level save automatically without prompting
                    if (currentSaveFile == null && !doesAnyLevelExist()) {
                        return saveFirstLevel()
                    }
                    return saveLevelWithDialog(parentComponent)
                } else {
                    return performSave()
                }
            }
        }
    }

    private fun saveOpenWorld(): Boolean {
        val openWorldFile = File(getLevelBaseDirectory(), DEFAULT_OPEN_WORLD_FILE)
        currentSaveFile = openWorldFile
        return performSave()
    }

    private fun saveFirstLevel(): Boolean {
        // Create level_1.world automatically
        val levelFile = File(getLevelBaseDirectory(), "${DEFAULT_LEVEL_PREFIX}${currentLevelNumber}.${WORLD_EXTENSION}")
        currentSaveFile = levelFile
        return performSave()
    }

    private fun saveLevelWithDialog(parentComponent: Component): Boolean {
        // Show save dialog in the levels directory
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Save Level"
            fileSelectionMode = JFileChooser.FILES_ONLY
            isAcceptAllFileFilterUsed = false
            fileFilter = FileNameExtensionFilter("World Files (*.${WORLD_EXTENSION})", WORLD_EXTENSION)
            currentDirectory = getLevelBaseDirectory()
        }

        if (fileChooser.showSaveDialog(parentComponent) == JFileChooser.APPROVE_OPTION) {
            var selectedFile = fileChooser.selectedFile
            // Add extension if not present
            if (!selectedFile.name.lowercase(Locale.getDefault()).endsWith(".${WORLD_EXTENSION}")) {
                selectedFile = File(selectedFile.absolutePath + ".${WORLD_EXTENSION}")
            }

            currentSaveFile = selectedFile
            return performSave()
        }
        return false
    }

    fun loadWorld(parentComponent: Component): Boolean {
        when (gameType) {
            GameType.OPEN_WORLD -> {
                // For Open World, just load the single file
                val openWorldFile = File(getLevelBaseDirectory(), DEFAULT_OPEN_WORLD_FILE)
                if (openWorldFile.exists()) {
                    return loadWorld(openWorldFile)
                } else {
                    // No open world file exists yet
                    return false
                }
            }
            GameType.LEVEL_BASED -> {
                // For level-based, show file chooser
                val fileChooser = JFileChooser().apply {
                    dialogTitle = "Load Level"
                    fileSelectionMode = JFileChooser.FILES_ONLY
                    isAcceptAllFileFilterUsed = false
                    fileFilter = FileNameExtensionFilter("World Files (*.${WORLD_EXTENSION})", WORLD_EXTENSION)
                    currentDirectory = getLevelBaseDirectory()
                }

                if (fileChooser.showOpenDialog(parentComponent) == JFileChooser.APPROVE_OPTION) {
                    val selectedFile = fileChooser.selectedFile
                    return loadWorld(selectedFile)
                }
            }
        }
        return false
    }

    fun loadWorld(file: File): Boolean {
        if (worldSaver.loadWorld(gridEditor, file.absolutePath)) {
            currentSaveFile = file

            // Update the menu system with discovered floors
            updateMenuFloorsAfterLoad()

            return true
        }
        return false
    }

    private fun updateMenuFloorsAfterLoad() {
        menuSystem?.let { menu ->
            // Get discovered floors from GridEditor
            val floors = gridEditor.getDiscoveredFloors()

            // Add each floor to the menu
            floors.forEach { floorLevel ->
                menu.addFloor(floorLevel)
            }

            // Set the current floor in the menu
            menu.setCurrentFloor(gridEditor.useCurrentFloor())
        }
    }

    fun quickSave(parentComponent: Component): File? {
        // Create quick save directory if it doesn't exist
        val saveDir = getQuicksaveDirectory()
        if (!saveDir.exists()) {
            saveDir.mkdir()
        }

        // Use timestamp for unique filename
        val timestamp = System.currentTimeMillis()
        val prefix = if (gameType == GameType.OPEN_WORLD) "openworld" else "level"
        val file = File(saveDir, "${prefix}_quicksave_$timestamp.${WORLD_EXTENSION}")

        if (worldSaver.saveWorld(gridEditor, file.absolutePath)) {
            return file
        }
        return null
    }

    fun quickLoad(parentComponent: Component): Boolean {
        val saveDir = getQuicksaveDirectory()
        if (!saveDir.exists() || saveDir.listFiles()?.isEmpty() != false) {
            return false
        }

        // Get the most recent quicksave for the current game type
        val prefix = if (gameType == GameType.OPEN_WORLD) "openworld" else "level"
        val files = saveDir.listFiles { file ->
            file.name.startsWith(prefix) && file.name.endsWith(".${WORLD_EXTENSION}")
        }

        val latestFile = files?.maxByOrNull { it.lastModified() }

        if (latestFile != null) {
            return loadWorld(latestFile)
        }
        return false
    }

    private fun performSave(): Boolean {
        currentSaveFile?.let { file ->
            return worldSaver.saveWorld(gridEditor, file.absolutePath)
        }
        return false
    }

    fun getCurrentFile(): File? = currentSaveFile

    fun resetCurrentFile() {
        currentSaveFile = null
    }

    private fun getWorldDirectory(): File {
        val dir = File(WORLD_DIR)
        if (!dir.exists()) dir.mkdir()
        return dir
    }

    private fun getSavesDirectory(): File {
        val dir = File(getWorldDirectory(), SAVES_DIR)
        if (!dir.exists()) dir.mkdir()
        return dir
    }

    private fun getLevelBaseDirectory(): File {
        val baseDir = when (gameType) {
            GameType.OPEN_WORLD -> File(getSavesDirectory(), OPEN_WORLD_DIR)
            GameType.LEVEL_BASED -> File(getSavesDirectory(), LEVELS_DIR)
        }

        if (!baseDir.exists()) baseDir.mkdir()
        return baseDir
    }

    private fun getQuicksaveDirectory(): File {
        val dir = File(getWorldDirectory(), QUICKSAVES_DIR)
        if (!dir.exists()) dir.mkdir()
        return dir
    }

    private fun doesAnyLevelExist(): Boolean {
        val levelsDir = File(getSavesDirectory(), LEVELS_DIR)
        return levelsDir.exists() && levelsDir.listFiles { file ->
            file.isFile && file.name.endsWith(".${WORLD_EXTENSION}")
        }?.isNotEmpty() ?: false
    }

    private fun createDirectoryStructure() {
        getWorldDirectory()
        getSavesDirectory()
        File(getSavesDirectory(), OPEN_WORLD_DIR).mkdir()
        File(getSavesDirectory(), LEVELS_DIR).mkdir()
        getQuicksaveDirectory()
    }

    // Get a list of existing worlds (both open world and levels)
    fun getExistingWorlds(): List<Pair<String, File>> {
        val result = mutableListOf<Pair<String, File>>()

        // Check open world
        val openWorldFile = File(File(getSavesDirectory(), OPEN_WORLD_DIR), DEFAULT_OPEN_WORLD_FILE)
        if (openWorldFile.exists()) {
            result.add("Open World" to openWorldFile)
        }

        // Check levels
        val levelsDir = File(getSavesDirectory(), LEVELS_DIR)
        if (levelsDir.exists()) {
            levelsDir.listFiles { file ->
                file.isFile && file.name.endsWith(".${WORLD_EXTENSION}")
            }?.forEach { file ->
                result.add("Level: ${file.nameWithoutExtension}" to file)
            }
        }

        return result
    }
}