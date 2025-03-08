package ui.components

import ObjectType
import grideditor.GridEditor
import java.awt.*
import javax.swing.*

class QuickActionsPanel(private val gridEditor: GridEditor) : JPanel() {
    // Store references to object type buttons
    private val addWallButton: JButton
    private val addFloorButton: JButton
    private val addPlayerSpawnButton: JButton
    private val clearAllButton: JButton

    // Colors for button states
    private val defaultButtonColor = Color(60, 63, 65)
    private val selectedButtonColor = Color(100, 100, 255)

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)

        // Create buttons with stored references
        addWallButton = createButton("Add Wall").apply {
            addActionListener {
                gridEditor.setObjectType(ObjectType.WALL)
                updateButtonStates(ObjectType.WALL)
                restoreFocusToGridEditor()
            }
        }

        addFloorButton = createButton("Add Floor").apply {
            addActionListener {
                gridEditor.setObjectType(ObjectType.FLOOR)
                updateButtonStates(ObjectType.FLOOR)
                restoreFocusToGridEditor()
            }
        }

        addPlayerSpawnButton = createButton("Add Player Spawn").apply {
            addActionListener {
                gridEditor.setObjectType(ObjectType.PLAYER_SPAWN)
                updateButtonStates(ObjectType.PLAYER_SPAWN)
                restoreFocusToGridEditor()
            }
        }

        clearAllButton = createButton("Clear All").apply {
            addActionListener {
                gridEditor.clearGrid()
            }
        }

        // Add buttons to panel
        add(addWallButton)
        add(Box.createVerticalStrut(5))
        add(addFloorButton)
        add(Box.createVerticalStrut(5))
        add(addPlayerSpawnButton)
        add(Box.createVerticalStrut(5))
        add(clearAllButton)
    }

    /**
     * Updates the visual state of object type buttons
     */
    private fun updateButtonStates(selectedType: ObjectType) {
        addWallButton.background = if (selectedType == ObjectType.WALL) selectedButtonColor else defaultButtonColor
        addFloorButton.background = if (selectedType == ObjectType.FLOOR) selectedButtonColor else defaultButtonColor
        addPlayerSpawnButton.background = if (selectedType == ObjectType.PLAYER_SPAWN) selectedButtonColor else defaultButtonColor
    }

    /**
     * Creates a styled button with consistent appearance
     */
    private fun createButton(text: String): JButton {
        return JButton(text).apply {
            background = defaultButtonColor
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
        }
    }

    /**
     * Restore focus to the grid editor after button click
     */
    private fun restoreFocusToGridEditor() {
        SwingUtilities.invokeLater {
            gridEditor.requestFocusInWindow()
        }
    }
}