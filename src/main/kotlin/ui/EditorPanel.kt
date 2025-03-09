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

        // Connect wall properties panel to grid editor
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
            addComponent(quickActionsPanel)
        }

        val wallStyleSection = CollapsibleSection("Wall Style").apply {
            addComponent(wallStylePanel)
        }

        // Wall properties section
        val wallPropertiesSection = CollapsibleSection("Wall Properties").apply {
            addComponent(wallPropertiesPanel)
        }

        // Tools section
        val toolsSection = CollapsibleSection("Tools").apply {
            addComponent(toolsPanel)
        }

        val imageSection = CollapsibleSection("Textures").apply {
            addComponent(textureManager)
        }

        val displayOptionsPanel = DisplayOptionsPanel(gridEditor)
        val displayOptionsSection = CollapsibleSection("Display Options").apply {
            addComponent(displayOptionsPanel)
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
        sectionsPanel.add(displayOptionsSection)
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
            alignmentX = Component.LEFT_ALIGNMENT
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

/*
can you help me with my kotlin boomer shooter engine?
i have this editorpanel, and it works good, but it doesnt fit anymore so good, bc it has not really seperations for like walls, floors, player things, map things, images etc.
i watched a video where someone also coded an engine, but not for boomer shooters. there was this thing with the gui, i really liked. maybe you can see what i mean in the image i send you. its like 6 buttons in two rows and when you press on it, the items in it then get visible under the two buttons rows. so these rows keep visible always. can you add this to my engine, but keep the collapse thing so this then just get visible. only adding the button rows, and dont change any other feature
 */