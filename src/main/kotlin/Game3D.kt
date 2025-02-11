import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import javax.swing.*

class Game3D : JPanel(), KeyListener, MouseMotionListener {
    private val camera = Camera(Vec3(0.0, 1.7, -5.0))
    private val renderer = Renderer(800, 600)
    private val keysPressed = mutableSetOf<Int>()
    private var editorMode = false
    private lateinit var contentPane: JPanel
    private lateinit var editorUI: EditorUI
    private lateinit var gridEditor: GridEditor

    // Empty lists for walls and floors - will be populated by editor
    private val walls = mutableListOf<Wall>()
    private val floors = mutableListOf<Floor>()

    init {
        preferredSize = Dimension(800, 600)
        isFocusable = true
        addKeyListener(this)
        addMouseMotionListener(this)

        cursor = Toolkit.getDefaultToolkit().createCustomCursor(
            BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB),
            Point(8, 8),
            "blank"
        )
    }

    fun setComponents(contentPane: JPanel, editorUI: EditorUI, gridEditor: GridEditor) {
        this.contentPane = contentPane
        this.editorUI = editorUI
        this.gridEditor = gridEditor
    }

    fun updateGeometry(editorWalls: List<Wall>, editorFloors: List<Floor>) {
        walls.clear()
        floors.clear()
        walls.addAll(editorWalls)
        floors.addAll(editorFloors)
        repaint()
    }

    private fun toggleEditorMode() {
        editorMode = !editorMode
        val cardLayout = contentPane.layout as CardLayout

        if (editorMode) {
            cardLayout.show(contentPane, "editor")
            cursor = Cursor.getDefaultCursor()
            editorUI.sideBar.isVisible = true
            gridEditor.requestFocusInWindow()
        } else {
            // Update the game geometry when switching back to game mode
            updateGeometry(gridEditor.getWalls(), gridEditor.getFloors())
            cardLayout.show(contentPane, "game")
            cursor = Toolkit.getDefaultToolkit().createCustomCursor(
                BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB),
                Point(8, 8),
                "blank"
            )
            editorUI.sideBar.isVisible = false
            requestFocusInWindow()
        }

        editorUI.updateModeButton()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Clear screen with sky color
        g2.color = Color(135, 206, 235)
        g2.fillRect(0, 0, width, height)

        // Draw floors first
        floors.forEach { floor -> renderer.drawFloor(g2, floor, camera) }

        // Draw walls
        walls.forEach { wall -> renderer.drawWall(g2, wall, camera) }

        // Draw crosshair
        g2.color = Color.WHITE
        g2.drawLine(width/2 - 10, height/2, width/2 + 10, height/2)
        g2.drawLine(width/2, height/2 - 10, width/2, height/2 + 10)
    }

    fun update() {
        var forward = 0.0
        var right = 0.0
        var up = 0.0

        if (KeyEvent.VK_W in keysPressed) forward += 1.0
        if (KeyEvent.VK_S in keysPressed) forward -= 1.0
        if (KeyEvent.VK_A in keysPressed) right -= 1.0
        if (KeyEvent.VK_D in keysPressed) right += 1.0
        if (KeyEvent.VK_SPACE in keysPressed) up += 1.0
        if (KeyEvent.VK_SHIFT in keysPressed) up -= 1.0

        camera.move(forward, right, up)

        val margin = 0.5
        camera.position.x = camera.position.x.coerceIn(-5.0 + margin, 5.0 - margin)
        camera.position.z = camera.position.z.coerceIn(-5.0 + margin, 5.0 - margin)
        camera.position.y = camera.position.y.coerceIn(0.5, 2.5)

        repaint()
    }

    override fun mouseMoved(e: MouseEvent) {
        val dx = e.x - width/2
        val dy = e.y - height/2
        camera.rotate(dx.toDouble(), dy.toDouble())

        try {
            val robot = Robot()
            robot.mouseMove(
                locationOnScreen.x + width/2,
                locationOnScreen.y + height/2
            )
        } catch (e: Exception) {
            // Handle potential security exceptions
        }
    }

    override fun keyPressed(e: KeyEvent) {
        keysPressed.add(e.keyCode)

        if (e.keyCode == KeyEvent.VK_E) {
            toggleEditorMode()
        }
    }
    override fun keyReleased(e: KeyEvent) { keysPressed.remove(e.keyCode) }
    override fun keyTyped(e: KeyEvent) {}
    override fun mouseDragged(e: MouseEvent) {}
}