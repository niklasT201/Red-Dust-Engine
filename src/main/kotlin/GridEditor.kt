import java.awt.*
import java.awt.event.*
import javax.swing.*
import kotlin.math.round

class GridEditor(
    private val width: Int,
    private val height: Int,
    private val cellSize: Int = 50
) : JPanel(), KeyListener {
    private var cameraOffset = Vec3(0.0, 0.0, 0.0)
    private var lastMousePos = Point(0, 0)
    private var isDragging = false
    private var selectedTool = EditorTool.WALL
    private val walls = mutableListOf<Wall>()
    private val floors = mutableListOf<Floor>()
    private lateinit var game: Game3D

    enum class EditorTool {
        WALL, FLOOR, PLAYER, MOVE
    }

    init {
        preferredSize = Dimension(width, height)
        isFocusable = true
        addKeyListener(this)
        setupInputHandlers()
    }

    fun setGame(game: Game3D) {
        this.game = game
    }

    private fun setupInputHandlers() {
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                when (selectedTool) {
                    EditorTool.MOVE -> {
                        isDragging = true
                        lastMousePos = e.point
                    }
                    EditorTool.WALL -> handleWallPlacement(e)
                    EditorTool.FLOOR -> handleFloorPlacement(e)
                    EditorTool.PLAYER -> handlePlayerPlacement(e)
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                isDragging = false
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                if (isDragging && selectedTool == EditorTool.MOVE) {
                    val dx = e.x - lastMousePos.x
                    val dy = e.y - lastMousePos.y
                    cameraOffset.x += dx.toDouble()
                    cameraOffset.y += dy.toDouble()
                    lastMousePos = e.point
                    repaint()
                }
            }
        })
    }

    private fun snapToGrid(x: Double, y: Double): Pair<Double, Double> {
        val gridX = round(x / cellSize) * cellSize
        val gridY = round(y / cellSize) * cellSize
        return Pair(gridX, gridY)
    }

    private fun handleWallPlacement(e: MouseEvent) {
        val (gridX, gridY) = snapToGrid(
            e.x - cameraOffset.x,
            e.y - cameraOffset.y
        )

        val start = Vec3(gridX, 0.0, gridY)
        val end = Vec3(gridX + cellSize, 0.0, gridY)
        walls.add(Wall(start, end, 3.0, Color.RED))
        repaint()
    }

    private fun handleFloorPlacement(e: MouseEvent) {
        val (gridX, gridY) = snapToGrid(
            e.x - cameraOffset.x,
            e.y - cameraOffset.y
        )

        floors.add(Floor(
            gridX, gridY,
            gridX + cellSize, gridY + cellSize,
            0.0, Color.GRAY
        ))
        repaint()
    }

    private fun handlePlayerPlacement(e: MouseEvent) {
        // Implement player placement logic here
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Draw background
        g2d.color = Color.BLACK
        g2d.fillRect(0, 0, width, height)

        // Draw grid
        g2d.color = Color(50, 50, 50)
        g2d.translate(cameraOffset.x, cameraOffset.y)

        // Draw horizontal grid lines
        for (y in 0..height step cellSize) {
            g2d.drawLine(0, y, width, y)
        }

        // Draw vertical grid lines
        for (x in 0..width step cellSize) {
            g2d.drawLine(x, 0, x, height)
        }

        // Draw floors
        floors.forEach { floor ->
            g2d.color = floor.color
            g2d.fillRect(
                floor.x1.toInt(),
                floor.z1.toInt(),
                cellSize,
                cellSize
            )
        }

        // Draw walls
        walls.forEach { wall ->
            g2d.color = wall.color
            g2d.drawLine(
                wall.start.x.toInt(),
                wall.start.z.toInt(),
                wall.end.x.toInt(),
                wall.end.z.toInt()
            )
        }

        g2d.translate(-cameraOffset.x, -cameraOffset.y)
    }

    fun setTool(tool: EditorTool) {
        selectedTool = tool
    }

    fun clearWalls() {
        walls.clear()
        repaint()
    }

    fun clearFloors() {
        floors.clear()
        repaint()
    }

    fun clearAll() {
        walls.clear()
        floors.clear()
        repaint()
    }

    fun getWalls(): List<Wall> = walls.toList()
    fun getFloors(): List<Floor> = floors.toList()

    override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_E) {
            // Simulate pressing E on the game panel
            game.dispatchEvent(e)
        }
    }

    override fun keyReleased(e: KeyEvent) {}
    override fun keyTyped(e: KeyEvent) {}
}