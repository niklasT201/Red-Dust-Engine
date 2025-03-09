package ui.topbar

import ui.MenuSystem
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.*
import javax.swing.border.EmptyBorder

class ControlsManager {
    private val controlsMap = mutableMapOf<String, String>()

    init {
        initializeControls()
    }

    private fun initializeControls() {
        controlsMap.apply {
            // Movement controls
            put("Move Forward", "W")
            put("Move Backward", "S")
            put("Strafe Left", "A")
            put("Strafe Right", "D")
            put("Jump/Move Up", "SPACE")
            put("Crouch/Move Down", "SHIFT")

            // Editor controls
            put("Toggle Editor Mode", "E")
            put("Delete Selected", "DELETE")
            put("Copy", "CTRL + C")
            put("Paste", "CTRL + V")
            put("Undo", "CTRL + Z")
            put("Redo", "CTRL + Y")
            put("Save", "CTRL + S")
            put("Quick Save", "F5")
            put("Quick Load", "F9")
        }
    }

    fun showControlsDialog(parentComponent: Component) {
        JDialog().apply {
            title = "Controls List"
            layout = BorderLayout()
            background = MenuSystem.BACKGROUND_COLOR

            val textArea = JTextArea(buildControlsMessage()).apply {
                background = MenuSystem.BACKGROUND_COLOR
                foreground = Color.WHITE
                isEditable = false
                border = EmptyBorder(10, 10, 10, 10)
                font = Font("Monospace", Font.PLAIN, 12)
            }

            add(JScrollPane(textArea).apply {
                border = BorderFactory.createLineBorder(MenuSystem.BORDER_COLOR)
                background = MenuSystem.BACKGROUND_COLOR
            }, BorderLayout.CENTER)

            add(JButton("Close").apply {
                background = MenuSystem.HOVER_COLOR
                foreground = Color.WHITE
                addActionListener { dispose() }
            }, BorderLayout.SOUTH)

            setSize(400, 500)
            setLocationRelativeTo(null)
            isVisible = true
        }
    }

    fun showControlsConfigDialog(parentComponent: Component) {
        JOptionPane.showMessageDialog(
            parentComponent,
            "Controls configuration will be implemented in a future update.",
            "Configure Controls",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    private fun buildControlsMessage(): String {
        return buildString {
            appendLine("Game Controls:")
            appendLine("-------------")
            controlsMap.entries.forEach { (action, key) ->
                appendLine("$action: $key")
            }
        }
    }
}