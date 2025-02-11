import javax.swing.*
import java.awt.*
import java.awt.event.*
import java.io.File
import javax.swing.filechooser.FileNameExtensionFilter

class EditorUI(private val game: Game3D) {
    val sideBar = JPanel()
    private val gridEditor = GridEditor(800, 600)
    private var isEditMode = true
    private var currentFloorLevel = 0
    private lateinit var modeButton: JButton
    private lateinit var floorLevelLabel: JLabel

    init {
        setupSideBar()
    }

    private fun setupSideBar() {
        sideBar.apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            background = Color(60, 60, 60)
            preferredSize = Dimension(200, game.height)
        }

        // Mode Section
        addSectionLabel("Mode")
        setupModeToggle()
        sideBar.add(Box.createVerticalStrut(20))

        // Tools Section
        addSectionLabel("Tools")
        setupToolButtons()
        sideBar.add(Box.createVerticalStrut(20))

        // Floor Controls
        addFloorControls()
        sideBar.add(Box.createVerticalStrut(20))

        // Clear Button
        addClearButton()
    }

    private fun addSectionLabel(text: String) {
        sideBar.add(JLabel(text).apply {
            foreground = Color.WHITE
            font = font.deriveFont(Font.BOLD, 14f)
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(180, 25)
        })
    }

    private fun setupModeToggle() {
        modeButton = JButton(if (isEditMode) "Edit Mode" else "Play Mode").apply {
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(180, 30)
            addActionListener {
                isEditMode = !isEditMode
                text = if (isEditMode) "Edit Mode" else "Play Mode"
                game.requestFocusInWindow()
            }
        }
        sideBar.add(modeButton)
    }

    private fun setupToolButtons() {
        val toolGroup = ButtonGroup()
        GridEditor.EditorTool.values().forEach { tool ->
            JRadioButton(tool.name).apply {
                foreground = Color.WHITE
                background = sideBar.background
                alignmentX = Component.LEFT_ALIGNMENT
                maximumSize = Dimension(180, 25)
                addActionListener {
                    gridEditor.setTool(tool)
                    game.requestFocusInWindow()
                }
                toolGroup.add(this)
                sideBar.add(this)
                isSelected = tool == GridEditor.EditorTool.WALL
            }
        }
    }

    private fun addFloorControls() {
        val floorControls = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = Color(60, 60, 60)
            maximumSize = Dimension(180, 30)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        floorLevelLabel = JLabel("Floor Level: $currentFloorLevel").apply {
            foreground = Color.WHITE
        }

        val decreaseBtn = JButton("-").apply {
            maximumSize = Dimension(40, 25)
            addActionListener { adjustFloorLevel(-1) }
        }

        val increaseBtn = JButton("+").apply {
            maximumSize = Dimension(40, 25)
            addActionListener { adjustFloorLevel(1) }
        }

        floorControls.apply {
            add(decreaseBtn)
            add(Box.createHorizontalStrut(10))
            add(floorLevelLabel)
            add(Box.createHorizontalStrut(10))
            add(increaseBtn)
            add(Box.createHorizontalGlue())
        }

        sideBar.add(floorControls)
    }

    private fun adjustFloorLevel(delta: Int) {
        if (currentFloorLevel + delta >= 0) {
            currentFloorLevel += delta
            floorLevelLabel.text = "Floor Level: $currentFloorLevel"
            game.repaint()
        }
    }

    private fun addClearButton() {
        JButton("Clear All").apply {
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(180, 30)
            addActionListener {
                gridEditor.clearAll()
                game.requestFocusInWindow()
            }
            sideBar.add(this)
        }
    }

     fun createMenuBar(): JMenuBar {
        val menuBar = JMenuBar()

        // File Menu
        val fileMenu = JMenu("File").apply {
            add(createMenuItem("New", KeyEvent.VK_N) {
                gridEditor.clearAll() // This will clear both walls and floors
                gridEditor.repaint()
            })
            add(createMenuItem("Save", KeyEvent.VK_S) { showSaveDialog() })
            add(createMenuItem("Load", KeyEvent.VK_O) { showLoadDialog() })
        }

        // Help Menu
        val helpMenu = JMenu("Help").apply {
            add(JMenuItem("Controls").apply {
                addActionListener { showControlsDialog() }
            })
        }

        menuBar.add(fileMenu)
        menuBar.add(helpMenu)
        return menuBar
    }

    private fun createMenuItem(text: String, keystroke: Int, action: () -> Unit): JMenuItem {
        return JMenuItem(text).apply {
            accelerator = KeyStroke.getKeyStroke(keystroke, InputEvent.CTRL_DOWN_MASK)
            addActionListener { action() }
        }
    }

    private fun showSaveDialog() {
        JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("Map Files (*.map)", "map")
            if (showSaveDialog(game) == JFileChooser.APPROVE_OPTION) {
                var file = selectedFile
                if (!file.name.endsWith(".map")) {
                    file = File(file.absolutePath + ".map")
                }
                // Implement save functionality
                // You'll need to implement the actual saving logic based on your map format
            }
        }
    }

    private fun showLoadDialog() {
        JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("Map Files (*.map)", "map")
            if (showOpenDialog(game) == JFileChooser.APPROVE_OPTION) {
                // Implement load functionality
                // You'll need to implement the actual loading logic based on your map format
            }
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
    fun getCurrentFloorLevel(): Int = currentFloorLevel
}