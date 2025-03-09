import grideditor.GridEditor
import player.Player
import ui.EditorPanel
import ui.MenuSystem
import java.awt.*
import java.awt.event.*
import javax.swing.*

class Game3D : JPanel() {
    private val player = Player()
    private val renderer = Renderer(800, 600)
    private val renderPanel = RenderPanel()
    private val gridEditor = GridEditor()
    private val editorPanel = EditorPanel(gridEditor) { toggleEditorMode() }
    private lateinit var menuSystem: MenuSystem
    private val keysPressed = mutableSetOf<Int>()
    private val walls = mutableListOf<Wall>()
    private val floors = mutableListOf<Floor>()
    private var isEditorMode = true

    private var isDebugInfoVisible = true

    // Right panel with card layout to switch between grid editor and game view
    private val rightPanel = JPanel(CardLayout()).apply {
        add(gridEditor, "editor")
        add(renderPanel, "game")
    }

    // Main split pane
    private val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
        leftComponent = editorPanel
        rightComponent = rightPanel
        dividerSize = 5
        isContinuousLayout = true
        resizeWeight = 0.0  // Editor panel keeps its size when resizing window
    }

    init {
        layout = BorderLayout()

        // Add property change listener to GridEditor
        gridEditor.addPropertyChangeListener("gridChanged") {
            // Convert grid to walls
            val newWalls = gridEditor.generateWalls()
            updateWalls(newWalls)
            renderPanel.repaint()
        }

        // Set up wall style change listener
        editorPanel.setWallStyleChangeListener { useBlockWalls ->
            gridEditor.useBlockWalls = useBlockWalls
            // Regenerate walls with new style
            val newWalls = gridEditor.generateWalls()
            updateWalls(newWalls)
            renderPanel.repaint()
        }

        editorPanel.gridEditor = gridEditor

        // Setup render panel
        renderPanel.apply {
            preferredSize = Dimension(800, 600)
            background = Color(135, 206, 235)
            isFocusable = true
        }

        // Set initial size for editor panel
        editorPanel.preferredSize = Dimension(250, height)

        // Create menu bar with reference to GridEditor for save/load functionality
        menuSystem = MenuSystem(
            onFloorSelected = { level ->
                // Update grid editor with new floor level
                gridEditor.setCurrentFloor(level)

                // Update the floor height based on the level
                gridEditor.updateCurrentFloorHeight(level * gridEditor.floorHeight)

                // Update UI to show we're on the new floor
                menuSystem.setCurrentFloor(level)

                // Refresh the rendering
                renderPanel.repaint()
            },
            onFloorAdded = { isAbove ->
                // Get existing floors
                val currentFloors = getExistingFloors()
                val currentFloor = gridEditor.getCurrentFloor()

                // Initial calculation of new level based on current floor
                var newLevel = if (isAbove) {
                    currentFloor + 1
                } else {
                    currentFloor - 1
                }

                // Check if the floor already exists, and if so, find the next available level
                while (currentFloors.contains(newLevel)) {
                    // If we're adding above, move up until we find an unused floor number
                    if (isAbove) {
                        newLevel++
                    } else {
                        // If we're adding below, move down until we find an unused floor number
                        newLevel--
                    }
                }

                // Add new floor to menu system
                menuSystem.addFloor(newLevel)

                // Set the current floor to the new level
                gridEditor.setCurrentFloor(newLevel)

                // Update floor height in the grid editor
                gridEditor.updateCurrentFloorHeight(newLevel * gridEditor.floorHeight)

                // Update UI immediately
                menuSystem.setCurrentFloor(newLevel)

                // Notify of change
                renderPanel.repaint()
            },
            gridEditor = gridEditor  // Pass the GridEditor reference to MenuSystem
        )

        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                renderer.updateDimensions(renderPanel.width, renderPanel.height)
            }
        })

        add(menuSystem.createMenuBar(), BorderLayout.NORTH)

        // split pane to the main panel
        add(splitPane, BorderLayout.CENTER)

        // Set initial divider location
        SwingUtilities.invokeLater {
            splitPane.dividerLocation = 250
        }

        gridEditor.setCamera(player.camera)

        setupInputHandling()
        updateMode()
    }

    // Helper method to get existing floor levels from the grid
    private fun getExistingFloors(): Set<Int> {
        val floors = mutableSetOf<Int>()
        // Collect all unique floor levels from the grid cells
        gridEditor.grid.values.forEach { cell ->
            floors.addAll(cell.getOccupiedFloors())
        }
        // Always include floor 0 (ground floor)
        floors.add(0)
        return floors
    }

    private fun setupInputHandling() {
        // Remove the old key bindings
        val inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW)
        inputMap.clear()

        // Add a single global key listener to the main panel
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { e ->
            when (e.id) {
                KeyEvent.KEY_PRESSED -> {
                    if (e.keyCode == KeyEvent.VK_E) {
                        SwingUtilities.invokeLater { toggleEditorMode() }
                    }
                    if (!isEditorMode) {
                        keysPressed.add(e.keyCode)
                    }
                }
                KeyEvent.KEY_RELEASED -> {
                    if (!isEditorMode) {
                        keysPressed.remove(e.keyCode)
                    }
                }
            }
            false // Allow the event to be processed by other listeners
        }

        // Mouse handling for game mode
        renderPanel.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                if (!isEditorMode) {
                    handleMouseMovement(e)
                }
            }
        })
    }

    private fun handleMouseMovement(e: MouseEvent) {
        val dx = e.x - renderPanel.width/2
        val dy = e.y - renderPanel.height/2
        player.rotate(dx.toDouble(), dy.toDouble())

        try {
            val robot = Robot()
            robot.mouseMove(
                renderPanel.locationOnScreen.x + renderPanel.width/2,
                renderPanel.locationOnScreen.y + renderPanel.height/2
            )
        } catch (e: Exception) {
            // Handle potential security exceptions
        }
    }

    private fun toggleEditorMode() {
        isEditorMode = !isEditorMode
        updateMode()
    }

    private fun updateMode() {
        val cardLayout = rightPanel.layout as CardLayout
        if (isEditorMode) {
            cardLayout.show(rightPanel, "editor")
            editorPanel.setModeButtonText("Editor Mode")
            SwingUtilities.invokeLater {
                gridEditor.requestFocusInWindow()
            }
        } else {
            // Find the player spawn point and set the camera position
            gridEditor.grid.forEach { (pos, cell) ->
                cell.getObjectsForFloor(gridEditor.getCurrentFloor()).firstOrNull { it.type == ObjectType.PLAYER_SPAWN }?.let {
                    val (x, y) = pos
                    player.camera.position.x = -x * gridEditor.baseScale
                    player.camera.position.z = y * gridEditor.baseScale

                    // Calculate correct Y position based on current floor
                    val floorHeight = gridEditor.getCurrentFloor() * gridEditor.floorHeight
                    player.camera.position.y = 1.7 + floorHeight  // Player eye height + floor height
                }
            }

            cardLayout.show(rightPanel, "game")
            editorPanel.setModeButtonText("Game Mode")
            renderPanel.requestFocusInWindow()
        }
    }

    inner class RenderPanel : JPanel() {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // Sky Color
            g2.color = Color(135, 206, 235)
            g2.fillRect(0, 0, width, height)

            renderer.drawScene(g2, walls, floors, player.camera)

            if (!isEditorMode) {
                // Draw crosshair
                g2.color = Color.WHITE
                g2.drawLine(width/2 - 10, height/2, width/2 + 10, height/2)
                g2.drawLine(width/2, height/2 - 10, width/2, height/2 + 10)

                // Conditionally draw debug information
                if (isDebugInfoVisible) {
                    // Get cardinal direction from player
                    val direction = player.getCardinalDirection()

                    // Get angles in degrees for display
                    val yawDegrees = player.getYawDegrees()

                    // Draw debug information
                    g2.font = Font("Monospace", Font.BOLD, 14)
                    g2.color = Color.WHITE
                    g2.drawString("Direction: $direction (${yawDegrees}°)", 10, 20)
                    g2.drawString("Position: (${String.format("%.1f", player.position.x)}, ${String.format("%.1f", player.position.y)}, ${String.format("%.1f", player.position.z)})", 10, 40)
                }
            }
        }
    }

    // Method to toggle debug info visibility
    fun setDebugInfoVisible(visible: Boolean) {
        isDebugInfoVisible = visible
        renderPanel.repaint()
    }

    // Method to check current debug info visibility
    fun isDebugInfoVisible(): Boolean = isDebugInfoVisible

    fun update() {
        if (!isEditorMode) {
            var forward = 0.0
            var right = 0.0
            var up = 0.0

            // Get movement input
            if (KeyBindings.MOVE_FORWARD in keysPressed) forward += 1.0
            if (KeyBindings.MOVE_BACKWARD in keysPressed) forward -= 1.0
            if (KeyBindings.MOVE_LEFT in keysPressed) right -= 1.0
            if (KeyBindings.MOVE_RIGHT in keysPressed) right += 1.0
            if (KeyBindings.FLY_UP in keysPressed) up += 1.0
            if (KeyBindings.FLY_DOWN in keysPressed) up -= 1.0

            // Move player with collected input and pass floors list
            player.move(forward, right, up, walls, floors)

            gridEditor.repaint()  // Update the grid editor to show new player position
        }

        renderPanel.repaint()
    }

    private fun updateWalls(newWalls: List<Wall>) {
        walls.clear()
        walls.addAll(newWalls)

        floors.clear()
        floors.addAll(gridEditor.generateFloors())
    }
}