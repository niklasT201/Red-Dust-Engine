import java.awt.*
import java.awt.event.*
import javax.swing.*
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

class GridEditor : JPanel() {
    private val grid = mutableMapOf<Pair<Int, Int>, CellData>()
    private var selectedCellType = CellType.WALL
    private var currentCellType = CellType.WALL
    private var isDragging = false
    private var lastCell: Pair<Int, Int>? = null
    private var isRightMouseButton = false
    var useBlockWalls = false

    enum class EditMode {
        DRAW, SELECT, MOVE, ROTATE
    }

    // Camera reference for player position
    private var cameraRef: Camera? = null
    private val baseScale = 2.0  // Keep this constant for grid calculations
    private var currentWallHeight = 3.0
    private var currentWallWidth = 2.0
    private var currentWallColor = Color(150, 0, 0)
    private var currentDirection = Direction.NORTH

    // View properties
    private var viewportX = 0.0 // Center of viewport in grid coordinates
    private var viewportY = 0.0
    private var cellSize = 20.0 // Initial cell size in pixels
    private val visibleCellPadding = 2 // Extra cells to render beyond viewport

    private var currentMode = EditMode.DRAW
    private var selectedCell: Pair<Int, Int>? = null
    private var onCellSelected: ((CellData?) -> Unit)? = null
    private val selectedCells = mutableSetOf<Pair<Int, Int>>()
    private var moveStartPosition: Pair<Int, Int>? = null
    private var isMultiSelectEnabled = false

    enum class CellType {
        WALL, FLOOR
    }

    init {
        background = Color(30, 33, 40)
        preferredSize = Dimension(400, 400)

        // Updated mouse listeners for painting and selection
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                when (currentMode) {
                    EditMode.DRAW -> {
                        isRightMouseButton = SwingUtilities.isRightMouseButton(e)
                        handleMouseEvent(e)
                        isDragging = true
                    }
                    EditMode.SELECT -> {
                        val (gridX, gridY) = screenToGrid(e.x, e.y)
                        val clickedCell = Pair(gridX, gridY)

                        // Update selection and notify listeners
                        if (grid.containsKey(clickedCell)) {
                            selectedCell = clickedCell
                            onCellSelected?.invoke(grid[clickedCell])
                        } else {
                            selectedCell = null
                            onCellSelected?.invoke(null)
                        }
                        repaint()
                    }
                    EditMode.MOVE -> {
                        val (gridX, gridY) = screenToGrid(e.x, e.y)
                        val clickedCell = Pair(gridX, gridY)

                        // Check if Control key is pressed for multi-select
                        isMultiSelectEnabled = e.isControlDown

                        if (grid.containsKey(clickedCell)) {
                            if (!isMultiSelectEnabled) {
                                selectedCells.clear()
                            }
                            selectedCells.add(clickedCell)
                            moveStartPosition = clickedCell
                        } else if (selectedCells.isNotEmpty()) {
                            // Move selected cells to new position
                            val deltaX = gridX - moveStartPosition!!.first
                            val deltaY = gridY - moveStartPosition!!.second

                            // Create new cells at target positions
                            val movedCells = mutableMapOf<Pair<Int, Int>, CellData>()
                            selectedCells.forEach { cell ->
                                val newPos = Pair(cell.first + deltaX, cell.second + deltaY)
                                grid[cell]?.let { cellData ->
                                    movedCells[newPos] = cellData
                                }
                            }

                            // Remove old cells
                            selectedCells.forEach { grid.remove(it) }

                            // Add new cells
                            grid.putAll(movedCells)

                            // Clear selection after move
                            selectedCells.clear()
                            moveStartPosition = null

                            // Notify listeners that the grid has changed
                            firePropertyChange("gridChanged", null, grid)
                        }
                        repaint()
                    }
                    EditMode.ROTATE -> {
                        val (gridX, gridY) = screenToGrid(e.x, e.y)
                        val clickedCell = Pair(gridX, gridY)

                        if (grid.containsKey(clickedCell)) {
                            grid[clickedCell]?.let { cellData ->
                                grid[clickedCell] = cellData.copy(
                                    direction = cellData.direction.rotate()
                                )
                                repaint()
                                firePropertyChange("gridChanged", null, grid)
                            }
                        }
                    }
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                isDragging = false
                lastCell = null
                isRightMouseButton = false
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                if (isDragging && currentMode == EditMode.DRAW) {
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

        inputMap.put(KeyStroke.getKeyStroke(KeyBindings.ROTATE_NORTH, 0), "rotate_north")
        inputMap.put(KeyStroke.getKeyStroke(KeyBindings.ROTATE_WEST, 0), "rotate_west")
        inputMap.put(KeyStroke.getKeyStroke(KeyBindings.ROTATE_SOUTH, 0), "rotate_south")
        inputMap.put(KeyStroke.getKeyStroke(KeyBindings.ROTATE_EAST, 0), "rotate_east")

        actionMap.put("rotate_north", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                setWallDirection(Direction.NORTH)
                repaint()
            }
        })
        actionMap.put("rotate_west", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                setWallDirection(Direction.WEST)
                repaint()
            }
        })
        actionMap.put("rotate_south", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                setWallDirection(Direction.SOUTH)
                repaint()
            }
        })
        actionMap.put("rotate_east", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                setWallDirection(Direction.EAST)
                repaint()
            }
        })

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

    fun setCellType(type: CellType) {
        currentCellType = type
    }

    fun setWallColor(color: Color) {
        currentWallColor = color
        repaint()
    }

    fun setWallHeight(height: Double) {
        currentWallHeight = height
        // Don't modify existing walls, only affect new ones
        repaint()
    }

    fun setWallWidth(width: Double) {
        currentWallWidth = width
        // Don't modify existing walls, only affect new ones
        repaint()
    }

    private fun setWallDirection(direction: Direction) {
        if (currentMode == EditMode.DRAW) {
            currentDirection = direction
        } else if (currentMode == EditMode.SELECT && selectedCell != null) {
            grid[selectedCell]?.let { cellData ->
                grid[selectedCell!!] = cellData.copy(direction = direction)
                repaint()
                firePropertyChange("gridChanged", null, grid)
            }
        }
    }

    private fun handleMouseEvent(e: MouseEvent) {
        val (gridX, gridY) = screenToGrid(e.x, e.y)
        val currentCell = Pair(gridX, gridY)

        if (currentCell != lastCell) {
            if (isRightMouseButton) {
                // Remove cell when right mouse button is pressed
                grid.remove(currentCell)
            } else {
                // Add cell when left mouse button is pressed
                val cellData = when (currentCellType) {
                    CellType.WALL -> CellData(
                        CellType.WALL,
                        currentWallColor,
                        useBlockWalls,
                        currentWallHeight,
                        currentWallWidth,
                        currentDirection
                    )
                    CellType.FLOOR -> CellData(
                        CellType.FLOOR,
                        Color(100, 100, 100),  // Default floor color
                        false,
                        0.0,  // Height for floor is always 0
                        2.0,  // Standard floor tile width
                        Direction.NORTH  // Direction doesn't matter for floors
                    )
                }
                grid[currentCell] = cellData
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
        // Added a +1 offset to correct the position
        return Pair((-x / baseScale) + 1, z / baseScale)
    }

    fun setEditMode(mode: EditMode) {
        currentMode = mode
        // Clear selection when switching modes
        if (mode == EditMode.DRAW) {
            selectedCell = null
            onCellSelected?.invoke(null)
            repaint()
        }
    }

    // Function to set selection callback
    fun setOnCellSelectedListener(listener: (CellData?) -> Unit) {
        onCellSelected = listener
    }

    // Function to update selected cell properties
    fun updateSelectedCell(color: Color? = null, height: Double? = null, width: Double? = null) {
        selectedCell?.let { cell ->
            grid[cell]?.let { cellData ->
                grid[cell] = cellData.copy(
                    color = color ?: cellData.color,
                    height = height ?: cellData.height,
                    width = width ?: cellData.width
                )
                repaint()
                // Notify that grid has changed
                firePropertyChange("gridChanged", null, grid)
            }
        }
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
                val cellData = grid[Pair(x, y)]
                when (cellData?.type) {
                    CellType.WALL -> g2.color = cellData.color
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

        if (currentMode == EditMode.MOVE) {
            selectedCells.forEach { (x, y) ->
                val (screenX, screenY) = gridToScreen(x, y)
                g2.color = Color(0, 255, 255, 100) // Semi-transparent cyan
                g2.stroke = BasicStroke(2f)
                g2.drawRect(
                    screenX,
                    screenY,
                    cellSize.toInt(),
                    cellSize.toInt()
                )
            }
        }

        // Draw selection highlight
        selectedCell?.let { (x, y) ->
            val (screenX, screenY) = gridToScreen(x, y)
            g2.color = Color(255, 255, 0, 100) // Semi-transparent yellow
            g2.stroke = BasicStroke(2f)
            g2.drawRect(
                screenX,
                screenY,
                cellSize.toInt(),
                cellSize.toInt()
            )
        }

        // Draw direction text
        g2.color = Color.WHITE // Set text color to white
        g2.font = Font("Monospace", Font.BOLD, 12)
        g2.drawString(currentDirection.name,
            10, // X position for the text
            25 // Y position for the text
        )

        // Draw direction letters on wall tiles
        grid.forEach { (pos, cellData) ->
            if (cellData.type == CellType.WALL && !cellData.isBlockWall) {
                val (x, y) = pos
                val (screenX, screenY) = gridToScreen(x, y)

                // Draw direction letter
                g2.color = Color.WHITE
                g2.font = Font("Monospace", Font.BOLD, (cellSize * 0.4).toInt())
                val letter = cellData.direction.name.first().toString()
                val metrics = g2.fontMetrics
                val letterX = screenX + (cellSize - metrics.stringWidth(letter))/2
                val letterY = screenY + (cellSize + metrics.height)/2 - metrics.descent
                g2.drawString(letter, letterX.toInt(), letterY.toInt())
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

        grid.forEach { (pos, cellData) ->
            if (cellData.type == CellType.WALL) {
                val (x, y) = pos
                // Use baseScale for position calculation, but cell's width for wall size
                val gameX = -x * baseScale
                val gameZ = y * baseScale

                if (cellData.isBlockWall) {
                    // Block walls
                    walls.addAll(listOf(
                        // North wall
                        Wall(
                            start = Vec3(gameX, 0.0, gameZ),
                            end = Vec3(gameX + cellData.width, 0.0, gameZ),
                            height = cellData.height,
                            color = cellData.color
                        ),
                        // East wall
                        Wall(
                            start = Vec3(gameX + cellData.width, 0.0, gameZ),
                            end = Vec3(gameX + cellData.width, 0.0, gameZ + cellData.width),
                            height = cellData.height,
                            color = cellData.color
                        ),
                        // South wall
                        Wall(
                            start = Vec3(gameX + cellData.width, 0.0, gameZ + cellData.width),
                            end = Vec3(gameX, 0.0, gameZ + cellData.width),
                            height = cellData.height,
                            color = cellData.color
                        ),
                        // West wall
                        Wall(
                            start = Vec3(gameX, 0.0, gameZ + cellData.width),
                            end = Vec3(gameX, 0.0, gameZ),
                            height = cellData.height,
                            color = cellData.color
                        )
                    ))
                } else {
                    // Flat walls with rotation support
                    val coords = when (cellData.direction) {
                        Direction.NORTH -> WallCoords(
                            gameX, gameZ,
                            gameX + cellData.width, gameZ
                        )
                        Direction.WEST -> WallCoords(
                            gameX + cellData.width, gameZ,
                            gameX + cellData.width, gameZ + cellData.width
                        )
                        Direction.SOUTH -> WallCoords(
                            gameX + cellData.width, gameZ + cellData.width,
                            gameX, gameZ + cellData.width
                        )
                        Direction.EAST -> WallCoords(
                            gameX, gameZ + cellData.width,
                            gameX, gameZ
                        )
                    }

                    walls.add(
                        Wall(
                            start = Vec3(coords.startX, 0.0, coords.startZ),
                            end = Vec3(coords.endX, 0.0, coords.endZ),
                            height = cellData.height,
                            color = cellData.color
                        )
                    )
                }
            }
        }
        return walls
    }

    fun generateFloors(): List<Floor> {
        val floors = mutableListOf<Floor>()

        grid.forEach { (pos, cellData) ->
            if (cellData.type == CellType.FLOOR) {
                val (x, y) = pos
                val gameX = -x * baseScale
                val gameZ = y * baseScale

                floors.add(
                    Floor(
                        x1 = gameX,
                        z1 = gameZ,
                        x2 = gameX + baseScale,
                        z2 = gameZ + baseScale,
                        y = 0.0,
                        color = cellData.color
                    )
                )
            }
        }
        return floors
    }

    fun clearGrid() {
        grid.clear()
        repaint()
        // Notify listeners that the grid has changed
        firePropertyChange("gridChanged", null, grid)
    }
}