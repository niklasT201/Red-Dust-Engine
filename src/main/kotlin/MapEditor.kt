import java.awt.*
import java.awt.event.*
import javax.swing.*
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

class GridEditor : JPanel() {
    private val grid = mutableMapOf<Pair<Int, Int>, CellType>()
    private var selectedCellType = CellType.WALL
    private var isDragging = false
    private var lastCell: Pair<Int, Int>? = null
    var useBlockWalls = false

    // Camera reference for player position
    private var cameraRef: Camera? = null

    // View properties
    private var viewportX = 0.0 // Center of viewport in grid coordinates
    private var viewportY = 0.0
    private var cellSize = 20.0 // Initial cell size in pixels
    private val visibleCellPadding = 2 // Extra cells to render beyond viewport

    enum class CellType {
        EMPTY, WALL, FLOOR
    }

    init {
        background = Color(30, 33, 40)
        preferredSize = Dimension(400, 400)

        // Add mouse listeners for painting
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                handleMouseEvent(e)
                isDragging = true
            }

            override fun mouseReleased(e: MouseEvent) {
                isDragging = false
                lastCell = null
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                if (isDragging) {
                    handleMouseEvent(e)
                }
            }
        })

        // Add mouse wheel listener for zooming
        addMouseWheelListener { e ->
            val zoomFactor = if (e.wheelRotation < 0) 1.1 else 0.9
            cellSize *= zoomFactor
            cellSize = cellSize.coerceIn(5.0, 100.0) // Limit zoom levels
            repaint()
        }

        // Add key bindings for panning
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "pan_left")
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "pan_right")
        inputMap.put(KeyStroke.getKeyStroke("UP"), "pan_up")
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "pan_down")

        actionMap.put("pan_left", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                viewportX -= 1
                repaint()
            }
        })
        actionMap.put("pan_right", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                viewportX += 1
                repaint()
            }
        })
        actionMap.put("pan_up", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                viewportY -= 1
                repaint()
            }
        })
        actionMap.put("pan_down", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                viewportY += 1
                repaint()
            }
        })

        isFocusable = true
    }

    private fun screenToGrid(screenX: Int, screenY: Int): Pair<Int, Int> {
        val gridX = floor((screenX / cellSize + viewportX - width / (2 * cellSize))).toInt()
        val gridY = floor((screenY / cellSize + viewportY - height / (2 * cellSize))).toInt()
        return Pair(gridX, gridY)
    }

    private fun gridToScreen(gridX: Int, gridY: Int): Pair<Int, Int> {
        val screenX = ((gridX - viewportX + width / (2 * cellSize)) * cellSize).toInt()
        val screenY = ((gridY - viewportY + height / (2 * cellSize)) * cellSize).toInt()
        return Pair(screenX, screenY)
    }

    private fun handleMouseEvent(e: MouseEvent) {
        val (gridX, gridY) = screenToGrid(e.x, e.y)
        val currentCell = Pair(gridX, gridY)

        if (currentCell != lastCell) {
            if (selectedCellType == CellType.EMPTY) {
                grid.remove(currentCell)
            } else {
                grid[currentCell] = selectedCellType
            }
            lastCell = currentCell
            repaint()

            // Notify listeners that the grid has changed
            firePropertyChange("gridChanged", null, grid)
        }
    }

    fun setCamera(camera: Camera) {
        cameraRef = camera
        repaint()
    }

    // Convert world coordinates to grid coordinates
    private fun worldToGrid(x: Double, z: Double): Pair<Double, Double> {
        return Pair(x / 2.0, z / 2.0)  // Divide by 2 because our grid scale is 2.0
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Calculate visible grid bounds
        val (minX, minY) = screenToGrid(0, 0)
        val (maxX, maxY) = screenToGrid(width, height)

        // Draw grid and cells
        for (x in (minX - visibleCellPadding)..(maxX + visibleCellPadding)) {
            for (y in (minY - visibleCellPadding)..(maxY + visibleCellPadding)) {
                val (screenX, screenY) = gridToScreen(x, y)

                // Draw cell content if it exists
                when (grid[Pair(x, y)]) {
                    CellType.WALL -> g2.color = Color(150, 0, 0)
                    CellType.FLOOR -> g2.color = Color(100, 100, 100)
                    else -> g2.color = Color(40, 44, 52)
                }

                g2.fillRect(
                    screenX,
                    screenY,
                    cellSize.toInt(),
                    cellSize.toInt()
                )

                // Draw grid lines
                g2.color = Color(60, 63, 65)
                g2.drawRect(
                    screenX,
                    screenY,
                    cellSize.toInt(),
                    cellSize.toInt()
                )
            }
        }

        // Draw player if camera reference exists
        cameraRef?.let { camera ->
            // Convert world coordinates to grid coordinates
            val (gridX, gridZ) = worldToGrid(camera.position.x, camera.position.z)
            val (screenX, screenY) = gridToScreen(floor(gridX).toInt(), floor(gridZ).toInt())

            // Calculate player position within the cell
            val xOffset = ((gridX % 1) * cellSize).toInt()
            val yOffset = ((gridZ % 1) * cellSize).toInt()

            // Draw player body (green circle)
            g2.color = Color(0, 255, 0)
            val playerSize = (cellSize * 0.3).toInt()
            g2.fillOval(
                screenX + xOffset - playerSize/2,
                screenY + yOffset - playerSize/2,
                playerSize,
                playerSize
            )

            // Draw direction indicator (line)
            val lineLength = cellSize * 0.5
            val dirX = sin(camera.yaw) * lineLength
            val dirZ = cos(camera.yaw) * lineLength
            g2.drawLine(
                screenX + xOffset,
                screenY + yOffset,
                screenX + xOffset + dirX.toInt(),
                screenY + yOffset + dirZ.toInt()
            )
        }
    }

    // Convert grid to game walls
    fun generateWalls(): List<Wall> {
        val walls = mutableListOf<Wall>()
        val scale = 2.0 // Each cell represents a 2x2 unit area in the game world
        val wallHeight = 3.0

        if (useBlockWalls) {
            grid.forEach { (pos, type) ->
                if (type == CellType.WALL) {
                    val (x, y) = pos
                    val gameX = x * scale
                    val gameZ = y * scale

                    walls.addAll(listOf(
                        // North wall
                        Wall(
                            start = Vec3(gameX, 0.0, gameZ),
                            end = Vec3(gameX + scale, 0.0, gameZ),
                            height = wallHeight,
                            color = Color(150, 0, 0)
                        ),
                        // East wall
                        Wall(
                            start = Vec3(gameX + scale, 0.0, gameZ),
                            end = Vec3(gameX + scale, 0.0, gameZ + scale),
                            height = wallHeight,
                            color = Color(150, 0, 0)
                        ),
                        // South wall
                        Wall(
                            start = Vec3(gameX + scale, 0.0, gameZ + scale),
                            end = Vec3(gameX, 0.0, gameZ + scale),
                            height = wallHeight,
                            color = Color(150, 0, 0)
                        ),
                        // West wall
                        Wall(
                            start = Vec3(gameX, 0.0, gameZ + scale),
                            end = Vec3(gameX, 0.0, gameZ),
                            height = wallHeight,
                            color = Color(150, 0, 0)
                        )
                    ))
                }
            }
        } else {
            grid.forEach { (pos, type) ->
                if (type == CellType.WALL) {
                    val (x, y) = pos
                    val gameX = x * scale
                    val gameZ = y * scale

                    // Create a single wall with proper width (scale)
                    walls.add(
                        Wall(
                            start = Vec3(gameX, 0.0, gameZ),
                            end = Vec3(gameX + scale, 0.0, gameZ),
                            height = wallHeight,
                            color = Color(150, 0, 0)
                        )
                    )
                }
            }
        }
        return walls
    }

    fun clearGrid() {
        grid.clear()
        repaint()
    }

    fun setSelectedType(type: CellType) {
        selectedCellType = type
    }
}