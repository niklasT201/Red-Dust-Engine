import controls.KeyBindings
import grideditor.GridEditor
import player.Player
import ui.EditorPanel
import ui.MenuSystem
import ui.components.CrosshairShape
import ui.components.SkyRenderer
import java.awt.*
import java.awt.event.*
import javax.swing.*

class Game3D : JPanel() {
    private val player = Player()
    private val renderer = Renderer(800, 600)
    private val renderPanel = RenderPanel()
    private val gridEditor = GridEditor()
    private val editorPanel = EditorPanel(gridEditor,renderer, this) { toggleEditorMode() }
    //private val settingsSaver = saving.SettingsSaver(gridEditor)
    private lateinit var menuSystem: MenuSystem
    private var skyColor = Color(135, 206, 235)
    private var skyRenderer: SkyRenderer = SkyRenderer(skyColor)
    private val keysPressed = mutableSetOf<Int>()
    private val walls = mutableListOf<Wall>()
    private val floors = mutableListOf<Floor>()
    private var isEditorMode = true

    private var frameCount = 0
    private var lastFpsUpdateTime = System.currentTimeMillis()
    private var currentFps = 0
    private var isFpsCounterVisible = true
    private var isDirectionVisible = true
    private var isPositionVisible = true

    private var isCrosshairVisible = true
    private var crosshairSize = 10
    private var crosshairColor = Color.WHITE
    private var crosshairShape = CrosshairShape.PLUS

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
            gridEditor = gridEditor,  // Pass the GridEditor reference to MenuSystem
            renderer = renderer,
            game3D = this,
            player = player
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
        addPredefinedPillars()

        //SwingUtilities.invokeLater { settingsSaver.loadPlayerSettings(player, this) }
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
                    if (e.keyCode == KeyBindings.TOGGLE_EDITOR) {
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
                cell.getObjectsForFloor(gridEditor.getCurrentFloor()).filterIsInstance<PlayerSpawnObject>().firstOrNull()?.let { playerSpawn ->
                    val (x, y) = pos

                    // Use the stored offsets to calculate the precise position
                    val worldX = -(x + playerSpawn.offsetX) * gridEditor.baseScale
                    val worldZ = (y + playerSpawn.offsetY) * gridEditor.baseScale

                    // Set player position with precise offset
                    player.camera.position.x = worldX
                    player.camera.position.z = worldZ

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

            // Use the sky color property instead of hardcoded value
            skyRenderer.render(g2, width, height)

            renderer.drawScene(g2, walls, floors, player.camera)

            if (!isEditorMode) {
                // Draw crosshair only if it's visible
                if (isCrosshairVisible) {
                    g2.color = crosshairColor

                    when (crosshairShape) {
                        CrosshairShape.PLUS -> {
                            g2.drawLine(width/2 - crosshairSize, height/2, width/2 + crosshairSize, height/2)
                            g2.drawLine(width/2, height/2 - crosshairSize, width/2, height/2 + crosshairSize)
                        }
                        CrosshairShape.X -> {
                            g2.drawLine(width/2 - crosshairSize, height/2 - crosshairSize, width/2 + crosshairSize, height/2 + crosshairSize)
                            g2.drawLine(width/2 - crosshairSize, height/2 + crosshairSize, width/2 + crosshairSize, height/2 - crosshairSize)
                        }
                        CrosshairShape.DOT -> {
                            val dotSize = crosshairSize / 3
                            g2.fillOval(width/2 - dotSize, height/2 - dotSize, dotSize * 2, dotSize * 2)
                        }
                        CrosshairShape.CIRCLE -> {
                            g2.drawOval(width/2 - crosshairSize, height/2 - crosshairSize, crosshairSize * 2, crosshairSize * 2)
                        }
                    }
                }

                g2.font = Font("Monospace", Font.BOLD, 14)
                g2.color = Color.WHITE

                // Draw FPS counter independently of other debug info
                if (isFpsCounterVisible) {
                    g2.drawString("FPS: $currentFps", 10, 20)
                }

                // Conditionally draw debug information
                // Get cardinal direction from player
                val direction = player.getCardinalDirection()

                // Get angles in degrees for display
                val yawDegrees = player.getYawDegrees()

                // Draw debug information
                // Adjust y-position based on whether FPS is also being shown
                val startY = if (isFpsCounterVisible) 40 else 20
                var currentY = startY
                if (isDirectionVisible) {
                    g2.drawString("Direction: $direction (${yawDegrees}°)", 10, currentY)
                    currentY += 20
                }
                if (isPositionVisible) {
                    g2.drawString("Position: (${String.format("%.1f", player.position.x)}, ${String.format("%.1f", player.position.y)}, ${String.format("%.1f", player.position.z)})", 10, currentY)
                }
            }
        }
    }

    fun isCrosshairVisible(): Boolean = isCrosshairVisible

    fun setCrosshairVisible(visible: Boolean) {
        isCrosshairVisible = visible
        renderPanel.repaint()  // Refresh the display when changed
    }

    fun getCrosshairSize(): Int = crosshairSize

    fun setCrosshairSize(size: Int) {
        crosshairSize = size
        renderPanel.repaint()  // Refresh the display when changed
    }

    fun getCrosshairColor(): Color = crosshairColor?: Color.WHITE

    fun setCrosshairColor(color: Color) {
        crosshairColor = color
        renderPanel.repaint()  // Refresh the display when changed
    }

    fun getCrosshairShape(): CrosshairShape = crosshairShape

    fun setCrosshairShape(shape: CrosshairShape) {
        crosshairShape = shape
        renderPanel.repaint()  // Refresh the display when changed
    }

    fun isDirectionVisible(): Boolean = isDirectionVisible
    fun setDirectionVisible(visible: Boolean) {
        isDirectionVisible = visible
        renderPanel.repaint()
    }

    fun isPositionVisible(): Boolean = isPositionVisible
    fun setPositionVisible(visible: Boolean) {
        isPositionVisible = visible
        renderPanel.repaint()
    }

    fun getSkyRenderer(): SkyRenderer = skyRenderer

    fun setSkyRenderer(renderer: SkyRenderer) {
        skyRenderer = renderer
        skyColor = renderer.skyColor  // Keep skyColor in sync with renderer
        renderPanel.repaint()
    }
    fun getSkyColor(): Color = skyColor

    // Setter for skyColor
    fun setSkyColor(color: Color) {
        skyColor = color
        renderPanel.background = skyColor
        renderPanel.repaint()
    }

    private fun calculateFps() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastFpsUpdateTime

        // Update FPS calculation every 500ms
        if (elapsedTime >= 500) {
            currentFps = (frameCount / (elapsedTime / 1000.0)).toInt()
            frameCount = 0
            lastFpsUpdateTime = currentTime
        }
    }

    // Add these getters and setters for FPS counter visibility
    fun setFpsCounterVisible(visible: Boolean) {
        isFpsCounterVisible = visible
        renderPanel.repaint()
    }

    fun isFpsCounterVisible(): Boolean = isFpsCounterVisible

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

        // Calculate FPS for every frame
        calculateFps()

        renderPanel.repaint()
    }

    private fun updateWalls(newWalls: List<Wall>) {
        walls.clear()
        walls.addAll(newWalls)

        floors.clear()
        floors.addAll(gridEditor.generateFloors())
    }

    private fun addPredefinedPillars() {
        // Define some predefined pillars at specific locations
        val predefinedPillars = listOf(
            // Simple square pillar at position (-5, 0, -5)
            createPillar(
                position = Vec3(-5.0, 0.0, -5.0),
                height = 4.0,
                width = 1.0,
                color = Color(180, 170, 150)
            ),

            // Taller ornate pillar at position (5, 0, -5)
            createPillar(
                position = Vec3(5.0, 0.0, -5.0),
                height = 6.0,
                width = 1.2,
                color = Color(190, 180, 160)
            ),

            // Shorter wide pillar at position (-5, 0, 5)
            createPillar(
                position = Vec3(-5.0, 0.0, 5.0),
                height = 3.0,
                width = 1.5,
                color = Color(170, 160, 140)
            ),

            // Standard pillar at position (5, 0, 5)
            createPillar(
                position = Vec3(5.0, 0.0, 5.0),
                height = 4.5,
                width = 1.0,
                color = Color(185, 175, 155)
            ),

            // Center pillar
            createPillar(
                position = Vec3(0.0, 0.0, 0.0),
                height = 5.0,
                width = 1.3,
                color = Color(195, 185, 165)
            )
        )

        // Add all the pillar walls to our game's walls list
        walls.addAll(predefinedPillars.flatten())
    }

    // Helper function to create a single pillar's walls
    private fun createPillar(position: Vec3, height: Double, width: Double, color: Color): List<Wall> {
        val halfWidth = width / 2
        val x = position.x
        val y = position.y
        val z = position.z

        // Create a more complex pillar with base, shaft and top
        val pillarWalls = mutableListOf<Wall>()

        // Base of the pillar (slightly wider)
        val baseHeight = 0.5
        val baseWidth = width * 1.2
        val baseHalfWidth = baseWidth / 2

        // Base walls
        pillarWalls.add(Wall(Vec3(x - baseHalfWidth, y, z - baseHalfWidth), Vec3(x + baseHalfWidth, y, z - baseHalfWidth), baseHeight, color))
        pillarWalls.add(Wall(Vec3(x + baseHalfWidth, y, z - baseHalfWidth), Vec3(x + baseHalfWidth, y, z + baseHalfWidth), baseHeight, color))
        pillarWalls.add(Wall(Vec3(x + baseHalfWidth, y, z + baseHalfWidth), Vec3(x - baseHalfWidth, y, z + baseHalfWidth), baseHeight, color))
        pillarWalls.add(Wall(Vec3(x - baseHalfWidth, y, z + baseHalfWidth), Vec3(x - baseHalfWidth, y, z - baseHalfWidth), baseHeight, color))

        // Main shaft of the pillar
        val shaftHeight = height - 1.0 // Reserve space for base and top
        pillarWalls.add(Wall(Vec3(x - halfWidth, y + baseHeight, z - halfWidth), Vec3(x + halfWidth, y + baseHeight, z - halfWidth), shaftHeight, color))
        pillarWalls.add(Wall(Vec3(x + halfWidth, y + baseHeight, z - halfWidth), Vec3(x + halfWidth, y + baseHeight, z + halfWidth), shaftHeight, color))
        pillarWalls.add(Wall(Vec3(x + halfWidth, y + baseHeight, z + halfWidth), Vec3(x - halfWidth, y + baseHeight, z + halfWidth), shaftHeight, color))
        pillarWalls.add(Wall(Vec3(x - halfWidth, y + baseHeight, z + halfWidth), Vec3(x - halfWidth, y + baseHeight, z - halfWidth), shaftHeight, color))

        // Top of the pillar (slightly wider again)
        val topHeight = 0.5
        val topWidth = width * 1.1
        val topHalfWidth = topWidth / 2
        val topY = y + baseHeight + shaftHeight

        pillarWalls.add(Wall(Vec3(x - topHalfWidth, topY, z - topHalfWidth), Vec3(x + topHalfWidth, topY, z - topHalfWidth), topHeight, color))
        pillarWalls.add(Wall(Vec3(x + topHalfWidth, topY, z - topHalfWidth), Vec3(x + topHalfWidth, topY, z + topHalfWidth), topHeight, color))
        pillarWalls.add(Wall(Vec3(x + topHalfWidth, topY, z + topHalfWidth), Vec3(x - topHalfWidth, topY, z + topHalfWidth), topHeight, color))
        pillarWalls.add(Wall(Vec3(x - topHalfWidth, topY, z + topHalfWidth), Vec3(x - topHalfWidth, topY, z - topHalfWidth), topHeight, color))

        return pillarWalls
    }
}