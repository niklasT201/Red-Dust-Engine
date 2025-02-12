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
    private var isEditorMode = true  // Start in editor mode

    private val renderPanel = RenderPanel()
    private val editorPanel = EditorPanel { toggleEditorMode() }

    init {
        layout = BorderLayout()

        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                renderPanel.preferredSize = Dimension(width - editorPanel.width, height)
                revalidate()
            }
        })

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

        renderPanel.apply {
            preferredSize = Dimension(800, 600)
            background = Color(135, 206, 235)
            addKeyListener(GameKeyListener())
            addMouseMotionListener(GameMouseListener())
            isFocusable = true
        }

        // Create menu bar
        val menuBar = JMenuBar()
        menuBar.add(createFileMenu())
        menuBar.add(createEditMenu())
        menuBar.add(createControlsMenu())
        menuBar.add(createHelpMenu())
        add(menuBar, BorderLayout.NORTH)

        // Add panels (editor panel on the left)
        add(editorPanel, BorderLayout.WEST)
        add(renderPanel, BorderLayout.CENTER)

        setupKeyBindings()
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
    }

    private fun toggleEditorMode() {
        isEditorMode = !isEditorMode
        renderPanel.requestFocus()
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

            // Forward/backward movement in camera direction
            camera.position.x += (forward * sinYaw + right * cosYaw) * moveSpeed
            camera.position.z += (forward * cosYaw - right * sinYaw) * moveSpeed
            camera.position.y += up * moveSpeed

            // Apply position constraints
            val margin = 0.5
            camera.position.x = camera.position.x.coerceIn(-5.0 + margin, 5.0 - margin)
            camera.position.z = camera.position.z.coerceIn(-5.0 + margin, 5.0 - margin)
            camera.position.y = camera.position.y.coerceIn(0.5, 2.5)
        }

        renderPanel.repaint()
    }
}