import javax.swing.*
import java.awt.event.KeyEvent
import java.awt.event.InputEvent

class MenuSystem {
    private val controlsMap = mutableMapOf<String, String>()

    init {
        // Initialize default controls
        initializeControls()
    }

    private fun initializeControls() {
        // Movement controls
        controlsMap["Move Forward"] = "W"
        controlsMap["Move Backward"] = "S"
        controlsMap["Strafe Left"] = "A"
        controlsMap["Strafe Right"] = "D"
        controlsMap["Jump/Move Up"] = "SPACE"
        controlsMap["Crouch/Move Down"] = "SHIFT"

        // Editor controls
        controlsMap["Toggle Editor Mode"] = "E"
        controlsMap["Delete Selected"] = "DELETE"
        controlsMap["Copy"] = "CTRL + C"
        controlsMap["Paste"] = "CTRL + V"
        controlsMap["Undo"] = "CTRL + Z"
        controlsMap["Redo"] = "CTRL + Y"
        controlsMap["Save"] = "CTRL + S"
        controlsMap["Quick Save"] = "F5"
        controlsMap["Quick Load"] = "F9"
    }

    fun createMenuBar(): JMenuBar {
        return JMenuBar().apply {
            add(createFileMenu())
            add(createEditMenu())
            add(createControlsMenu())
            add(createHelpMenu())
        }
    }

    private fun createFileMenu(): JMenu {
        return JMenu("File").apply {
            mnemonic = KeyEvent.VK_F

            add(JMenuItem("New Project").apply {
                accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK)
            })

            add(JMenuItem("Open Project").apply {
                accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK)
            })

            add(JMenuItem("Save").apply {
                accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)
            })

            add(JMenuItem("Save As...").apply {
                accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)
            })

            addSeparator()

            add(JMenuItem("Exit").apply {
                accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK)
            })
        }
    }

    private fun createEditMenu(): JMenu {
        return JMenu("Edit").apply {
            mnemonic = KeyEvent.VK_E

            add(JMenuItem("Undo").apply {
                accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK)
            })

            add(JMenuItem("Redo").apply {
                accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK)
            })

            addSeparator()

            add(JMenuItem("Preferences"))
        }
    }

    private fun createControlsMenu(): JMenu {
        return JMenu("Controls").apply {
            mnemonic = KeyEvent.VK_C

            add(JMenuItem("Show All Controls").apply {
                addActionListener {
                    showControlsDialog()
                }
            })

            addSeparator()

            add(JMenuItem("Configure Controls...").apply {
                addActionListener {
                    showControlsConfigDialog()
                }
            })
        }
    }

    private fun createHelpMenu(): JMenu {
        return JMenu("Help").apply {
            mnemonic = KeyEvent.VK_H

            add(JMenuItem("Documentation").apply {
                accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)
            })

            add(JMenuItem("About"))
        }
    }

    private fun showControlsDialog() {
        val message = buildControlsMessage()
        JOptionPane.showMessageDialog(
            null,
            message,
            "Controls List",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    private fun showControlsConfigDialog() {
        JOptionPane.showMessageDialog(
            null,
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