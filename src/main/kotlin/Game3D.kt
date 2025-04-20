import keyinput.KeyBindings
import grideditor.GridEditor
import player.Player
import player.uis.CustomizableGameUI
import ui.EditorPanel
import ui.MenuSystem
import ui.components.CrosshairShape
import render.SkyRenderer
import ui.GameType
import ui.WelcomeScreen
import ui.builder.UIBuilder
import ui.topbar.FileManager
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

    private var lastFrameTime = System.nanoTime() // Track time of each frame
    private var deltaTime: Double = 0.0
    private var frameTimeSum = 0L // Sum of frame times
    private var frameTimeCount = 0
    var currentFps = 0

    private val renderPanel = RenderPanel(this, renderer, player, walls, floors, waters, ramps)

    private val gridEditor = GridEditor()
    private val editorPanel = EditorPanel(gridEditor,renderer, this) { toggleEditorMode() }
    private val settingsSaver = saving.SettingsSaver(gridEditor)
    private lateinit var menuSystem: MenuSystem
    private var fileManager: FileManager
    private var menuBar: JMenuBar
    private val keysPressed = mutableSetOf<Int>()

    var isGravityEnabled: Boolean = false // Default state

    private var uiBuilder: UIBuilder
    private var isUIBuilderMode = false

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
    private val contentPanel = JPanel().apply {
        layout = BorderLayout()
    }
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
                gridEditor.changeCurrentFloor(level)

                // Update the floor height based on the level
                //gridEditor.updateCurrentFloorHeight(level * gridEditor.floorHeight)

                // Update UI to show we're on the new floor
                menuSystem.setCurrentFloor(level)

                // Refresh the rendering
                renderPanel.repaint()
            },
            onFloorAdded = { isAbove ->
                // Get existing floors
                val currentFloors = getExistingFloors()
                val currentFloor = gridEditor.useCurrentFloor()

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
                gridEditor.changeCurrentFloor(newLevel)

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
        this.menuBar = menuSystem.createMenuBar()
        this.fileManager = menuSystem.getFileManager()

        uiBuilder = UIBuilder(this, this.fileManager)

        add(welcomeScreen, "welcome")
        add(contentPanel, "content")
        add(uiBuilder, "uiBuilder")

        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                renderer.updateDimensions(renderPanel.width, renderPanel.height)
            }
        })

        contentPanel.add(this.menuBar, BorderLayout.NORTH)
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
                    // --- UI Builder Toggle ---
                    if (e.keyCode == KeyBindings.TOGGLE_UI_BUILDER && isWorldLoaded) { // Only allow toggle if world is loaded
                        SwingUtilities.invokeLater { toggleUIBuilderMode() }
                        return@addKeyEventDispatcher true // Consume the event
                    }

                    // Prevent editor toggle if UI builder is active
                    if (e.keyCode == KeyBindings.TOGGLE_EDITOR && !isUIBuilderMode) {
                        SwingUtilities.invokeLater { toggleEditorMode() }
                    }

                    // Prevent weapon UI toggle if UI builder is active
                    if (e.keyCode == KeyBindings.TOGGLE_WEAPON_UI && !isEditorMode && !isUIBuilderMode) {
                        renderPanel.setGameUIVisible(!renderPanel.isGameUIVisible())
                    }

                    // Only add movement keys if in game mode AND not in UI builder mode
                    if (!isEditorMode && !isUIBuilderMode) {
                        keysPressed.add(e.keyCode)
                    }
                }
                KeyEvent.KEY_RELEASED -> {
                    // Only remove movement keys if in game mode AND not in UI builder mode
                    // (Or just always remove, simpler)
                    keysPressed.remove(e.keyCode)
//                    if (!isEditorMode && !isUIBuilderMode) {
//                        keysPressed.remove(e.keyCode)
//                    }
                }
            }
            false // Allow the event to be processed further if not consumed
        }

        // Mouse handling for game mode (only if not in UI Builder)
        renderPanel.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                if (!isEditorMode && !isUIBuilderMode) {
                    handleMouseMovement(e)
                }
            }
        })
    }

    private fun toggleUIBuilderMode() {
        // Prevent toggling if no world/project is loaded
        if (!isWorldLoaded) {
            JOptionPane.showMessageDialog(
                this,
                "Please load or create a project first to use the UI Builder.",
                "Project Required",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        isUIBuilderMode = !isUIBuilderMode
        val cardLayout = this.layout as CardLayout

        if (isUIBuilderMode) {
            // Entering UI Builder
            cardLayout.show(this, "uiBuilder")
            cursor = defaultCursor
            renderPanel.cursor = defaultCursor
            this.menuBar.isVisible = false // <--- HIDE the stored menu bar
            SwingUtilities.invokeLater { uiBuilder.requestFocusInWindow() }
        } else {
            // Exiting UI Builder
            cardLayout.show(this, "content")
            this.menuBar.isVisible = true // <--- SHOW the stored menu bar
            updateMode()
        }
    }

    private fun handleMouseMovement(e: MouseEvent) {
        // Only handle if UI builder isn't active
        if (isUIBuilderMode) return

        val dx = e.x - renderPanel.width / 2
        val dy = e.y - renderPanel.height / 2
        player.rotate(dx.toDouble(), dy.toDouble())

        try {
            val robot = Robot()
            val centerPoint = renderPanel.locationOnScreen
            centerPoint.translate(renderPanel.width / 2, renderPanel.height / 2)
            // Check if the mouse is already centered to avoid jitter
            if (MouseInfo.getPointerInfo().location != centerPoint) {
                robot.mouseMove(centerPoint.x, centerPoint.y)
            }
        } catch (ex: Exception) {
            // Handle potential security exceptions or AWTException
            println("Error centering mouse: ${ex.message}")
        }
    }

    private fun toggleEditorMode() {
        if (isUIBuilderMode || !isWorldLoaded) return
            isEditorMode = !isEditorMode
            updateMode()
    }

    private fun updateMode() {
        // If UI builder is active, do nothing here, let toggleUIBuilderMode handle it
        if (isUIBuilderMode) return

        val cardLayout = rightPanel.layout as CardLayout
        if (isEditorMode) {
            cardLayout.show(rightPanel, "editor")
            editorPanel.setModeButtonText("Editor Mode")
            // Use default cursor in editor mode
            cursor = defaultCursor
            renderPanel.cursor = defaultCursor
            SwingUtilities.invokeLater { gridEditor.requestFocusInWindow() }
        } else {
            // Find the player spawn point and set the camera position
            gridEditor.grid.forEach { (pos, cell) ->
                cell.getObjectsForFloor(gridEditor.useCurrentFloor()).filterIsInstance<PlayerSpawnObject>().firstOrNull()?.let { playerSpawn ->
                    val (x, y) = pos

                    // Use the stored offsets to calculate the precise position
                    val worldX = -(x + playerSpawn.offsetX) * gridEditor.baseScale
                    val worldZ = (y + playerSpawn.offsetY) * gridEditor.baseScale

                    // Set player position with precise offset
                    player.camera.position.x = worldX
                    player.camera.position.z = worldZ

                    // Calculate correct Y position based on current floor
                    val floorHeight = gridEditor.useCurrentFloor() * gridEditor.floorHeight
                    player.camera.position.y = 1.7 + floorHeight  // Player eye height + floor height
                }
            }

            cardLayout.show(rightPanel, "game")
            editorPanel.setModeButtonText("Game Mode")
            // Use blank cursor in game mode
            cursor = blankCursor
            renderPanel.cursor = blankCursor
            SwingUtilities.invokeLater { renderPanel.requestFocusInWindow() }
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
    fun setCustomUI(customUI: CustomizableGameUI) {
        renderPanel.setCustomUI(customUI)
        renderPanel.setGameUIVisible(true)
    }

    fun update() {
        val currentTime = System.nanoTime()
        val frameTime = currentTime - lastFrameTime // Time taken for this frame in nanoseconds
        lastFrameTime = currentTime

        // Convert frameTime from nanoseconds to seconds for deltaTime
        // Avoid division by zero or huge values if frameTime is weird (e.g., on first frame or after pause)
        deltaTime = if (frameTime in 1..1_000_000_000) { // Ensure frameTime is reasonable (e.g., < 1 second)
            frameTime / 1_000_000_000.0
        } else {
            1.0 / 60.0 // Default to 60 FPS if frameTime is unusual
        }

        if (!isEditorMode && !isUIBuilderMode) {
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

        }
        // --- Editor Mode Specific Logic ---
        else if (isEditorMode && !isUIBuilderMode) {
            // Ensure default cursor is set if we are in editor mode
            if (cursor != defaultCursor) {
                cursor = defaultCursor
                renderPanel.cursor = defaultCursor // Also set on renderPanel
            }
            // Any other specific logic for editor mode during update could go here
        }

        // Calculate FPS for every frame
        if (frameTime in 1..999999999) { // Less than 1 second
            frameTimeSum += frameTime
            frameTimeCount++

            // Update FPS calculation every 500ms
            if (frameTimeSum > 500_000_000) { // 500ms in nanoseconds
                // Convert average frame time to FPS
                val avgFrameTime = frameTimeSum.toDouble() / frameTimeCount
                currentFps = (1_000_000_000.0 / avgFrameTime).toInt()

                // Reset accumulators
                frameTimeSum = 0
                frameTimeCount = 0
            }
        }

        // Repaint the correct active panel based on the current mode
        if (isUIBuilderMode) {
            uiBuilder.repaint() // Repaint the builder if it's active
        } else {
            // If not in UI builder, repaint the main content (game or editor view)
            renderPanel.repaint()
            if (isEditorMode) {
                gridEditor.repaint() // Also repaint grid editor if in editor mode
            }
        }
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

    private fun loadProjectUI() {
        if (fileManager.getCurrentProjectName() == null) {
            println("No project loaded, cannot load project UI.")
            // Ensure a default UI state in the builder if no project is loaded
            uiBuilder.customizableGameUI.createDefaultLayout(renderPanel.width, renderPanel.height)
            setCustomUI(uiBuilder.customizableGameUI) // Apply the default internal one
            return
        }

        val uiDir = fileManager.getUiDirectory()
        if (uiDir == null || !uiDir.exists()) {
            println("Project UI directory not found. Using default UI.")
            uiBuilder.customizableGameUI.createDefaultLayout(renderPanel.width, renderPanel.height)
            setCustomUI(uiBuilder.customizableGameUI) // Apply the default internal one
            return
        }

        val defaultUiFile = File(uiDir, FileManager.DEFAULT_UI_FILENAME)

        if (defaultUiFile.exists() && defaultUiFile.isFile) {
            println("Found default project UI file: ${defaultUiFile.absolutePath}")
            // Try loading into the UIBuilder's instance
            if (uiBuilder.customizableGameUI.loadFromFile(defaultUiFile)) {
                println("Successfully loaded custom UI from project file.")
                // Apply the loaded UI to the game
                setCustomUI(uiBuilder.customizableGameUI)
                uiBuilder.previewPanel.repaint() // Update preview in builder too
            } else {
                System.err.println("Failed to load custom UI from ${defaultUiFile.name}. Using default.")
                // Fallback to default if loading fails
                uiBuilder.customizableGameUI.createDefaultLayout(renderPanel.width, renderPanel.height)
                setCustomUI(uiBuilder.customizableGameUI)
            }
        } else {
            println("No default UI file ('${FileManager.DEFAULT_UI_FILENAME}') found in project. Using default UI.")
            // Use default if file doesn't exist
            uiBuilder.customizableGameUI.createDefaultLayout(renderPanel.width, renderPanel.height)
            setCustomUI(uiBuilder.customizableGameUI)
        }
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

        println("Started new session. Project name/directory will be requested on first save.")
    }

    private fun loadExistingWorld() {
        // 1. Select Project Directory
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Select Project Directory"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            val projectsDir = File("Projects") // Use constant if preferred: File(FileManager.PROJECTS_DIR)
            if (projectsDir.exists()) {
                currentDirectory = projectsDir
            } else {
                println("Projects directory ('Projects') not found.")
            }
            isAcceptAllFileFilterUsed = false
        }

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val projectDir = fileChooser.selectedFile
            if (projectDir == null || !projectDir.isDirectory) {
                JOptionPane.showMessageDialog(this, "Invalid project directory selected.", "Error", JOptionPane.ERROR_MESSAGE)
                return
            }

            val projectName = projectDir.name
            val projectPath = projectDir.absolutePath

            // 2. Set Project Context in FileManager
            // Ensure menuSystem is initialized before welcome screen interaction if needed,
            // or pass fileManager directly to WelcomeScreen. Assuming menuSystem is ready.
            val fileManager = menuSystem.getFileManager()
            fileManager.setCurrentProjectName(projectName) // This also updates KeyBindings

            // 3. Find the World File to Load
            var worldFileToLoad: File? = null
            var loadedGameType: GameType? = null

            val savesDir = File(projectDir, "saves") // Use constants: File(projectDir, FileManager.SAVES_DIR)
            val openWorldDir = File(savesDir, "open_world") // Use constants
            val openWorldFile = File(openWorldDir, "open_world.world") // Use constants

            if (openWorldFile.exists()) {
                worldFileToLoad = openWorldFile
                loadedGameType = GameType.OPEN_WORLD
            } else {
                val levelsDir = File(savesDir, "levels") // Use constants
                if (levelsDir.exists() && levelsDir.isDirectory) {
                    val levelFiles = levelsDir.listFiles { file ->
                        file.isFile && file.name.lowercase().endsWith(".world") // Use constant
                    }?.sortedBy { it.name }

                    if (levelFiles != null && levelFiles.isNotEmpty()) {
                        worldFileToLoad = levelFiles[0]
                        loadedGameType = GameType.LEVEL_BASED
                    }
                }
            }

            // 4. Load World and Settings if a world file was found
            if (worldFileToLoad != null && loadedGameType != null) {
                // Set the game type before loading
                fileManager.setGameType(loadedGameType)
                this.gameType = loadedGameType // Update Game3D's gameType

                // Load the world grid data
                if (fileManager.loadWorld(worldFileToLoad)) {
                    println("Successfully loaded world: ${worldFileToLoad.absolutePath}")

                    // ---> 5. Load Settings using SettingsManager <---
                    val settingsManager = menuSystem.getSettingsManager()
                    val displayOptionsPanel = editorPanel.getDisplayOptionsPanel() // Get panel instance

                    val (displaySuccess, worldSuccess, playerSuccess) = settingsManager.loadSettings(projectPath, displayOptionsPanel)

                    if (displaySuccess && worldSuccess && playerSuccess) {
                        println("Successfully loaded all project settings.")
                    } else {
                        println("Warning: Some project settings might not have loaded correctly.")
                        // Optionally show a less severe warning message
                        JOptionPane.showMessageDialog(
                            this,
                            "World loaded. Some project settings might not have loaded correctly.\n" +
                                    "Display: $displaySuccess, World: $worldSuccess, Player: $playerSuccess",
                            "Settings Load Warning",
                            JOptionPane.WARNING_MESSAGE
                        )
                    }

                    // 6. Switch UI
                    isEditorMode = true // Start in editor mode after loading
                    //updateMode()
                    showContent() // Show the main editor/game view
                    println("Project '$projectName' loaded.")

                } else {
                    // World loading failed
                    JOptionPane.showMessageDialog(
                        this,
                        "Failed to load world file: ${worldFileToLoad.name}",
                        "World Load Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            } else {
                // No world file found in the project directory
                JOptionPane.showMessageDialog(
                    this,
                    "Could not find a valid world file (open_world.world or level_*.world) in the selected project directory's 'saves' subfolder.",
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }

    private fun showWelcomeScreen() {
        isWorldLoaded = false
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
        isUIBuilderMode = false
        this.menuBar.isVisible = false
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
        isUIBuilderMode = false
        this.menuBar.isVisible = true // <--- SHOW the stored menu bar when content is shown
        updateMode()
        loadProjectUI()
    }
}