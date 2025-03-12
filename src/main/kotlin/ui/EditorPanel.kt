package ui

import ObjectType
import WallObject
import grideditor.GridEditor
import texturemanager.ResourceManager
import texturemanager.TextureManagerPanel
import ui.components.*
import java.awt.*
import javax.swing.*

class EditorPanel(var gridEditor: GridEditor, private val onModeSwitch: () -> Unit) : JPanel() {
    private val resourceManager = ResourceManager()
    private val textureManager = TextureManagerPanel(resourceManager)
    private val modeButton = JButton("Editor Mode")
    private val mainPanel = JPanel()
    private var onWallStyleChange: ((Boolean) -> Unit)? = null

    // Component panels
    private val wallPropertiesPanel = WallPropertiesPanel()
    private val wallStylePanel = WallStylePanel(gridEditor)
    private val toolsPanel: ToolsPanel
    private val quickActionsPanel: QuickActionsPanel

    // Tab buttons
    private val wallsTabButton = JButton("Walls")
    private val floorsTabButton = JButton("Floors")
    private val playerTabButton = JButton("Player")
    private val mapTabButton = JButton("Map")
    private val texturesTabButton = JButton("Textures")
    private val toolsTabButton = JButton("Tools")

    // Section containers for each tab
    private val wallsSectionsPanel = JPanel()
    private val floorsSectionsPanel = JPanel()
    private val playerSectionsPanel = JPanel()
    private val mapSectionsPanel = JPanel()
    private val texturesSectionsPanel = JPanel()
    private val toolsSectionsPanel = JPanel()

    // Main content panel that will hold the active tab content
    private val tabContentPanel = JPanel(CardLayout())

    // Currently active tab button
    private var activeTabButton: JButton? = null

    init {
        layout = BorderLayout()
        background = Color(40, 44, 52)

        // Initialize the components with each other
        gridEditor.initializeResourceManager(resourceManager)
        gridEditor.initializeTextureManagerPanel(textureManager)
        textureManager.gridEditor = gridEditor

        // Initialize component panels
        toolsPanel = ToolsPanel(gridEditor)
        quickActionsPanel = QuickActionsPanel(gridEditor)

        setupModeButton()
        setupMainPanel()
        setupSelectionHandling()
        setupWallPropertiesPanel()
        setupWallStylePanel()
        setupTabPanels()
        createTabSections()

        // Connect wall properties panel to grid editor
        wallPropertiesPanel.setGridEditor(gridEditor)

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

        // Mode button panel with fixed left alignment
        val modeButtonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            background = Color(40, 44, 52)
            add(modeButton)
            // Force the panel to use the preferred width of the button
            preferredSize = Dimension(modeButton.preferredSize.width, modeButton.preferredSize.height)
        }

        // Top panel with mode button and separator
        val topPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createEmptyBorder(10, 5, 5, 5)

            // Add the mode button panel instead of just the button
            add(modeButtonPanel)
            add(Box.createVerticalStrut(10))
            add(JSeparator())
            add(Box.createVerticalStrut(10))

            // Set maximum size to prevent stretching
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }

        // Tab buttons panel
        val tabButtonsPanel = createTabButtonsPanel()

        // Main container panel
        val mainContainerPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)

            add(topPanel)
            add(tabButtonsPanel)
            add(Box.createVerticalStrut(10))
            add(tabContentPanel)
        }

        // Add scroll pane
        val scrollPane = JScrollPane(mainContainerPanel).apply {
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

        // Activate walls tab by default
        activateTab(wallsTabButton)
    }

    private fun setupTabPanels() {
        // Setup panel properties for each tab section container
        val panels = listOf(
            wallsSectionsPanel, floorsSectionsPanel, playerSectionsPanel,
            mapSectionsPanel, texturesSectionsPanel, toolsSectionsPanel
        )

        panels.forEach { panel ->
            panel.apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                background = Color(40, 44, 52)
                border = BorderFactory.createEmptyBorder(0, 10, 10, 10)

                // Add vertical glue at the end to push everything to the top
                add(Box.createVerticalGlue())
            }
        }

        // Add panels to card layout
        tabContentPanel.apply {
            add(wallsSectionsPanel, "walls")
            add(floorsSectionsPanel, "floors")
            add(playerSectionsPanel, "player")
            add(mapSectionsPanel, "map")
            add(texturesSectionsPanel, "textures")
            add(toolsSectionsPanel, "tools")
        }
    }

    private fun createTabButtonsPanel(): JPanel {
        // Create a panel for the tab buttons with two rows
        val tabButtonsPanel = JPanel().apply {
            layout = GridLayout(2, 3, 5, 5)
            background = Color(40, 44, 52)
            border = BorderFactory.createEmptyBorder(0, 5, 0, 5)

            // First row
            add(wallsTabButton)
            add(floorsTabButton)
            add(playerTabButton)

            // Second row
            add(mapTabButton)
            add(texturesTabButton)
            add(toolsTabButton)
        }

        // Style the tab buttons
        val tabButtons = listOf(
            wallsTabButton, floorsTabButton, playerTabButton,
            mapTabButton, texturesTabButton, toolsTabButton
        )

        tabButtons.forEach { button ->
            button.apply {
                background = Color(60, 63, 65)
                foreground = Color.WHITE
                isFocusPainted = false
                addActionListener {
                    activateTab(this)
                }
            }
        }

        // Create a container to keep the tabs at a consistent width
        val tabButtonsContainer = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            add(tabButtonsPanel)

            // Set maximum size to prevent vertical stretching
            maximumSize = Dimension(Int.MAX_VALUE, tabButtonsPanel.preferredSize.height)
        }

        return tabButtonsContainer
    }

    private fun activateTab(tabButton: JButton) {
        // Reset all buttons
        val allButtons = listOf(
            wallsTabButton, floorsTabButton, playerTabButton,
            mapTabButton, texturesTabButton, toolsTabButton
        )

        allButtons.forEach { button ->
            button.background = Color(60, 63, 65)
            button.isEnabled = true
        }

        // Highlight selected button
        tabButton.background = Color(88, 91, 93)
        tabButton.isEnabled = false
        activeTabButton = tabButton

        // Show the selected tab content
        val cardLayout = tabContentPanel.layout as CardLayout
        when (tabButton) {
            wallsTabButton -> cardLayout.show(tabContentPanel, "walls")
            floorsTabButton -> cardLayout.show(tabContentPanel, "floors")
            playerTabButton -> cardLayout.show(tabContentPanel, "player")
            mapTabButton -> cardLayout.show(tabContentPanel, "map")
            texturesTabButton -> cardLayout.show(tabContentPanel, "textures")
            toolsTabButton -> cardLayout.show(tabContentPanel, "tools")
        }
    }

    private fun createTabSections() {
        // Walls tab sections
        val wallStyleSection = CollapsibleSection("Wall Style").apply {
            addComponent(wallStylePanel)
        }

        val wallPropertiesSection = CollapsibleSection("Wall Properties").apply {
            addComponent(wallPropertiesPanel)
        }

        wallsSectionsPanel.add(wallStyleSection)
        wallsSectionsPanel.add(Box.createVerticalStrut(10))
        wallsSectionsPanel.add(wallPropertiesSection)

        // Floors tab sections
        // You can move relevant floor-related sections here

        // Player tab sections
        // Add player-related sections here

        // Map tab sections
        val quickActionsSection = CollapsibleSection("Quick Actions").apply {
            addComponent(quickActionsPanel)
        }

        val displayOptionsPanel = DisplayOptionsPanel(gridEditor)
        val displayOptionsSection = CollapsibleSection("Display Options").apply {
            addComponent(displayOptionsPanel)
        }

        mapSectionsPanel.add(quickActionsSection)
        mapSectionsPanel.add(Box.createVerticalStrut(10))
        mapSectionsPanel.add(displayOptionsSection)

        // Textures tab sections
        val imageSection = CollapsibleSection("Textures").apply {
            addComponent(textureManager)
        }

        texturesSectionsPanel.add(imageSection)

        // Tools tab sections
        val toolsSection = CollapsibleSection("Tools").apply {
            addComponent(toolsPanel)
        }

        toolsSectionsPanel.add(toolsSection)
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

    private fun setupWallStylePanel() {
        // Connect the wall style change event
        wallStylePanel.setWallStyleChangeListener { isBlockWall ->
            onWallStyleChange?.invoke(isBlockWall)
        }
    }

    fun setModeButtonText(text: String) {
        modeButton.text = text
    }

    fun setWallStyleChangeListener(listener: (Boolean) -> Unit) {
        onWallStyleChange = listener
        wallStylePanel.setWallStyleChangeListener(listener)
    }

    private fun setupSelectionHandling() {
        gridEditor.setOnCellSelectedListener { cellData ->
            // Update the property buttons with selected cell's values
            cellData?.let { cell ->
                // Use the getter instead of directly accessing the property
                val currentFloorObjects = cell.getObjectsForFloor(gridEditor.getCurrentFloor())

                val wallObject = currentFloorObjects.filterIsInstance<WallObject>().firstOrNull()

                wallObject?.let {
                    // Update wall properties panel
                    wallPropertiesPanel.updateProperties(
                        color = it.color,
                        height = it.height,
                        width = it.width
                    )
                }
            }
        }
    }

    private fun setupModeButton() {
        modeButton.apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            // Explicitly set alignments to ensure it stays left
            horizontalAlignment = SwingConstants.LEFT
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = preferredSize  // Prevent stretching
            addActionListener {
                onModeSwitch()
            }
        }
    }

    private fun setupMainPanel() {
        mainPanel.apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
        }
    }
}