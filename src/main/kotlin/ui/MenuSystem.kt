package ui

import WorldSaver
import javax.swing.*
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.InputEvent
import javax.swing.border.EmptyBorder
import javax.swing.border.MatteBorder
import grideditor.GridEditor
import java.io.File
import java.util.*
import javax.swing.Timer

class MenuSystem(
    onFloorSelected: (Int) -> Unit,
    onFloorAdded: (Boolean) -> Unit,
    private val gridEditor: GridEditor
) {
    private val controlsMap = mutableMapOf<String, String>()
    private val floorLevelMenu = FloorLevelMenu(onFloorSelected, onFloorAdded)
    private val worldSaver = WorldSaver()
    private var currentSaveFile: File? = null

    // Define colors as constants for consistency
    companion object {
        val BACKGROUND_COLOR = Color(40, 44, 52)
        private val BORDER_COLOR = Color(28, 31, 36)
        private val HOVER_COLOR = Color(60, 63, 65)
        private val SEPARATOR_COLOR = Color(70, 73, 75)
    }

    init {
        // Initialize default controls
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

    fun createMenuBar(): JMenuBar {
        return JMenuBar().apply {
            background = BACKGROUND_COLOR
            // Add a bottom border and subtle top border for better separation
            border = BorderFactory.createCompoundBorder(
                MatteBorder(1, 0, 1, 0, BORDER_COLOR),
                EmptyBorder(2, 2, 2, 2)
            )
            isOpaque = true

            // Add subtle gradient painter
            UIManager.put("MenuBar.gradient", null)  // Remove default gradient

            add(createFileMenu())
            add(createEditMenu())
            add(createControlsMenu())
            add(createHelpMenu())

            // Add a glue to push the floor menu to the right
            add(Box.createHorizontalGlue())

            // Add the floor level menu
            add(floorLevelMenu)
        }
    }

    fun addFloor(level: Int) {
        floorLevelMenu.addFloor(level)
    }

    fun setCurrentFloor(level: Int) {
        floorLevelMenu.setCurrentFloor(level)
    }

    private fun createMenuItem(text: String, accelerator: KeyStroke? = null): JMenuItem {
        return JMenuItem(text).apply {
            background = BACKGROUND_COLOR
            foreground = Color.WHITE
            this.accelerator = accelerator
            border = EmptyBorder(5, 15, 5, 15)

            // Hover effect
            addChangeListener { e ->
                if (isArmed) {
                    background = HOVER_COLOR
                } else {
                    background = BACKGROUND_COLOR
                }
            }
        }
    }

    private fun createMenu(text: String, mnemonic: Int): JMenu {
        return JMenu(text).apply {
            this.mnemonic = mnemonic
            background = BACKGROUND_COLOR
            foreground = Color.WHITE
            border = EmptyBorder(5, 5, 5, 5)

            // Custom popup menu style
            popupMenu.border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(2, 0, 2, 0)
            )
            popupMenu.background = BACKGROUND_COLOR

            // Add hover effect for top-level menu items
            addChangeListener { e ->
                if (isSelected || isArmed) {
                    background = HOVER_COLOR
                } else {
                    background = BACKGROUND_COLOR
                }
            }
        }
    }

    private fun createFileMenu(): JMenu {
        return createMenu("File", KeyEvent.VK_F).apply {
            add(createMenuItem("New Project",
                KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK)).apply {
                addActionListener {
                    if (confirmUnsavedChanges()) {
                        gridEditor.clearGrid()
                        currentSaveFile = null
                    }
                }
            })

            add(createMenuItem("Open Project",
                KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK)).apply {
                addActionListener {
                    if (confirmUnsavedChanges()) {
                        showLoadDialog()
                    }
                }
            })

            add(createMenuItem("Save",
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)).apply {
                addActionListener {
                    saveWorld(false)
                }
            })

            add(createMenuItem("Save As...",
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)).apply {
                addActionListener {
                    saveWorld(true)
                }
            })

            addSeparator()

            // Quick save/load
            add(createMenuItem("Quick Save",
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)).apply {
                addActionListener {
                    quickSave()
                }
            })

            add(createMenuItem("Quick Load",
                KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)).apply {
                addActionListener {
                    quickLoad()
                }
            })

            addSeparator()

            add(createMenuItem("Exit",
                KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK)).apply {
                addActionListener {
                    if (confirmUnsavedChanges()) {
                        (SwingUtilities.getWindowAncestor(this@MenuSystem.floorLevelMenu) as? JFrame)?.dispose()
                    }
                }
            })
        }
    }

    private fun saveWorld(saveAs: Boolean) {
        if (currentSaveFile == null || saveAs) {
            // Show save dialog
            val fileChooser = JFileChooser().apply {
                dialogTitle = "Save World"
                fileSelectionMode = JFileChooser.FILES_ONLY
                isAcceptAllFileFilterUsed = false
                fileFilter = javax.swing.filechooser.FileNameExtensionFilter("World Files (*.world)", "world")
                currentDirectory = File(System.getProperty("user.dir"))
            }

            if (fileChooser.showSaveDialog(floorLevelMenu) == JFileChooser.APPROVE_OPTION) {
                var selectedFile = fileChooser.selectedFile
                // Add extension if not present
                if (!selectedFile.name.lowercase(Locale.getDefault()).endsWith(".world")) {
                    selectedFile = File(selectedFile.absolutePath + ".world")
                }

                currentSaveFile = selectedFile
                performSave()
            }
        } else {
            // Use existing file
            performSave()
        }
    }

    private fun performSave() {
        currentSaveFile?.let { file ->
            if (worldSaver.saveWorld(gridEditor, file.absolutePath)) {
                JOptionPane.showMessageDialog(
                    floorLevelMenu,
                    "World saved successfully.",
                    "Save Complete",
                    JOptionPane.INFORMATION_MESSAGE
                )
            } else {
                JOptionPane.showMessageDialog(
                    floorLevelMenu,
                    "Failed to save world.",
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }

    private fun showLoadDialog() {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Load World"
            fileSelectionMode = JFileChooser.FILES_ONLY
            isAcceptAllFileFilterUsed = false
            fileFilter = javax.swing.filechooser.FileNameExtensionFilter("World Files (*.world)", "world")
            currentDirectory = File(System.getProperty("user.dir"))
        }

        if (fileChooser.showOpenDialog(floorLevelMenu) == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile
            loadWorld(selectedFile)
        }
    }

    private fun loadWorld(file: File) {
        if (worldSaver.loadWorld(gridEditor, file.absolutePath)) {
            currentSaveFile = file
            JOptionPane.showMessageDialog(
                floorLevelMenu,
                "World loaded successfully.",
                "Load Complete",
                JOptionPane.INFORMATION_MESSAGE
            )
        } else {
            JOptionPane.showMessageDialog(
                floorLevelMenu,
                "Failed to load world.",
                "Load Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun quickSave() {
        // Create quick save directory if it doesn't exist
        val saveDir = File("quicksaves")
        if (!saveDir.exists()) {
            saveDir.mkdir()
        }

        // Use timestamp for unique filename
        val timestamp = System.currentTimeMillis()
        val file = File(saveDir, "quicksave_$timestamp.world")

        if (worldSaver.saveWorld(gridEditor, file.absolutePath)) {
            // Show small notification
            val frame = SwingUtilities.getWindowAncestor(floorLevelMenu) as? JFrame
            val notification = JDialog(frame, "Quick Save", false).apply {
                layout = BorderLayout()
                add(JLabel("  Quick save successful  ", JLabel.CENTER).apply {
                    border = EmptyBorder(10, 20, 10, 20)
                }, BorderLayout.CENTER)
                pack()
                setLocationRelativeTo(frame)
                isVisible = true
            }

            // Auto-close notification after 1.5 seconds
            Timer(1500) { notification.dispose() }.apply {
                isRepeats = false
                start()
            }
        } else {
            JOptionPane.showMessageDialog(
                floorLevelMenu,
                "Quick save failed.",
                "Save Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun quickLoad() {
        val saveDir = File("quicksaves")
        if (!saveDir.exists() || saveDir.listFiles()?.isEmpty() != false) {
            JOptionPane.showMessageDialog(
                floorLevelMenu,
                "No quick saves found.",
                "Quick Load",
                JOptionPane.INFORMATION_MESSAGE
            )
            return
        }

        // Get the most recent quicksave
        val files = saveDir.listFiles { file -> file.name.endsWith(".world") }
        val latestFile = files?.maxByOrNull { it.lastModified() }

        if (latestFile != null) {
            loadWorld(latestFile)
        } else {
            JOptionPane.showMessageDialog(
                floorLevelMenu,
                "No valid quick saves found.",
                "Quick Load",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }

    private fun confirmUnsavedChanges(): Boolean {
        // In a real application, you would check if there are unsaved changes
        // For now, just confirm with the user
        val result = JOptionPane.showConfirmDialog(
            floorLevelMenu,
            "You may have unsaved changes. Continue?",
            "Unsaved Changes",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )
        return result == JOptionPane.YES_OPTION
    }

    private fun createEditMenu(): JMenu {
        return createMenu("Edit", KeyEvent.VK_E).apply {
            add(createMenuItem("Undo",
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK)))
            add(createMenuItem("Redo",
                KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK)))

            addSeparator()

            add(createMenuItem("Preferences"))
        }
    }

    private fun createControlsMenu(): JMenu {
        return createMenu("Controls", KeyEvent.VK_C).apply {
            add(createMenuItem("Show All Controls").apply {
                addActionListener { showControlsDialog() }
            })

            addSeparator()

            add(createMenuItem("Configure Controls...").apply {
                addActionListener { showControlsConfigDialog() }
            })
        }
    }

    private fun createHelpMenu(): JMenu {
        return createMenu("Help", KeyEvent.VK_H).apply {
            add(createMenuItem("Documentation",
                KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)))
            add(createMenuItem("About"))
        }
    }

    private fun showControlsDialog() {
        JDialog().apply {
            title = "Controls List"
            layout = BorderLayout()
            background = BACKGROUND_COLOR

            val textArea = JTextArea(buildControlsMessage()).apply {
                background = BACKGROUND_COLOR
                foreground = Color.WHITE
                isEditable = false
                border = EmptyBorder(10, 10, 10, 10)
                font = Font("Monospace", Font.PLAIN, 12)
            }

            add(JScrollPane(textArea).apply {
                border = BorderFactory.createLineBorder(BORDER_COLOR)
                background = BACKGROUND_COLOR
            }, BorderLayout.CENTER)

            add(JButton("Close").apply {
                background = HOVER_COLOR
                foreground = Color.WHITE
                addActionListener { dispose() }
            }, BorderLayout.SOUTH)

            setSize(400, 500)
            setLocationRelativeTo(null)
            isVisible = true
        }
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