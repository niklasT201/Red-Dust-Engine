package ui.components

import grideditor.GridEditor
import java.awt.Color
import javax.swing.JButton
import ui.components.ButtonFactory

class ToolManager(private val gridEditor: GridEditor) {
    private val toolButtons = mutableListOf<JButton>()

    fun createSelectButton(): JButton {
        return ButtonFactory.createButton("Select").apply {
            addActionListener {
                handleToolButtonClick(this, GridEditor.EditMode.SELECT)
            }
            toolButtons.add(this)
        }
    }

    fun createMoveButton(): JButton {
        return ButtonFactory.createButton("Move").apply {
            addActionListener {
                handleToolButtonClick(this, GridEditor.EditMode.MOVE)
            }
            toolButtons.add(this)
        }
    }

    fun createRotateButton(): JButton {
        return ButtonFactory.createButton("Rotate").apply {
            addActionListener {
                handleToolButtonClick(this, GridEditor.EditMode.ROTATE)
            }
            toolButtons.add(this)
        }
    }

    private fun handleToolButtonClick(button: JButton, mode: GridEditor.EditMode) {
        if (button.background == ButtonFactory.defaultButtonColor) {
            updateToolButtonStates(button)
            gridEditor.setEditMode(mode)
        } else {
            button.background = ButtonFactory.defaultButtonColor
            gridEditor.setEditMode(GridEditor.EditMode.DRAW)
        }
    }

    fun updateToolButtonStates(activeButton: JButton) {
        toolButtons.forEach { button ->
            if (button == activeButton) {
                button.background = ButtonFactory.selectedButtonColor
            } else {
                button.background = ButtonFactory.defaultButtonColor
            }
        }
    }
}