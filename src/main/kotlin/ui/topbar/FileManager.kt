package ui.topbar

import keyinput.KeyBindings
import saving.WorldSaver
import grideditor.GridEditor
import java.awt.Component
import java.io.File
import java.util.*
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter
import ui.MenuSystem
import ui.GameType
import javax.swing.JFrame
import javax.swing.SwingUtilities

class FileManager(
    private val gridEditor: GridEditor,
    private val menuSystem: MenuSystem? = null
) {
    private val worldSaver = WorldSaver()
    private var currentSaveFile: File? = null
    private var gameType: GameType = GameType.LEVEL_BASED // Default game type
    private var currentLevelNumber: Int = 1 // For level-based games
    private var currentProjectName: String? = null // Store the current project name

    companion object {
        const val PROJECTS_DIR = "Projects" // New top-level directory
        const val SAVES_DIR = "saves"
        const val OPEN_WORLD_DIR = "open_world"
        const val LEVELS_DIR = "levels"
        private const val QUICKSAVES_DIR = "quicksaves"
        const val DEFAULT_OPEN_WORLD_FILE = "open_world.world"
        private const val DEFAULT_LEVEL_PREFIX = "level_"
        const val WORLD_EXTENSION = "world"
        const val ASSETS_DIR = "assets"
        const val UI_DIR = "ui" // Subdirectory within assets
        const val DEFAULT_UI_FILENAME = "custom_layout.ui" // Default filename to look for
    }

    init {
        // Create base directory structure if it doesn't exist
        createBaseDirectoryStructure()
    }

    fun setGameType(type: GameType) {
        gameType = type
    }

    fun getGameType(): GameType = gameType

    fun getCurrentProjectName(): String? = currentProjectName

    fun setCurrentProjectName(name: String) {
        currentProjectName = name
        // Create project directory structure when setting the project name
        createProjectDirectoryStructure(name)

        // Update key bindings to use the new project
        KeyBindings.setCurrentProject(name)
    }

    fun saveWorld(parentComponent: Component, saveAs: Boolean): Boolean {
        // If no project name is set yet, prompt for it
        if (currentProjectName == null) {
            val projectName = promptForProjectName(parentComponent)
            if (projectName.isNullOrBlank()) {
                return false // User cancelled or entered invalid name
            }
            setCurrentProjectName(projectName)
        }

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

    private fun promptForProjectName(parentComponent: Component): String? {
        val frame = SwingUtilities.getWindowAncestor(parentComponent) as? JFrame
        val pane = JOptionPane(
            "Enter a name for your project:",
            JOptionPane.QUESTION_MESSAGE,
            JOptionPane.OK_CANCEL_OPTION
        )
        pane.wantsInput = true
        val dialog = pane.createDialog(frame, "Project Name")
        dialog.setLocationRelativeTo(frame) // Center the dialog
        dialog.isVisible = true

        return if (pane.value == JOptionPane.OK_OPTION) pane.inputValue as? String else null
    }

    private fun saveOpenWorld(): Boolean {
        // Make sure we have a project name
        if (currentProjectName == null) return false

        val openWorldFile = File(getOpenWorldDirectory(), DEFAULT_OPEN_WORLD_FILE)
        currentSaveFile = openWorldFile
        return performSave()
    }

    private fun saveFirstLevel(): Boolean {
        // Make sure we have a project name
        if (currentProjectName == null) return false

        // Create level_1.world automatically
        val levelFile = File(getLevelsDirectory(), "${DEFAULT_LEVEL_PREFIX}${currentLevelNumber}.${WORLD_EXTENSION}")
        currentSaveFile = levelFile
        return performSave()
    }

    private fun saveLevelWithDialog(parentComponent: Component): Boolean {
        // Make sure we have a project name
        if (currentProjectName == null) return false

        // Show save dialog in the levels directory
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Save Level"
            fileSelectionMode = JFileChooser.FILES_ONLY
            isAcceptAllFileFilterUsed = false
            fileFilter = FileNameExtensionFilter("World Files (*.${WORLD_EXTENSION})", WORLD_EXTENSION)
            currentDirectory = getLevelsDirectory()
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
        // First let the user select a project
        val projectDir = selectProjectDirectory(parentComponent)
        if (projectDir == null || !projectDir.exists() || !projectDir.isDirectory) {
            return false
        }

        // Set the current project name based on the selected directory
        currentProjectName = projectDir.name

        // Now let them choose a file within that project
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Load World"
            fileSelectionMode = JFileChooser.FILES_ONLY
            isAcceptAllFileFilterUsed = false
            fileFilter = FileNameExtensionFilter("World Files (*.${WORLD_EXTENSION})", WORLD_EXTENSION)

            // Look in the right directory based on what's available
            val openWorldDir = File(projectDir, "$SAVES_DIR/$OPEN_WORLD_DIR")
            val levelsDir = File(projectDir, "$SAVES_DIR/$LEVELS_DIR")

            currentDirectory = if (openWorldDir.exists() && openWorldDir.list()?.isNotEmpty() == true) {
                gameType = GameType.OPEN_WORLD
                openWorldDir
            } else if (levelsDir.exists() && levelsDir.list()?.isNotEmpty() == true) {
                gameType = GameType.LEVEL_BASED
                levelsDir
            } else {
                File(projectDir, SAVES_DIR)
            }
        }

        if (fileChooser.showOpenDialog(parentComponent) == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile

            // Determine game type based on file location
            if (selectedFile.absolutePath.contains("/$OPEN_WORLD_DIR/")) {
                gameType = GameType.OPEN_WORLD
            } else if (selectedFile.absolutePath.contains("/$LEVELS_DIR/")) {
                gameType = GameType.LEVEL_BASED
            }

            return loadWorld(selectedFile)
        }

        return false
    }

    private fun selectProjectDirectory(parentComponent: Component): File? {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Select Project"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            currentDirectory = File(PROJECTS_DIR)
        }

        return if (fileChooser.showOpenDialog(parentComponent) == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile
        } else {
            null
        }
    }

    fun loadWorld(file: File): Boolean {
        if (worldSaver.loadWorld(gridEditor, file.absolutePath)) {
            currentSaveFile = file

            // Make sure key bindings are updated to the current project
            KeyBindings.setCurrentProject(currentProjectName)

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
        // Make sure we have a project name
        if (currentProjectName == null) {
            val projectName = promptForProjectName(parentComponent)
            if (projectName.isNullOrBlank()) {
                return null // User cancelled or entered invalid name
            }
            setCurrentProjectName(projectName)
        }

        // Create quick save directory if it doesn't exist
        val saveDir = getQuicksavesDirectory()
        if (saveDir != null) {
            if (!saveDir.exists()) {
                saveDir.mkdirs()
            }
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
        // Make sure we have a project selected
        if (currentProjectName == null) {
            val projectDir = selectProjectDirectory(parentComponent)
            if (projectDir == null || !projectDir.exists() || !projectDir.isDirectory) {
                return false
            }
            currentProjectName = projectDir.name
        }

        val saveDir = getQuicksavesDirectory()
        if (saveDir != null) {
            if (!saveDir.exists() || saveDir.listFiles()?.isEmpty() != false) {
                return false
            }
        }

        // Get the most recent quicksave for the current game type
        val prefix = if (gameType == GameType.OPEN_WORLD) "openworld" else "level"
        val files = saveDir?.listFiles { file ->
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

    private fun getProjectsDirectory(): File {
        val dir = File(PROJECTS_DIR)
        if (!dir.exists()) dir.mkdir()
        return dir
    }

    fun getProjectDirectory(): File? {
        currentProjectName?.let { name ->
            val dir = File(getProjectsDirectory(), name)
            if (!dir.exists()) dir.mkdir()
            return dir
        }
        return null
    }

    private fun getSavesDirectory(): File? {
        return getProjectDirectory()?.let { projectDir ->
            val dir = File(projectDir, SAVES_DIR)
            if (!dir.exists()) dir.mkdir()
            return dir
        }
    }

    private fun getOpenWorldDirectory(): File? {
        return getSavesDirectory()?.let { savesDir ->
            val dir = File(savesDir, OPEN_WORLD_DIR)
            if (!dir.exists()) dir.mkdir()
            return dir
        }
    }

    private fun getLevelsDirectory(): File? {
        return getSavesDirectory()?.let { savesDir ->
            val dir = File(savesDir, LEVELS_DIR)
            if (!dir.exists()) dir.mkdir()
            return dir
        }
    }

    private fun getQuicksavesDirectory(): File? {
        return getProjectDirectory()?.let { projectDir ->
            val dir = File(projectDir, QUICKSAVES_DIR)
            if (!dir.exists()) dir.mkdir()
            return dir
        }
    }

    private fun doesAnyLevelExist(): Boolean {
        val levelsDir = getLevelsDirectory() ?: return false
        return levelsDir.exists() && levelsDir.listFiles { file ->
            file.isFile && file.name.endsWith(".${WORLD_EXTENSION}")
        }?.isNotEmpty() ?: false
    }

    private fun createBaseDirectoryStructure() {
        // Just create the Projects directory
        getProjectsDirectory()
    }

    fun getAssetsDirectory(): File? {
        return getProjectDirectory()?.let { projectDir ->
            val dir = File(projectDir, ASSETS_DIR)
            if (!dir.exists()) dir.mkdirs() // Use mkdirs for safety
            return dir
        }
    }

    fun getUiDirectory(): File? {
        return getAssetsDirectory()?.let { assetsDir ->
            val dir = File(assetsDir, UI_DIR)
            if (!dir.exists()) dir.mkdirs()
            return dir
        }
    }

    private fun createProjectDirectoryStructure(projectName: String) {
        // Set current project name
        currentProjectName = projectName

        // Create the project directory and subdirectories
        getProjectDirectory()
        getSavesDirectory()

        // Create game-type specific directories based on the selected game type
        if (gameType == GameType.OPEN_WORLD) {
            getOpenWorldDirectory()
        } else {
            getLevelsDirectory()
        }

        // Create quicksaves directory
        getQuicksavesDirectory()

        // Ensure Assets and UI directories are created
        getAssetsDirectory()
        getUiDirectory()
    }

    // Get a list of existing projects
    fun getExistingProjects(): List<String> {
        val projectsDir = getProjectsDirectory()
        return projectsDir.listFiles { file -> file.isDirectory }?.map { it.name } ?: emptyList()
    }

    // Get a list of existing worlds (both open world and levels) for the current project
    fun getExistingWorlds(): List<Pair<String, File>> {
        val result = mutableListOf<Pair<String, File>>()

        // Check open world
        getOpenWorldDirectory()?.let { openWorldDir ->
            val openWorldFile = File(openWorldDir, DEFAULT_OPEN_WORLD_FILE)
            if (openWorldFile.exists()) {
                result.add("Open World" to openWorldFile)
            }
        }

        // Check levels
        getLevelsDirectory()?.let { levelsDir ->
            if (levelsDir.exists()) {
                levelsDir.listFiles { file ->
                    file.isFile && file.name.endsWith(".${WORLD_EXTENSION}")
                }?.forEach { file ->
                    result.add("Level: ${file.nameWithoutExtension}" to file)
                }
            }
        }

        return result
    }
}