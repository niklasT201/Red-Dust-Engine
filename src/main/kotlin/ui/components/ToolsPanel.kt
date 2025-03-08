package ui.components

import grideditor.GridEditor
import java.awt.*
import javax.swing.*

class ToolsPanel(private val gridEditor: GridEditor) : JPanel() {
    // Tool buttons
    private val selectButton: JButton
    private val moveButton: JButton
    private val rotateButton: JButton

    // Colors for button states
    private val defaultButtonColor = Color(60, 63, 65)
    private val selectedButtonColor = Color(100, 100, 255)

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)

        // Create tool buttons
        selectButton = createButton("Select").apply {
            addActionListener {
                handleToolButtonClick(this, GridEditor.EditMode.SELECT)
            }
        }

        moveButton = createButton("Move").apply {
            addActionListener {
                handleToolButtonClick(this, GridEditor.EditMode.MOVE)
            }
        }

        rotateButton = createButton("Rotate").apply {
            addActionListener {
                handleToolButtonClick(this, GridEditor.EditMode.ROTATE)
            }
        }

        // Add buttons to panel
        add(selectButton)
        add(Box.createVerticalStrut(5))
        add(moveButton)
        add(Box.createVerticalStrut(5))
        add(rotateButton)
    }

    /**
     * Handles tool button clicks, toggling between tool mode and draw mode
     */
    private fun handleToolButtonClick(button: JButton, mode: GridEditor.EditMode) {
        if (button.background == defaultButtonColor) {
            updateToolButtonStates(button)
            gridEditor.setEditMode(mode)
        } else {
            button.background = defaultButtonColor
            gridEditor.setEditMode(GridEditor.EditMode.DRAW)
        }
    }

    /**
     * Updates the visual state of tool buttons
     */
    private fun updateToolButtonStates(activeButton: JButton) {
        val toolButtons = listOf(selectButton, moveButton, rotateButton)
        toolButtons.forEach { button ->
            button.background = if (button == activeButton) selectedButtonColor else defaultButtonColor
        }
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
}