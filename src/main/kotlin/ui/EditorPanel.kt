package ui

import ObjectType
import WallObject
import grideditor.GridEditor
import texturemanager.ResourceManager
import texturemanager.TextureManagerPanel
import ui.components.WallPropertiesPanel
import java.awt.*
import javax.swing.*
import javax.swing.border.TitledBorder

class EditorPanel(private val onModeSwitch: () -> Unit) : JPanel() {
    var gridEditor = GridEditor()
    private val resourceManager = ResourceManager()
    private val textureManager = TextureManagerPanel(resourceManager)
    val sectionChooser = FloorSelectorPanel()
    private val modeButton = JButton("Editor Mode")
    private val mainPanel = JPanel()
    private val wallStyleGroup = ButtonGroup()
    private var onWallStyleChange: ((Boolean) -> Unit)? = null

    //Wall properties panel
    private val wallPropertiesPanel = WallPropertiesPanel()

    // Store references to object type buttons
    private lateinit var addWallButton: JButton
    private lateinit var addFloorButton: JButton
    private lateinit var addPlayerSpawnButton: JButton

    // Colors for button states
    private val defaultButtonColor = Color(60, 63, 65)
    private val selectedButtonColor = Color(100, 100, 255)

    // Reference to wall property buttons for updating
    private var selectButton: JButton
    private var moveButton: JButton
    private var rotateButton: JButton

    init {
        layout = BorderLayout()
        background = Color(40, 44, 52)

        setupModeButton()
        setupMainPanel()
        setupSelectionHandling()
        setupWallPropertiesPanel()

        // Initialize the components with each other
        gridEditor.initializeResourceManager(resourceManager)
        gridEditor.initializeTextureManagerPanel(textureManager)
        textureManager.gridEditor = gridEditor

        // NEW: Connect wall properties panel to grid editor
        wallPropertiesPanel.setGridEditor(gridEditor)

        // Create sections container
        val sectionsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
        }

        // Top panel with mode button and separator
        val topPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createEmptyBorder(10, 5, 5, 5)

            add(modeButton)
            add(Box.createVerticalStrut(10))
            add(JSeparator())
            add(Box.createVerticalStrut(10))

            // Set maximum size to prevent stretching
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }

        textureManager.setTextureSelectionListener(object : TextureManagerPanel.TextureSelectionListener {
            override fun onTextureSetAsDefault(entry: TextureManagerPanel.TextureEntry, objectType: ObjectType) {
                // Apply the texture to the grid editor based on type
                when (objectType) {
                    ObjectType.WALL -> {
                        gridEditor.setWallTexture(entry.imageEntry)
                        gridEditor.currentWallTexture = entry.imageEntry
                        println("Listener: Applied wall texture '${entry.imageEntry.name}' to GridEditor")
                    }
                    ObjectType.FLOOR -> {
                        gridEditor.setFloorTexture(entry.imageEntry)
                        gridEditor.currentFloorTexture = entry.imageEntry
                        println("Listener: Applied floor texture '${entry.imageEntry.name}' to GridEditor")
                    }
                    // Handle other types
                    else -> {
                        println("Listener: Object type ${objectType.name} not handled for texture application")
                    }
                }
            }
        })

        // Create collapsible sections
        val quickActionsSection = CollapsibleSection("Quick Actions").apply {
            addComponent(addWallButton)
            addComponent(addFloorButton)
            addComponent(addPlayerSpawnButton)
            addComponent(createButton("Clear All").apply {
                addActionListener {
                    gridEditor.clearGrid()
                }
            })
        }

        val wallStyleSection = CollapsibleSection("Wall Style").apply {
            // Create wall style components
            val flatWallRadio = JRadioButton("Flat Walls").apply {
                isSelected = true
                background = Color(40, 44, 52)
                foreground = Color.WHITE
                alignmentX = Component.LEFT_ALIGNMENT
                addActionListener { onWallStyleChange?.invoke(false) }
            }

            val blockWallRadio = JRadioButton("Block Walls").apply {
                background = Color(40, 44, 52)
                foreground = Color.WHITE
                alignmentX = Component.LEFT_ALIGNMENT
                addActionListener { onWallStyleChange?.invoke(true) }
            }

            wallStyleGroup.add(flatWallRadio)
            wallStyleGroup.add(blockWallRadio)

            val visualizationToggle = JCheckBox("Show Flat Walls as Lines").apply {
                background = Color(40, 44, 52)
                foreground = Color.WHITE
                alignmentX = Component.LEFT_ALIGNMENT
                addActionListener {
                    gridEditor.setFlatWallVisualization(isSelected)
                }
            }

            addComponent(flatWallRadio)
            addComponent(blockWallRadio)
            addComponent(visualizationToggle)
        }

        // NEW: Wall properties section
        val wallPropertiesSection = CollapsibleSection("Wall Properties").apply {
            addComponent(wallPropertiesPanel)
        }

        val toolsSection = CollapsibleSection("Tools").apply {
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

            addComponent(selectButton)
            addComponent(moveButton)
            addComponent(rotateButton)
        }

        sectionChooser.addPropertyChangeListener { evt ->
            when (evt.propertyName) {
                "currentFloorChanged" -> {
                    val floor = evt.newValue as FloorSelectorPanel.Floor
                    gridEditor.setCurrentFloor(floor.level) // Set the current floor
                    gridEditor.updateCurrentFloorHeight(floor.heightOffset) // Set the height offset
                }
            }
        }

        val imageSection = CollapsibleSection("Textures").apply {
            addComponent(textureManager)
        }

        // Add sections to the panel
        sectionsPanel.add(topPanel)
        sectionsPanel.add(quickActionsSection)
        sectionsPanel.add(Box.createVerticalStrut(10))  // Increased spacing between sections
        sectionsPanel.add(wallStyleSection)
        sectionsPanel.add(Box.createVerticalStrut(10))
        sectionsPanel.add(wallPropertiesSection)
        sectionsPanel.add(Box.createVerticalStrut(10))
        sectionsPanel.add(toolsSection)
        sectionsPanel.add(Box.createVerticalStrut(10))
        sectionsPanel.add(imageSection)

        // Add rigid area at the bottom to prevent stretching
        sectionsPanel.add(Box.createVerticalGlue())

        // Wrap sectionsPanel in a panel that handles alignment
        val wrapperPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createEmptyBorder(0, 10, 10, 10)

            add(sectionsPanel)
            add(Box.createVerticalGlue())
        }

        // Add scroll pane
        val scrollPane = JScrollPane(wrapperPanel).apply {
            border = null
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER

            // Make the scrollbar invisible but functional
            verticalScrollBar.apply {
                unitIncrement = 5 // Adjust scroll speed
                setUI(InvisibleScrollBarUI())
                preferredSize = Dimension(0, 0) // Make it take up no space
            }

            // Optional: Add mouse wheel listener for smoother scrolling
            addMouseWheelListener { e ->
                val scrollBar = verticalScrollBar
                val increment = if (e.wheelRotation > 0) 30 else -30
                scrollBar.value += increment
            }
        }

        add(scrollPane, BorderLayout.CENTER)
    }

    private fun setupWallPropertiesPanel() {
        // Connect wall properties panel with EditorPanel via listener
        wallPropertiesPanel.setWallPropertyChangeListener(object : WallPropertiesPanel.WallPropertyChangeListener {
            override fun onWallColorChanged(color: Color) {
                gridEditor.setWallColor(color)
            }

            override fun onWallHeightChanged(height: Double) {
                gridEditor.setWallHeight(height)
            }

            override fun onWallWidthChanged(width: Double) {
                gridEditor.setWallWidth(width)
            }
        })
    }

    private fun handleToolButtonClick(button: JButton, mode: GridEditor.EditMode) {
        if (button.background == Color(60, 63, 65)) {
            updateToolButtonStates(button)
            gridEditor.setEditMode(mode)
        } else {
            button.background = Color(60, 63, 65)
            gridEditor.setEditMode(GridEditor.EditMode.DRAW)
        }
    }

    fun setModeButtonText(text: String) {
        modeButton.text = text
    }

    fun setWallStyleChangeListener(listener: (Boolean) -> Unit) {
        onWallStyleChange = listener
    }

    private fun setupSelectionHandling() {
        gridEditor.setOnCellSelectedListener { cellData ->
            // Update the property buttons with selected cell's values
            cellData?.let { cell ->
                // Use the getter instead of directly accessing the property
                val currentFloorObjects = cell.getObjectsForFloor(gridEditor.getCurrentFloor())

                val wallObject = currentFloorObjects.filterIsInstance<WallObject>().firstOrNull()

                wallObject?.let {
                    // NEW: Update wall properties panel instead of individual buttons
                    wallPropertiesPanel.updateProperties(
                        color = it.color,
                        height = it.height,
                        width = it.width
                    )
                }
            }
        }
    }

    private fun updateButtonStates(selectedType: ObjectType) {
        addWallButton.background = if (selectedType == ObjectType.WALL) selectedButtonColor else defaultButtonColor
        addFloorButton.background = if (selectedType == ObjectType.FLOOR) selectedButtonColor else defaultButtonColor
        addPlayerSpawnButton.background = if (selectedType == ObjectType.PLAYER_SPAWN) selectedButtonColor else defaultButtonColor
    }

    private fun setupModeButton() {
        modeButton.apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                onModeSwitch()
            }
        }
    }

    private fun createWallStylePanel(): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color(70, 73, 75)),
                "Wall Style",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                Color.WHITE
            )

            val flatWallRadio = JRadioButton("Flat Walls").apply {
                isSelected = true
                background = Color(40, 44, 52)
                foreground = Color.WHITE
                alignmentX = Component.LEFT_ALIGNMENT
                addActionListener { onWallStyleChange?.invoke(false) }
            }

            val blockWallRadio = JRadioButton("Block Walls").apply {
                background = Color(40, 44, 52)
                foreground = Color.WHITE
                alignmentX = Component.LEFT_ALIGNMENT
                addActionListener { onWallStyleChange?.invoke(true) }
            }

            wallStyleGroup.add(flatWallRadio)
            wallStyleGroup.add(blockWallRadio)

            add(flatWallRadio)
            add(Box.createVerticalStrut(2))
            add(blockWallRadio)

            // visualization toggle checkbox
            val visualizationToggle = JCheckBox("Show Flat Walls as Lines").apply {
                background = Color(40, 44, 52)
                foreground = Color.WHITE
                alignmentX = Component.LEFT_ALIGNMENT
                addActionListener {
                    gridEditor.setFlatWallVisualization(isSelected)
                }
            }

            add(Box.createVerticalStrut(5))
            add(visualizationToggle)

            border = BorderFactory.createCompoundBorder(
                border,
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            )
        }
    }

    private fun setupMainPanel() {
        mainPanel.apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createEmptyBorder(5, 10, 5, 10)

            // Create buttons with stored references
            addWallButton = createButton("Add Wall").apply {
                addActionListener {
                    gridEditor.setObjectType(ObjectType.WALL)
                    updateButtonStates(ObjectType.WALL)
                    restoreFocusToGridEditor() // Add this line
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

            // Quick Actions section
            add(createSection("Quick Actions", listOf(
                addWallButton,
                addFloorButton,
                addPlayerSpawnButton,
                createButton("Clear All").apply {
                    addActionListener {
                        gridEditor.clearGrid()
                    }
                }
            )))

            add(Box.createVerticalStrut(10))

            add(createWallStylePanel())

            add(Box.createVerticalStrut(10))

            // Tools section
            selectButton = createButton("Select").apply {
                addActionListener {
                    if (background == Color(60, 63, 65)) {
                        // Activate selection mode
                        updateToolButtonStates(this)
                        gridEditor.setEditMode(GridEditor.EditMode.SELECT)
                    } else {
                        // Deactivate selection mode
                        background = Color(60, 63, 65)
                        gridEditor.setEditMode(GridEditor.EditMode.DRAW)
                    }
                }
            }

            moveButton = createButton("Move").apply {
                addActionListener {
                    if (background == Color(60, 63, 65)) {
                        // Activate move mode
                        updateToolButtonStates(this)
                        gridEditor.setEditMode(GridEditor.EditMode.MOVE)
                    } else {
                        // Deactivate move mode
                        background = Color(60, 63, 65)
                        gridEditor.setEditMode(GridEditor.EditMode.DRAW)
                    }
                }
            }

            rotateButton = createButton("Rotate").apply {
                addActionListener {
                    if (background == Color(60, 63, 65)) {
                        // Activate rotate mode
                        updateToolButtonStates(this)
                        gridEditor.setEditMode(GridEditor.EditMode.ROTATE)
                    } else {
                        // Deactivate rotate mode
                        background = Color(60, 63, 65)
                        gridEditor.setEditMode(GridEditor.EditMode.DRAW)
                    }
                }
            }

            add(createSection("Tools", listOf(
                selectButton,
                moveButton,
                rotateButton,
            )))
        }
    }

    private fun createSection(title: String, components: List<JComponent>): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color(70, 73, 75)),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                Color.WHITE
            )

            components.forEach { component ->
                add(component)
                add(Box.createVerticalStrut(5))
            }

            border = BorderFactory.createCompoundBorder(
                border,
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            )
        }
    }

    private fun createButton(text: String): JButton {
        return JButton(text).apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
        }
    }

    private fun updateToolButtonStates(activeButton: JButton) {
        val toolButtons = listOf(selectButton, moveButton, rotateButton)
        toolButtons.forEach { button ->
            if (button == activeButton) {
                button.background = Color(100, 100, 255)
            } else {
                button.background = Color(60, 63, 65)
            }
        }
    }

    private fun restoreFocusToGridEditor() {
        SwingUtilities.invokeLater {
            gridEditor.requestFocusInWindow()
        }
    }
}

/*
can you help me with my kotlin boomer shooter engine?
i have this editorpanel, and it works good, but it doesnt fit anymore so good, bc it has not really seperations for like walls, floors, player things, map things, images etc.
i watched a video where someone also coded an engine, but not for boomer shooters. there was this thing with the gui, i really liked. maybe you can see what i mean in the image i send you. its like 6 buttons in two rows and when you press on it, the items in it then get visible under the two buttons rows. so these rows keep visible always. can you add this to my engine, but keep the collapse thing so this then just get visible. only adding the button rows, and dont change any other feature
 */