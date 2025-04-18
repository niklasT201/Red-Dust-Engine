package ui

import Game3D
import javax.swing.*
import java.awt.*
import javax.swing.border.EmptyBorder
import javax.swing.border.MatteBorder
import grideditor.GridEditor
import Renderer
import player.Player
import ui.topbar.ControlsManager
import ui.topbar.FileManager
import ui.topbar.MenuBuilder
import saving.SettingsManager

class MenuSystem(
    onFloorSelected: (Int) -> Unit,
    onFloorAdded: (Boolean) -> Unit,
    gridEditor: GridEditor,
    renderer: Renderer,
    game3D: Game3D,
    player: Player
) {
    private val floorLevelMenu = FloorLevelMenu(onFloorSelected, onFloorAdded)
    private val fileManager: FileManager = FileManager(gridEditor, this)
    private val settingsManager = SettingsManager(renderer, game3D, player, gridEditor)
    private val controlsManager = ControlsManager()
    private var menuBuilder: MenuBuilder = MenuBuilder(fileManager, controlsManager, settingsManager, gridEditor)

    companion object {
        val BACKGROUND_COLOR = Color(40, 44, 52)
        val BORDER_COLOR = Color(28, 31, 36)
        val HOVER_COLOR = Color(60, 63, 65)
    }

    init {
        // Property change listener to GridEditor for floor discovery
        gridEditor.addPropertyChangeListener("floorsDiscovered") { event ->
            val floors = event.newValue as Set<Int>
            floors.forEach { floor ->
                floorLevelMenu.addFloor(floor)
            }
        }
    }

    fun createMenuBar(): JMenuBar {
        return JMenuBar().apply {
            background = BACKGROUND_COLOR
            border = BorderFactory.createCompoundBorder(
                MatteBorder(1, 0, 1, 0, BORDER_COLOR),
                EmptyBorder(2, 2, 2, 2)
            )
            isOpaque = true

            UIManager.put("MenuBar.gradient", null)

            add(menuBuilder.createFileMenu(floorLevelMenu))
            add(menuBuilder.createControlsMenu(floorLevelMenu))
            add(menuBuilder.createHelpMenu(floorLevelMenu))

            add(Box.createHorizontalGlue())
            add(floorLevelMenu)
        }
    }

    fun addFloor(level: Int) {
        floorLevelMenu.addFloor(level)
    }

    fun setCurrentFloor(level: Int) {
        floorLevelMenu.setCurrentFloor(level)
    }

    // Add this method to expose the file manager
    fun getFileManager(): FileManager = fileManager
    fun getSettingsManager(): SettingsManager = settingsManager

}