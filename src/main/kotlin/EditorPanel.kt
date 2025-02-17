import java.awt.*
import javax.swing.*
import javax.swing.border.TitledBorder

class EditorPanel(private val onModeSwitch: () -> Unit) : JPanel() {
    var gridEditor = GridEditor()
    private val modeButton = JButton("Editor Mode")
    private val mainPanel = JPanel()
    private val wallStyleGroup = ButtonGroup()
    private var onWallStyleChange: ((Boolean) -> Unit)? = null
    private var currentWallColor = Color(150, 0, 0)  // Default wall color
    private var onColorChange: ((Color) -> Unit)? = null
    private var currentWallHeight = 3.0
    private var currentWallWidth = 2.0

    // Store references to object type buttons
    private lateinit var addWallButton: JButton
    private lateinit var addFloorButton: JButton

    // Colors for button states
    private val defaultButtonColor = Color(60, 63, 65)
    private val selectedButtonColor = Color(100, 100, 255)

    // Reference to wall property buttons for updating
    private var colorButton: JButton
    private var heightButton: JButton
    private var widthButton: JButton
    private var selectButton: JButton
    private var moveButton: JButton
    private var rotateButton: JButton

    init {
        layout = BorderLayout()
        background = Color(40, 44, 52)

        setupModeButton()
        setupMainPanel()
        setupSelectionHandling()

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

        // Create collapsible sections
        val quickActionsSection = CollapsibleSection("Quick Actions").apply {
            addComponent(addWallButton)
            addComponent(addFloorButton)
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

        val wallPropertiesSection = CollapsibleSection("Wall Properties").apply {
            val (colorBtn, heightBtn, widthBtn) = createWallPropertiesButtons()
            colorButton = colorBtn
            heightButton = heightBtn
            widthButton = widthBtn

            addComponent(colorBtn)
            addComponent(heightBtn)
            addComponent(widthBtn)
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

        // Add sections to the panel
        sectionsPanel.add(topPanel)
        sectionsPanel.add(quickActionsSection)
        sectionsPanel.add(Box.createVerticalStrut(10))  // Increased spacing between sections
        sectionsPanel.add(wallStyleSection)
        sectionsPanel.add(Box.createVerticalStrut(10))
        sectionsPanel.add(wallPropertiesSection)
        sectionsPanel.add(Box.createVerticalStrut(10))
        sectionsPanel.add(toolsSection)

        // Add rigid area at the bottom to prevent stretching
        sectionsPanel.add(Box.createVerticalGlue())

        // Wrap sectionsPanel in a panel that handles alignment
        val wrapperPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createEmptyBorder(0, 10, 10, 10)

            add(sectionsPanel)
            add(Box.createVerticalGlue())  // Push everything to the top
        }

        // Add scroll pane
        val scrollPane = JScrollPane(wrapperPanel).apply {
            border = null
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }

        add(scrollPane, BorderLayout.CENTER)
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

    private fun createWallPropertiesButtons(): Triple<JButton, JButton, JButton> {
        val colorBtn = createColorButton("Wall Color", currentWallColor)
        val heightBtn = createHeightButton()
        val widthBtn = createWidthButton()
        return Triple(colorBtn, heightBtn, widthBtn)
    }

    private fun createTopPanel(): JPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)
        border = BorderFactory.createEmptyBorder(15, 5, 2, 5)

        add(modeButton)
        add(Box.createVerticalStrut(15))

        // Create a panel for the separator with margins
        val separatorPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)

            // Add left margin
            add(Box.createHorizontalStrut(5))

            // Add separator with fixed width
            add(JSeparator().apply {
                maximumSize = Dimension(Short.MAX_VALUE.toInt(), 1)
                alignmentX = Component.LEFT_ALIGNMENT
            })

            // Add right margin
            add(Box.createHorizontalStrut(5))
        }

        add(separatorPanel)
        add(Box.createVerticalStrut(2))
    }

    fun setModeButtonText(text: String) {
        modeButton.text = text
    }

    fun setWallStyleChangeListener(listener: (Boolean) -> Unit) {
        onWallStyleChange = listener
    }

    fun setColorChangeListener(listener: (Color) -> Unit) {
        onColorChange = listener
    }

    private fun setupSelectionHandling() {
        gridEditor.setOnCellSelectedListener { cellData ->
            // Update the property buttons with selected cell's values
            cellData?.let { cell ->
                // Find the first wall object in the cell
                val wallObject = cell.objects.filterIsInstance<WallObject>().firstOrNull()

                // Update UI only if a wall object is found
                wallObject?.let {
                    colorButton.background = it.color
                    heightButton.text = "Wall Height: ${it.height}"
                    widthButton.text = "Wall Width: ${it.width}"
                    currentWallColor = it.color
                    currentWallHeight = it.height
                    currentWallWidth = it.width
                }
            }
        }
    }

    private fun updateButtonStates(selectedType: ObjectType) {
        addWallButton.background = if (selectedType == ObjectType.WALL) selectedButtonColor else defaultButtonColor
        addFloorButton.background = if (selectedType == ObjectType.FLOOR) selectedButtonColor else defaultButtonColor
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
                }
            }

            addFloorButton = createButton("Add Floor").apply {
                addActionListener {
                    gridEditor.setObjectType(ObjectType.FLOOR)
                    updateButtonStates(ObjectType.FLOOR)
                }
            }

            // Quick Actions section
            add(createSection("Quick Actions", listOf(
                addWallButton,
                addFloorButton,
                createButton("Clear All").apply {
                    addActionListener {
                        gridEditor.clearGrid()
                    }
                }
            )))

            add(Box.createVerticalStrut(10))

            add(createWallStylePanel())

            add(Box.createVerticalStrut(10))

            // Updated Wall Properties section with stored references
            val (propPanel, buttons) = createWallPropertiesPanel()
            add(propPanel)
            colorButton = buttons.first
            heightButton = buttons.second
            widthButton = buttons.third

            add(Box.createVerticalStrut(10))

            // Updated Tools section with Select button
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

    fun updateToolButtonStates(activeButton: JButton) {
        val toolButtons = listOf(selectButton, moveButton, rotateButton)
        toolButtons.forEach { button ->
            if (button == activeButton) {
                button.background = Color(100, 100, 255)
            } else {
                button.background = Color(60, 63, 65)
            }
        }
    }

    private fun createWallPropertiesPanel(): Pair<JPanel, Triple<JButton, JButton, JButton>> {
        val colorBtn = createColorButton("Wall Color", currentWallColor)
        val heightBtn = createHeightButton()
        val widthBtn = createWidthButton()

        val panel = createSection("Wall Properties", listOf(
            colorBtn,
            heightBtn,
            widthBtn
        ))

        return Pair(panel, Triple(colorBtn, heightBtn, widthBtn))
    }


    private fun createColorButton(text: String, initialColor: Color): JButton {
        return JButton(text).apply {
            background = initialColor
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                val newColor = JColorChooser.showDialog(
                    this,
                    "Choose Wall Color",
                    background
                )
                if (newColor != null) {
                    background = newColor
                    currentWallColor = newColor
                    onColorChange?.invoke(newColor)
                    // Update selected cell if in select mode
                    gridEditor.updateSelectedCell(color = newColor)
                }
            }
        }
    }

    private fun createHeightButton(): JButton {
        return JButton("Wall Height: $currentWallHeight").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                val input = JOptionPane.showInputDialog(
                    this,
                    "Enter wall height (0.5 - 10.0):",
                    currentWallHeight
                )
                try {
                    val newHeight = input?.toDoubleOrNull()
                    if (newHeight != null && newHeight in 0.5..10.0) {
                        currentWallHeight = newHeight
                        text = "Wall Height: $currentWallHeight"
                        gridEditor.setWallHeight(currentWallHeight)
                        // Update selected cell if in select mode
                        gridEditor.updateSelectedCell(height = newHeight)
                    }
                } catch (e: NumberFormatException) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Please enter a valid number between 0.5 and 10.0",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }

    private fun createWidthButton(): JButton {
        return JButton("Wall Width: $currentWallWidth").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                val input = JOptionPane.showInputDialog(
                    this,
                    "Enter wall width (0.5 - 5.0):",
                    currentWallWidth
                )
                try {
                    val newWidth = input?.toDoubleOrNull()
                    if (newWidth != null && newWidth in 0.5..5.0) {
                        currentWallWidth = newWidth
                        text = "Wall Width: $currentWallWidth"
                        gridEditor.setWallWidth(currentWallWidth)
                        // Update selected cell if in select mode
                        gridEditor.updateSelectedCell(width = newWidth)
                    }
                } catch (e: NumberFormatException) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Please enter a valid number between 0.5 and 5.0",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }
}