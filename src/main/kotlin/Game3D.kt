import grideditor.GridEditor
import player.Player
import ui.EditorPanel
import ui.FloorSelectorPanel
import ui.MenuSystem
import java.awt.*
import java.awt.event.*
import javax.swing.*
import kotlin.math.cos
import kotlin.math.sin

class Game3D : JPanel() {
    private val player = Player()
    private val renderer = Renderer(800, 600)
    private lateinit var menuSystem: MenuSystem
    private val keysPressed = mutableSetOf<Int>()
    private val walls = mutableListOf<Wall>()
    private val floors = mutableListOf<Floor>()
    private var isEditorMode = true

    private val renderPanel = RenderPanel()
    private val gridEditor = GridEditor()
    private val editorPanel = EditorPanel { toggleEditorMode() }

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
        gridEditor.addPropertyChangeListener("gridChanged") { evt ->
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

        editorPanel.setColorChangeListener { color ->
            gridEditor.setWallColor(color)
            val newWalls = gridEditor.generateWalls()
            updateWalls(newWalls)
        }

        // Initialize floor grid
        for (x in -1..1) {
            for (z in -1..1) {
                floors.add(
                    Floor(
                        x1 = x.toDouble() * 2.0,
                        z1 = z.toDouble() * 2.0,
                        x2 = (x + 1).toDouble() * 2.0,
                        z2 = (z + 1).toDouble() * 2.0,
                        y = 0.0,
                        color = if ((x + z) % 2 == 0) Color(100, 100, 100) else Color(150, 150, 150)
                    )
                )
            }
        }

        // Setup render panel
        renderPanel.apply {
            preferredSize = Dimension(800, 600)
            background = Color(135, 206, 235)
            isFocusable = true
        }

        // Set initial size for editor panel
        editorPanel.preferredSize = Dimension(250, height)

        // Create menu bar
        menuSystem = MenuSystem(
            onFloorSelected = { level ->
                // Update editor panel's floor selector
                editorPanel.sectionChooser.setCurrentFloor(level)
                // Update grid editor
                gridEditor.setCurrentFloor(level)
                gridEditor.updateCurrentFloorHeight(level * 4.0) // 4 units between floors
                renderPanel.repaint()
            },
            onFloorAdded = { isAbove ->
                val floors = editorPanel.sectionChooser.floors
                val newLevel = if (isAbove) {
                    floors.maxOf { it.level } + 1
                } else {
                    floors.minOf { it.level } - 1
                }

                // Add new floor to both menu and section chooser
                menuSystem.addFloor(newLevel)
                editorPanel.sectionChooser.floors.add(
                    FloorSelectorPanel.Floor(newLevel)
                )

                // Update UI
                editorPanel.sectionChooser.updateFloorButtons(
                    editorPanel.sectionChooser.findFloorsPanel()
                )
                renderPanel.repaint()
            }
        )

        // Add listeners to keep menu and section chooser in sync
        editorPanel.sectionChooser.addPropertyChangeListener("currentFloorChanged") { evt ->
            val floor = evt.newValue as FloorSelectorPanel.Floor
            menuSystem.setCurrentFloor(floor.level)
        }

        editorPanel.sectionChooser.addPropertyChangeListener("floorsChanged") { _ ->
            // Ensure menu shows all available floors
            editorPanel.sectionChooser.floors.forEach { floor ->
                menuSystem.addFloor(floor.level)
            }
        }

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
                    player.camera.position.y = 1.7  // Default player height
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

            g2.color = Color(135, 206, 235)
            g2.fillRect(0, 0, width, height)

            renderer.drawScene(g2, walls, floors, player.camera)

            if (!isEditorMode) {
                // Draw crosshair
                g2.color = Color.WHITE
                g2.drawLine(width/2 - 10, height/2, width/2 + 10, height/2)
                g2.drawLine(width/2, height/2 - 10, width/2, height/2 + 10)

                // Draw direction indicator
                g2.font = Font("Monospace", Font.BOLD, 14)

                // Get cardinal direction from player
                val direction = player.getCardinalDirection()

                // Get angles in degrees for display
                val yawDegrees = player.getYawDegrees()
                val pitchDegrees = player.getPitchDegrees()

                // Draw debug information
                g2.color = Color.WHITE
                g2.drawString("Direction: $direction (${yawDegrees}°)", 10, 20)
                g2.drawString("Position: (${String.format("%.1f", player.position.x)}, ${String.format("%.1f", player.position.y)}, ${String.format("%.1f", player.position.z)})", 10, 40)
            }
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
            if (KeyBindings.MOVE_UP in keysPressed) up += 1.0
            if (KeyBindings.MOVE_DOWN in keysPressed) up -= 1.0

            // Move player with collected input
            player.move(forward, right, up, walls)

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