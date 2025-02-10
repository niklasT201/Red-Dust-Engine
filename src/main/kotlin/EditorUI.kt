import java.awt.*
import java.awt.event.*
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class EditorUI(private val game: Game3D) {
    val sideBar: JPanel
    lateinit var modeButton: JButton
    lateinit var toolButtons: Map<EditorTool, JRadioButton>
    var currentFloorLevel = 0

    enum class EditorTool {
        WALL, FLOOR, PLAYER, MOVE
    }

    private var selectedTool = EditorTool.WALL
    private var isEditMode = true

    init {
        sideBar = createSideBar()
    }

    fun createMenuBar(): JMenuBar {
        val menuBar = JMenuBar()

        // File Menu
        val fileMenu = JMenu("File")
        val saveItem = JMenuItem("Save Map").apply {
            accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)
        }
        val loadItem = JMenuItem("Load Map").apply {
            accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK)
        }

        saveItem.addActionListener {
            val fileChooser = JFileChooser().apply {
                fileFilter = FileNameExtensionFilter("Map Files (*.map)", "map")
            }
            if (fileChooser.showSaveDialog(game) == JFileChooser.APPROVE_OPTION) {
                var file = fileChooser.selectedFile
                if (!file.name.endsWith(".map")) {
                    file = File(file.absolutePath + ".map")
                }
                // Implement saveMap functionality
            }
        }

        loadItem.addActionListener {
            val fileChooser = JFileChooser().apply {
                fileFilter = FileNameExtensionFilter("Map Files (*.map)", "map")
            }
            if (fileChooser.showOpenDialog(game) == JFileChooser.APPROVE_OPTION) {
                // Implement loadMap functionality
            }
        }

        // Help Menu
        val helpMenu = JMenu("Help")
        val controlsItem = JMenuItem("Controls")
        controlsItem.addActionListener {
            showControlsDialog()
        }

        fileMenu.add(saveItem)
        fileMenu.add(loadItem)
        helpMenu.add(controlsItem)
        menuBar.add(fileMenu)
        menuBar.add(helpMenu)

        return menuBar
    }

    private fun createSideBar(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        panel.preferredSize = Dimension(200, game.height)
        panel.background = Color(60, 60, 60)

        // Mode Section
        panel.add(createSectionLabel("Mode"))
        modeButton = JButton(if (isEditMode) "Edit Mode" else "Play Mode").apply {
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(180, 30)
        }
        modeButton.addActionListener {
            isEditMode = !isEditMode
            updateModeButton()
            game.requestFocusInWindow()
        }
        panel.add(modeButton)
        panel.add(Box.createVerticalStrut(20))

        // Tools Section
        panel.add(createSectionLabel("Tools"))
        val toolGroup = ButtonGroup()
        toolButtons = mutableMapOf()

        for (tool in EditorTool.values()) {
            val radioButton = JRadioButton(tool.name).apply {
                foreground = Color.WHITE
                background = panel.background
                isSelected = selectedTool == tool
                alignmentX = Component.LEFT_ALIGNMENT
                maximumSize = Dimension(180, 25)
            }
            radioButton.addActionListener {
                selectedTool = tool
                game.requestFocusInWindow()
            }
            toolGroup.add(radioButton)
            panel.add(radioButton)
            (toolButtons as MutableMap<EditorTool, JRadioButton>)[tool] = radioButton
        }
        panel.add(Box.createVerticalStrut(20))

        // Floor Controls
        val floorControlsPanel = createFloorControls()
        floorControlsPanel.alignmentX = Component.LEFT_ALIGNMENT
        panel.add(floorControlsPanel)

        // Clear Walls Button
        panel.add(Box.createVerticalStrut(20))
        val clearButton = JButton("Clear All").apply {
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(180, 30)
        }
        clearButton.addActionListener {
            // Implement clear functionality
            game.requestFocusInWindow()
        }
        panel.add(clearButton)

        return panel
    }

    private fun createFloorControls(): JPanel {
        val floorControls = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = Color(60, 60, 60)
            maximumSize = Dimension(180, 30)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        val floorLabel = JLabel("Floor Level: $currentFloorLevel").apply {
            foreground = Color.WHITE
        }

        val decreaseFloorBtn = JButton("-").apply {
            maximumSize = Dimension(40, 25)
        }
        decreaseFloorBtn.addActionListener {
            if (currentFloorLevel > 0) {
                currentFloorLevel--
                floorLabel.text = "Floor Level: $currentFloorLevel"
                game.repaint()
            }
        }

        val increaseFloorBtn = JButton("+").apply {
            maximumSize = Dimension(40, 25)
        }
        increaseFloorBtn.addActionListener {
            currentFloorLevel++
            floorLabel.text = "Floor Level: $currentFloorLevel"
            game.repaint()
        }

        floorControls.add(decreaseFloorBtn)
        floorControls.add(Box.createHorizontalStrut(10))
        floorControls.add(floorLabel)
        floorControls.add(Box.createHorizontalStrut(10))
        floorControls.add(increaseFloorBtn)
        floorControls.add(Box.createHorizontalGlue())

        return floorControls
    }

    private fun createSectionLabel(text: String): JLabel {
        return JLabel(text).apply {
            foreground = Color.WHITE
            font = font.deriveFont(Font.BOLD, 14f)
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(180, 25)
        }
    }

    private fun showControlsDialog() {
        JOptionPane.showMessageDialog(
            game,
            """
            Movement:
            WASD - Move in play mode
            Mouse - Look around
            Space - Move up
            Shift - Move down
            
            Mode Selection:
            E - Toggle edit/play mode
            
            Tools:
            1 - Wall tool
            2 - Floor tool
            3 - Player tool
            4 - Move tool
            """.trimIndent(),
            "Controls",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    fun updateModeButton() {
        modeButton.text = if (isEditMode) "Edit Mode" else "Play Mode"
    }

    fun isInEditMode(): Boolean = isEditMode
    fun getSelectedTool(): EditorTool = selectedTool
}