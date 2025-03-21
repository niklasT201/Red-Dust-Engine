package ui.topbar

import saving.WorldSaver
import grideditor.GridEditor
import java.awt.Component
import java.io.File
import java.util.*
import javax.swing.JFileChooser
import ui.MenuSystem

class FileManager(
    private val gridEditor: GridEditor,
    private val menuSystem: MenuSystem? = null
) {
    private val worldSaver = WorldSaver()
    private var currentSaveFile: File? = null

    fun saveWorld(parentComponent: Component, saveAs: Boolean): Boolean {
        if (currentSaveFile == null || saveAs) {
            // Show save dialog
            val fileChooser = JFileChooser().apply {
                dialogTitle = "Save World"
                fileSelectionMode = JFileChooser.FILES_ONLY
                isAcceptAllFileFilterUsed = false
                fileFilter = javax.swing.filechooser.FileNameExtensionFilter("World Files (*.world)", "world")
                currentDirectory = File(System.getProperty("user.dir"))
            }

            if (fileChooser.showSaveDialog(parentComponent) == JFileChooser.APPROVE_OPTION) {
                var selectedFile = fileChooser.selectedFile
                // Add extension if not present
                if (!selectedFile.name.lowercase(Locale.getDefault()).endsWith(".world")) {
                    selectedFile = File(selectedFile.absolutePath + ".world")
                }

                currentSaveFile = selectedFile
                return performSave()
            }
            return false
        } else {
            // Use existing file
            return performSave()
        }
    }

    fun loadWorld(parentComponent: Component): Boolean {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Load World"
            fileSelectionMode = JFileChooser.FILES_ONLY
            isAcceptAllFileFilterUsed = false
            fileFilter = javax.swing.filechooser.FileNameExtensionFilter("World Files (*.world)", "world")
            currentDirectory = File(System.getProperty("user.dir"))
        }

        if (fileChooser.showOpenDialog(parentComponent) == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile
            return loadWorld(selectedFile)
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
            menu.setCurrentFloor(gridEditor.getCurrentFloor())
        }
    }

    fun quickSave(parentComponent: Component): File? {
        // Create quick save directory if it doesn't exist
        val saveDir = File("quicksaves")
        if (!saveDir.exists()) {
            saveDir.mkdir()
        }

        // Use timestamp for unique filename
        val timestamp = System.currentTimeMillis()
        val file = File(saveDir, "quicksave_$timestamp.world")

        if (worldSaver.saveWorld(gridEditor, file.absolutePath)) {
            return file
        }
        return null
    }

    fun quickLoad(parentComponent: Component): Boolean {
        val saveDir = File("quicksaves")
        if (!saveDir.exists() || saveDir.listFiles()?.isEmpty() != false) {
            return false
        }

        // Get the most recent quicksave
        val files = saveDir.listFiles { file -> file.name.endsWith(".world") }
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
}