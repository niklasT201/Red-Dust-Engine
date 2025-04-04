import controls.KeyBindings
import grideditor.GridEditor
import player.Player
import ui.EditorPanel
import ui.MenuSystem
import ui.components.CrosshairShape
import render.SkyRenderer
import ui.GameType
import ui.WelcomeScreen
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.*

class Game3D : JPanel() {
    val player = Player()
    val renderer = Renderer(800, 600)
    private val walls = mutableListOf<Wall>()
    private val floors = mutableListOf<Floor>()
    private val waters = mutableListOf<WaterSurface>()
    private val ramps = mutableListOf<Ramp>()
    var isEditorMode = true
    private var isWorldLoaded = false
    private var skyColor = Color(135, 206, 235)
    private var skyRenderer: SkyRenderer = SkyRenderer(skyColor)

    private var frameCount = 0
    private var lastFpsUpdateTime = System.currentTimeMillis()
    var currentFps = 0

    private val renderPanel = RenderPanel(this, renderer, player, walls, floors, waters, ramps)

    private val gridEditor = GridEditor()
    private val editorPanel = EditorPanel(gridEditor,renderer, this) { toggleEditorMode() }
    //private val settingsSaver = saving.SettingsSaver(gridEditor)
    private lateinit var menuSystem: MenuSystem
    private val keysPressed = mutableSetOf<Int>()

    var isGravityEnabled: Boolean = false // Default state

    // --- Cursor Management ---
    private val blankCursor: Cursor
    private val defaultCursor: Cursor = Cursor.getDefaultCursor()

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

    private val welcomeScreen: WelcomeScreen

    // Add this property to hold the main content panel (that will replace the welcome screen)
    private val contentPanel = JPanel().apply {
        layout = BorderLayout()
    }

    // Add this property to track the game type
    private var gameType: GameType = GameType.LEVEL_BASED


    init {
        layout = CardLayout()

        // Create the welcome screen with callbacks
        welcomeScreen = WelcomeScreen(
            onCreateOpenWorld = {
                gameType = GameType.OPEN_WORLD
                startNewProject()
            },
            onCreateLevelBased = {
                gameType = GameType.LEVEL_BASED
                startNewProject()
            },
            onLoadExisting = { loadExistingWorld() }
        )

        // --- Create Blank Cursor ---
        val cursorImg = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        // Create a blank cursor from the transparent image
        blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
            cursorImg, Point(0, 0), "blank cursor"
        )

        add(welcomeScreen, "welcome")
        add(contentPanel, "content")

        // Initially show the welcome screen
        SwingUtilities.invokeLater { showWelcomeScreen() }

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

        contentPanel.add(menuSystem.createMenuBar(), BorderLayout.NORTH)
        contentPanel.add(splitPane, BorderLayout.CENTER)

        // Set initial divider location
        SwingUtilities.invokeLater {
            splitPane.dividerLocation = 250
        }

        gridEditor.setCamera(player.camera)

        setupInputHandling()
        //updateMode()
        menuSystem.getFileManager().setGameType(gameType)

        //SwingUtilities.invokeLater { settingsSaver.loadPlayerSettings(player, this) }
    }

    fun changeGravityEnabled(enabled: Boolean) {
        isGravityEnabled = enabled
        player.setGravity(enabled) // Update player state as well
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

                    // Toggle Weapon UI Key (Only when not in editor mode)
                    if (e.keyCode == KeyBindings.TOGGLE_WEAPON_UI && !isEditorMode) {
                        // Toggle the visibility state in RenderPanel
                        renderPanel.setGameUIVisible(!renderPanel.isGameUIVisible())
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
        if (isWorldLoaded) {
            isEditorMode = !isEditorMode
            updateMode()
        }
    }

    private fun updateMode() {
        val cardLayout = rightPanel.layout as CardLayout
        if (isEditorMode) {
            cardLayout.show(rightPanel, "editor")
            editorPanel.setModeButtonText("Editor Mode")

            renderPanel.cursor = defaultCursor

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
            renderPanel.cursor = blankCursor
            renderPanel.requestFocusInWindow()
        }
    }

    fun isCrosshairVisible(): Boolean = renderPanel.isCrosshairVisible()
    fun setCrosshairVisible(visible: Boolean) = renderPanel.setCrosshairVisible(visible)
    fun getCrosshairSize(): Int = renderPanel.getCrosshairSize()
    fun setCrosshairSize(size: Int) = renderPanel.setCrosshairSize(size)
    fun getCrosshairColor(): Color = renderPanel.getCrosshairColor()
    fun setCrosshairColor(color: Color) = renderPanel.setCrosshairColor(color)
    fun getCrosshairShape(): CrosshairShape = renderPanel.getCrosshairShape()
    fun setCrosshairShape(shape: CrosshairShape) = renderPanel.setCrosshairShape(shape)
    fun isDirectionVisible(): Boolean = renderPanel.isDirectionVisible()
    fun setDirectionVisible(visible: Boolean) = renderPanel.setDirectionVisible(visible)
    fun isPositionVisible(): Boolean = renderPanel.isPositionVisible()
    fun setPositionVisible(visible: Boolean) = renderPanel.setPositionVisible(visible)
    fun setFpsCounterVisible(visible: Boolean) = renderPanel.setFpsCounterVisible(visible)
    fun isFpsCounterVisible(): Boolean = renderPanel.isFpsCounterVisible()
    fun isGameUIVisible(): Boolean = renderPanel.isGameUIVisible()
    fun setGameUIVisible(visible: Boolean) = renderPanel.setGameUIVisible(visible)

    fun getSkyRenderer(): SkyRenderer = skyRenderer
    fun setSkyRenderer(renderer: SkyRenderer) {
        skyRenderer = renderer
        skyColor = renderer.skyColor  // Keep skyColor in sync with renderer
        renderPanel.repaint()
    }
    fun getSkyColor(): Color = skyColor
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

            if (isGravityEnabled) {
                // Gravity ON: Check for JUMP key
                if (KeyBindings.JUMP in keysPressed) {
                    player.jump() // Attempt to jump
                }
            } else {
                // Gravity OFF: Check for FLY keys
                if (KeyBindings.FLY_UP in keysPressed) up += 1.0
                if (KeyBindings.FLY_DOWN in keysPressed) up -= 1.0
            }

            // Move player with collected input and pass floors list
            player.move(forward, right, up, walls, floors, ramps)

            // Check if the renderPanel still has the blank cursor
            if (renderPanel.cursor != blankCursor) {
                renderPanel.cursor = blankCursor
            }

            gridEditor.repaint()
        }

        // Calculate FPS for every frame
        calculateFps()

        renderPanel.repaint()
    }

    private fun updateWalls(newWalls: List<Wall>) {
        walls.clear()
        walls.addAll(newWalls)
        walls.addAll(gridEditor.generatePillars())

        floors.clear()
        floors.addAll(gridEditor.generateFloors())

        waters.clear()
        waters.addAll(gridEditor.generateWater())

        ramps.clear()
        ramps.addAll(gridEditor.generateRamps())
    }

    private fun startNewProject() {
        // Set game type in file manager
        menuSystem.getFileManager().setGameType(gameType)

        // Clear the grid
        gridEditor.clearGrid()

        // Switch to editor mode
        isEditorMode = true
        updateMode()

        // Show the main content
        showContent()
    }

    private fun loadExistingWorld() {
        // Create a file chooser dialog
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Open Existing World"
            fileSelectionMode = JFileChooser.FILES_ONLY

            // Set root directory to World/saves
            val worldDir = File("World/saves")
            if (worldDir.exists()) {
                currentDirectory = worldDir
            }

            fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                "World Files (*.world)", "world"
            )
        }

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile

            // Determine game type based on file location
            if (file.absolutePath.contains("open_world")) {
                gameType = GameType.OPEN_WORLD
            } else {
                gameType = GameType.LEVEL_BASED
            }

            // Update file manager
            menuSystem.getFileManager().setGameType(gameType)

            // Load the world
            if (menuSystem.getFileManager().loadWorld(file)) {
                // Switch to editor mode
                isEditorMode = true
                updateMode()

                // Show the main content
                showContent()
            } else {
                // Show error
                JOptionPane.showMessageDialog(
                    this,
                    "Failed to load world file.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }

    private fun showWelcomeScreen() {
        (layout as CardLayout).show(this, "welcome")

        // Get the parent window (JFrame)
        val frame = SwingUtilities.getWindowAncestor(this) as? JFrame
        frame?.let {
            // Make the window non-resizable
            it.isResizable = false

            // Set a fixed size for the welcome screen
            val welcomeWidth = 800
            val welcomeHeight = 600
            it.size = Dimension(welcomeWidth, welcomeHeight)

            // Center on screen
            it.setLocationRelativeTo(null)
        }
    }

    private fun showContent() {
        isWorldLoaded = true
        (layout as CardLayout).show(this, "content")

        // Get the parent window (JFrame)
        val frame = SwingUtilities.getWindowAncestor(this) as? JFrame
        frame?.let {
            // Make the window resizable again
            it.isResizable = true

            // You might want to set a minimum size for the editor/game view
            it.minimumSize = Dimension(800, 600)
        }
    }
}