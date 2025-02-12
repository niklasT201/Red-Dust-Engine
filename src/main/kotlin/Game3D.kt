import java.awt.*
import java.awt.event.*
import javax.swing.*
import kotlin.math.cos
import kotlin.math.sin

class Game3D : JPanel() {
    private val camera = Camera(Vec3(0.0, 1.7, -5.0))
    private val renderer = Renderer(800, 600)
    private val keysPressed = mutableSetOf<Int>()
    private val walls = mutableListOf(
        Wall(Vec3(-2.0, 0.0, 2.0), Vec3(2.0, 0.0, 2.0), 3.0, Color(150, 0, 0))
    )
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

        // Initialize floor grid (keep your existing code)
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

        renderPanel.apply {
            preferredSize = Dimension(800, 600)
            background = Color(135, 206, 235)
            addKeyListener(GameKeyListener())
            addMouseMotionListener(GameMouseListener())
            isFocusable = true
        }

        // Set initial size for editor panel
        editorPanel.preferredSize = Dimension(250, height)

        // Create menu bar
        val menuBar = JMenuBar()
        menuBar.add(createFileMenu())
        menuBar.add(createEditMenu())
        menuBar.add(createControlsMenu())
        menuBar.add(createHelpMenu())
        add(menuBar, BorderLayout.NORTH)

        // Add the split pane to the main panel
        add(splitPane, BorderLayout.CENTER)

        // Set initial divider location after components are added
        SwingUtilities.invokeLater {
            splitPane.dividerLocation = 250
        }

        setupKeyBindings()

        // Show initial mode
        updateMode()
    }

    private fun createFileMenu(): JMenu {
        return JMenu("File").apply {
            add(JMenuItem("New Project"))
            add(JMenuItem("Open Project"))
            add(JMenuItem("Save"))
            add(JMenuItem("Save As..."))
            addSeparator()
            add(JMenuItem("Exit"))
        }
    }

    private fun createEditMenu(): JMenu {
        return JMenu("Edit").apply {
            add(JMenuItem("Undo"))
            add(JMenuItem("Redo"))
            addSeparator()
            add(JMenuItem("Preferences"))
        }
    }

    private fun createHelpMenu(): JMenu {
        return JMenu("Help").apply {
            add(JMenuItem("Documentation"))
            add(JMenuItem("About"))
        }
    }

    private fun createControlsMenu(): JMenu {
        return JMenu("Controls").apply {
            add(JMenuItem("WASD: Movement"))
            add(JMenuItem("Mouse: Look around"))
            add(JMenuItem("Space: Jump/Up"))
            add(JMenuItem("Shift: Crouch/Down"))
            add(JMenuItem("E: Toggle Editor Mode"))
            addSeparator()
            add(JMenuItem("Configure Controls..."))
        }
    }

    private fun setupKeyBindings() {
        renderPanel.inputMap.put(KeyStroke.getKeyStroke('e'), "toggleEditor")
        renderPanel.actionMap.put("toggleEditor", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                toggleEditorMode()
            }
        })

        // Also add binding for 'e' key (lowercase)
        inputMap.put(KeyStroke.getKeyStroke('e'), "toggleEditor")
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
        } else {
            cardLayout.show(rightPanel, "game")
            editorPanel.setModeButtonText("Game Mode")
            renderPanel.requestFocus()
        }
    }

    inner class RenderPanel : JPanel() {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            g2.color = Color(135, 206, 235)
            g2.fillRect(0, 0, width, height)

            floors.forEach { floor -> renderer.drawFloor(g2, floor, camera) }
            walls.forEach { wall -> renderer.drawWall(g2, wall, camera) }

            if (!isEditorMode) {
                g2.color = Color.WHITE
                g2.drawLine(width/2 - 10, height/2, width/2 + 10, height/2)
                g2.drawLine(width/2, height/2 - 10, width/2, height/2 + 10)
            }
        }
    }

    inner class GameKeyListener : KeyListener {
        override fun keyPressed(e: KeyEvent) {
            if (!isEditorMode) {
                keysPressed.add(e.keyCode)
            }
        }
        override fun keyReleased(e: KeyEvent) {
            keysPressed.remove(e.keyCode)
        }
        override fun keyTyped(e: KeyEvent) {}
    }

    inner class GameMouseListener : MouseMotionListener {
        override fun mouseMoved(e: MouseEvent) {
            if (!isEditorMode) {
                val dx = e.x - renderPanel.width/2
                val dy = e.y - renderPanel.height/2
                camera.rotate(dx.toDouble(), dy.toDouble())

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
        }
        override fun mouseDragged(e: MouseEvent) {}
    }

    fun update() {
        if (!isEditorMode) {
            var forward = 0.0
            var right = 0.0
            var up = 0.0

            // Get movement input
            if (KeyEvent.VK_W in keysPressed) forward += 1.0
            if (KeyEvent.VK_S in keysPressed) forward -= 1.0
            if (KeyEvent.VK_A in keysPressed) right -= 1.0
            if (KeyEvent.VK_D in keysPressed) right += 1.0
            if (KeyEvent.VK_SPACE in keysPressed) up += 1.0
            if (KeyEvent.VK_SHIFT in keysPressed) up -= 1.0

            // Calculate movement based on camera direction
            val moveSpeed = 0.05
            val cosYaw = cos(camera.yaw)
            val sinYaw = sin(camera.yaw)

            // Calculate new position
            val newX = camera.position.x + (forward * sinYaw + right * cosYaw) * moveSpeed
            val newZ = camera.position.z + (forward * cosYaw - right * sinYaw) * moveSpeed
            val newY = camera.position.y + up * moveSpeed

            // Check for collisions with walls
            val playerRadius = 0.3 // Collision radius for the player
            var canMoveX = true
            var canMoveZ = true

            // Check each wall for collision
            for (wall in walls) {
                // Simple box collision check
                val wallMinX = minOf(wall.start.x, wall.end.x) - playerRadius
                val wallMaxX = maxOf(wall.start.x, wall.end.x) + playerRadius
                val wallMinZ = minOf(wall.start.z, wall.end.z) - playerRadius
                val wallMaxZ = maxOf(wall.start.z, wall.end.z) + playerRadius

                // Check X collision
                if (newX in wallMinX..wallMaxX &&
                    camera.position.z in wallMinZ..wallMaxZ) {
                    canMoveX = false
                }

                // Check Z collision
                if (camera.position.x in wallMinX..wallMaxX &&
                    newZ in wallMinZ..wallMaxZ) {
                    canMoveZ = false
                }
            }

            // Apply movement with collision detection
            if (canMoveX) {
                camera.position.x = newX
            }
            if (canMoveZ) {
                camera.position.z = newZ
            }

            // Apply Y movement (up/down) with bounds
            camera.position.y = newY.coerceIn(0.5, 2.5)
        }

        renderPanel.repaint()
    }

    fun updateWalls(newWalls: List<Wall>) {
        walls.clear()
        walls.addAll(newWalls)
    }
}