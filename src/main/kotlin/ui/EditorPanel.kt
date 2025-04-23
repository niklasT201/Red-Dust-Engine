package ui

import Direction
import FloorObject
import Renderer
import Game3D
import ObjectType
import PillarObject
import RampObject
import WallObject
import WaterObject
import grideditor.GridEditor
import texturemanager.ResourceManager
import texturemanager.TextureManagerPanel
import ui.components.*
import ui.topbar.FileManager
import java.awt.*
import javax.swing.*

class EditorPanel(var gridEditor: GridEditor, val renderer: Renderer, private val game3D: Game3D, resourceManager: ResourceManager, fileManager: FileManager, private val onModeSwitch: () -> Unit) : JPanel() {
    private val textureManager = TextureManagerPanel(resourceManager, fileManager)
    private val modeButton = JButton("Editor Mode")
    private val mainPanel = JPanel()
    private var onWallStyleChange: ((Boolean) -> Unit)? = null

    // Component panels
    private val objectSelectorPanel = ObjectSelectorPanel(gridEditor, renderer)
    private val wallPropertiesPanel = WallPropertiesPanel()
    private val wallStylePanel = WallStylePanel(gridEditor)
    private val toolsPanel: ToolsPanel
    private val quickActionsPanel: QuickActionsPanel
    private lateinit var displayOptionsPanel: DisplayOptionsPanel

    // Tab buttons
    private val objectsTabButton = JButton("Objects")
    private val playerTabButton = JButton("Player")
    private val mapTabButton = JButton("Map")
    private val texturesTabButton = JButton("Textures")
    private val toolsTabButton = JButton("Tools")
    private var openTabButton: JButton? = null  // Tracks currently open tab
    private val closedTabColor = Color(60, 63, 65) // Default tab color
    private val openTabColor = Color(88, 91, 93)  // Highlighted tab color
    private val cardLayout = CardLayout()  // Declare cardLayout as a property for easier access

    // Section containers for each tab
    private val objectsSectionsPanel = JPanel()
    private val playerSectionsPanel = JPanel()
    private val mapSectionsPanel = JPanel()
    private val texturesSectionsPanel = JPanel()
    private val toolsSectionsPanel = JPanel()
    private val emptyPanel = JPanel().apply {
        background = Color(40, 44, 52)
        layout = BorderLayout()
        val label = JLabel("Select a tab to view content", SwingConstants.CENTER)
        label.foreground = Color(150, 150, 150)
        add(label, BorderLayout.CENTER)
    }

    // Main content panel that will hold the active tab content
    private val tabContentPanel = JPanel(cardLayout)

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
        setupObjectSelectorPanel()
        createTabSections()

        // Connect wall properties panel to grid editor
        wallPropertiesPanel.setGridEditor(gridEditor)

        textureManager.setTextureSelectionListener(object : TextureManagerPanel.TextureSelectionListener {
            override fun onTextureSetAsDefault(entry: TextureManagerPanel.TextureEntry, objectType: ObjectType) {
                // Apply the texture to the grid editor based on type
                when (objectType) {
                    ObjectType.WALL -> {
                        gridEditor.setWallTexture(entry.imageEntry)
                        // gridEditor.currentWallTexture = entry.imageEntry // Direct setting is redundant if setter works
                        println("Listener: Applied wall texture '${entry.imageEntry.name}' to GridEditor")
                    }
                    ObjectType.FLOOR -> {
                        gridEditor.setFloorTexture(entry.imageEntry)
                        // gridEditor.currentFloorTexture = entry.imageEntry // Redundant
                        println("Listener: Applied floor texture '${entry.imageEntry.name}' to GridEditor")
                    }
                    ObjectType.PILLAR -> { // Added
                        gridEditor.setPillarTexture(entry.imageEntry)
                        println("Listener: Applied pillar texture '${entry.imageEntry.name}' to GridEditor")
                    }
                    ObjectType.WATER -> { // Added
                        gridEditor.setWaterTexture(entry.imageEntry)
                        println("Listener: Applied water texture '${entry.imageEntry.name}' to GridEditor")
                    }
                    ObjectType.RAMP -> { // Added
                        gridEditor.setRampTexture(entry.imageEntry)
                        println("Listener: Applied ramp texture '${entry.imageEntry.name}' to GridEditor")
                    }
                    // Handle other types
                    else -> {
                        println("Listener: Object type ${objectType.name} not handled for texture application")
                    }
                }
            }

            // Implement the new method
            override fun onTextureCleared(objectType: ObjectType) {
                when (objectType) {
                    ObjectType.WALL -> {
                        // Clear the wall texture and revert to using color
                        gridEditor.clearWallTexture()
                        println("Listener: Cleared wall texture, reverting to color")
                    }
                    ObjectType.FLOOR -> {
                        // Clear the floor texture and revert to using color
                        gridEditor.clearFloorTexture()
                        println("Listener: Cleared floor texture, reverting to color")
                    }
                    ObjectType.PILLAR -> { // Added
                        gridEditor.clearPillarTexture() // Needs to be added to GridEditor
                        println("Listener: Cleared pillar texture, reverting to color")
                    }
                    ObjectType.WATER -> {
                        gridEditor.clearWaterTexture() // Already existed
                        println("Listener: Cleared water texture, reverting to color")
                    }
                    ObjectType.RAMP -> { // Added
                        gridEditor.clearRampTexture() // Needs to be added to GridEditor
                        println("Listener: Cleared ramp texture, reverting to color")
                    }
                    else -> {
                        println("Listener: Object type ${objectType.name} not handled for texture clearing")
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
        cardLayout.show(tabContentPanel, "empty")
    }

    private fun setupObjectSelectorPanel() {
        // Connect the object selector to wall property changes
        objectSelectorPanel.setWallPropertyChangeListener(object : WallPropertiesPanel.WallPropertyChangeListener {
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

        // Connect floor property changes
        objectSelectorPanel.setFloorPropertyChangeListener(object : FloorPropertiesPanel.FloorPropertyChangeListener {
            override fun onFloorColorChanged(color: Color) {
                gridEditor.setFloorColor(color)  // Assuming this method exists
            }

            override fun onFloorHeightChanged(height: Double) {
                gridEditor.updateCurrentFloorHeight(height)
            }
        })

        // Connect pillar property changes
        objectSelectorPanel.setPillarPropertyChangeListener(object : PillarPropertiesPanel.PillarPropertyChangeListener {
            override fun onPillarColorChanged(color: Color) {
                gridEditor.setPillarColor(color)
            }

            override fun onPillarHeightChanged(height: Double) {
                gridEditor.setPillarHeight(height)
            }

            override fun onPillarWidthChanged(width: Double) {
                gridEditor.setPillarWidth(width)
            }
        })

        objectSelectorPanel.setWaterPropertyChangeListener(object : WaterPropertiesPanel.WaterPropertyChangeListener {
            override fun onWaterColorChanged(color: Color) {
                gridEditor.setWaterColor(color)
            }

            override fun onFloorHeightChanged(height: Double) {
                gridEditor.updateCurrentFloorHeight(height)
            }

            override fun onDepthChanged(depth: Double) {
                gridEditor.setWaterDepth(depth)
            }

            override fun onWaveHeightChanged(height: Double) {
                gridEditor.setWaterWaveHeight(height)
            }

            override fun onWaveSpeedChanged(speed: Double) {
                gridEditor.setWaterWaveSpeed(speed)
            }

            override fun onDamagePerSecondChanged(damage: Double) {
                gridEditor.setWaterDamagePerSecond(damage)
            }
        })

        // Connect ramp property changes
        objectSelectorPanel.setRampPropertyChangeListener(object : RampPropertiesPanel.RampPropertyChangeListener {
            override fun onRampColorChanged(color: Color) {
                gridEditor.setRampColor(color)
            }

            override fun onRampHeightChanged(height: Double) {
                gridEditor.setRampHeight(height)
            }

            override fun onRampWidthChanged(width: Double) {
                gridEditor.setRampWidth(width)
            }

            override fun onRampDirectionChanged(direction: Direction) {
                gridEditor.setRampDirection(direction)  // Assuming this method exists
            }
        })

        // Connect the wall style change event
        objectSelectorPanel.setWallStyleChangeListener { isBlockWall ->
            // This updates the GridEditor's visualization setting
            gridEditor.setFlatWallVisualization(!isBlockWall)

            // And also forwards to any external listeners
            onWallStyleChange?.invoke(isBlockWall)
        }
    }

    private fun setupTabPanels() {
        // Setup panel properties for each tab section container
        val panels = listOf(
            objectsSectionsPanel, playerSectionsPanel,
            mapSectionsPanel, texturesSectionsPanel, toolsSectionsPanel
        )

        panels.forEach { panel ->
            panel.apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                background = Color(40, 44, 52)
                border = BorderFactory.createEmptyBorder(0, 10, 10, 10)
            }
        }

        // Add panels to card layout
        tabContentPanel.apply {
            add(objectsSectionsPanel, "objects")
            add(playerSectionsPanel, "player")
            add(mapSectionsPanel, "map")
            add(texturesSectionsPanel, "textures")
            add(toolsSectionsPanel, "tools")
            add(emptyPanel, "empty")
        }
    }


    private fun createTabButtonsPanel(): JPanel {
        // Create a panel for the tab buttons with two rows
        val tabButtonsPanel = JPanel().apply {
            layout = GridLayout(2, 3, 3, 3) // Reduce the gaps between buttons
            background = Color(40, 44, 52)
            border = BorderFactory.createEmptyBorder(0, 5, 0, 5)

            // First row
            add(objectsTabButton)
            add(playerTabButton)
            add(mapTabButton)

            // Second row
            add(texturesTabButton)
            add(toolsTabButton)
            add(JPanel().apply { background = Color(40, 44, 52) }) // Empty panel for balance
        }

        // Style the tab buttons
        val tabButtons = listOf(
            objectsTabButton, playerTabButton, mapTabButton,
            texturesTabButton, toolsTabButton
        )

        tabButtons.forEach { button ->
            button.apply {
                background = closedTabColor
                foreground = Color.WHITE
                isFocusPainted = false
                // Make the buttons smaller
                font = Font(font.name, font.style, 12) // Smaller font
                margin = Insets(2, 4, 2, 4) // Smaller internal margins
                addActionListener {
                    toggleTab(this)
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

    private fun toggleTab(tabButton: JButton) {
        val tabName = when (tabButton) {
            objectsTabButton -> "objects"
            playerTabButton -> "player"
            mapTabButton -> "map"
            texturesTabButton -> "textures"
            toolsTabButton -> "tools"
            else -> "empty"
        }

        // Reset all buttons to closed appearance
        val allButtons = listOf(
            objectsTabButton, playerTabButton, mapTabButton,
            texturesTabButton, toolsTabButton
        )

        allButtons.forEach { button ->
            button.background = closedTabColor
            button.isEnabled = true
        }

        // Toggle tab state
        if (openTabButton == tabButton) {
            // If clicked tab is already open, close it
            openTabButton = null
            cardLayout.show(tabContentPanel, "empty")
        } else {
            // Otherwise, open this tab and close any previously open tab
            tabButton.background = openTabColor
            openTabButton = tabButton
            cardLayout.show(tabContentPanel, tabName)
        }
    }

    private fun createTabSections() {
        // Objects tab sections
        objectsSectionsPanel.add(objectSelectorPanel)
        objectsSectionsPanel.add(Box.createVerticalGlue())

        // Map tab sections
        val quickActionsSection = CollapsibleSection("Quick Actions").apply {
            addComponent(quickActionsPanel)
        }

        val skyOptionsPanel = SkyOptionsPanel(game3D)
        val skyOptionsSection = CollapsibleSection("Sky Settings").apply {
            addComponent(skyOptionsPanel)
        }

        val renderOptionsPanel = RenderOptionsPanel(renderer)
        val renderOptionsSection = CollapsibleSection("Render Options").apply {
            addComponent(renderOptionsPanel)
        }

        // Create grid labels panel with error handling
        this.displayOptionsPanel = try {
            DisplayOptionsPanel(gridEditor)
        } catch (e: Exception) {
            println("Error creating DisplayOptionsPanel: ${e.message}")
            e.printStackTrace()
            // Create a simple fallback panel
            JPanel().apply {
                background = Color(40, 44, 52)
                layout = BorderLayout()
                add(JLabel("Display options unavailable", SwingConstants.CENTER).apply {
                    foreground = Color.WHITE
                }, BorderLayout.CENTER)
            } as DisplayOptionsPanel
        }

        // Player tab sections
        val playerOptionsPanel = PlayerOptionsPanel(game3D)
        val playerOptionsSection = CollapsibleSection("Crosshair Settings").apply {
            addComponent(playerOptionsPanel)
        }
        val debugOptionsPanel = DebugOptionsPanel(game3D)
        val debugOptionsSection = CollapsibleSection("Player Debug Info").apply {
            addComponent(debugOptionsPanel)
        }

        val playerSettingsPanel = PlayerSettingsPanel(game3D.player, game3D)
        val playerSettingsSection = CollapsibleSection("Player Settings").apply {
            addComponent(playerSettingsPanel)
        }

        val mouseControlPanel = MouseControlSettingsPanel(game3D.player.camera, game3D.renderer)
        val mouseSettingsSection = CollapsibleSection("Mouse Controls").apply {
            addComponent(mouseControlPanel)
        }

        playerSectionsPanel.add(debugOptionsSection)
        playerSectionsPanel.add(Box.createVerticalStrut(10))
        playerSectionsPanel.add(playerOptionsSection)
        playerSectionsPanel.add(Box.createVerticalStrut(10))
        playerSectionsPanel.add(playerSettingsSection)
        playerSectionsPanel.add(Box.createVerticalStrut(10))
        playerSectionsPanel.add(mouseSettingsSection)
        playerSectionsPanel.add(Box.createVerticalGlue())

        mapSectionsPanel.add(renderOptionsSection)
        mapSectionsPanel.add(Box.createVerticalStrut(10))
        mapSectionsPanel.add(skyOptionsSection)
        mapSectionsPanel.add(Box.createVerticalStrut(10))
        mapSectionsPanel.add(quickActionsSection)
        mapSectionsPanel.add(Box.createVerticalGlue())

        // Textures tab sections
        val imageSection = CollapsibleSection("Textures").apply {
            addComponent(textureManager)
        }

        texturesSectionsPanel.add(imageSection)
        texturesSectionsPanel.add(Box.createVerticalGlue())

        // Tools tab sections
        val toolsSection = CollapsibleSection("Tools").apply {
            addComponent(toolsPanel)
        }

        val gridLabelsSection = CollapsibleSection("Grid Labels").apply {
            addComponent(this@EditorPanel.displayOptionsPanel)
        }

        toolsSectionsPanel.add(toolsSection)
        toolsSectionsPanel.add(Box.createVerticalStrut(10))
        // Vertical glue AFTER adding all sections
        toolsSectionsPanel.add(gridLabelsSection)
        toolsSectionsPanel.add(Box.createVerticalGlue())
    }

    fun getDisplayOptionsPanel(): DisplayOptionsPanel {
        // Consider adding a check if it's initialized if you switch from lateinit
        return displayOptionsPanel
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
            // Update the property panels with selected cell's values
            cellData?.let { cell ->
                // Use the getter instead of directly accessing the property
                val currentFloorObjects = cell.getObjectsForFloor(gridEditor.useCurrentFloor())

                // Handle WallObject
                val wallObject = currentFloorObjects.filterIsInstance<WallObject>().firstOrNull()

                wallObject?.let {
                    // Update object selector panel instead of wall properties panel
                    objectSelectorPanel.updateWallProperties(
                        color = it.color,
                        height = it.height,
                        width = it.width
                    )
                }

                // Handle FloorObject
                val floorObject = currentFloorObjects.filterIsInstance<FloorObject>().firstOrNull()
                floorObject?.let {
                    objectSelectorPanel.updateFloorProperties(
                        color = it.color,
                        height = it.height
                    )
                }

                // Handle PillarObject
                val pillarObject = currentFloorObjects.filterIsInstance<PillarObject>().firstOrNull()
                pillarObject?.let {
                    objectSelectorPanel.updatePillarProperties(
                        color = it.color,
                        height = it.height,
                        width = it.width
                    )
                }

                val waterObject = currentFloorObjects.filterIsInstance<WaterObject>().firstOrNull()
                waterObject?.let {
                    objectSelectorPanel.updateWaterProperties(
                        color = it.color, // Assuming WaterObject has these properties
                        depth = it.depth,
                        waveHeight = it.waveHeight,
                        waveSpeed = it.waveSpeed,
                        damagePerSecond = it.damagePerSecond
                    )
                }

                // Handle RampObject
                val rampObject = currentFloorObjects.filterIsInstance<RampObject>().firstOrNull()
                rampObject?.let {
                    objectSelectorPanel.updateRampProperties(
                        color = it.color,
                        height = it.height,
                        width = it.width,
                        direction = it.direction
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