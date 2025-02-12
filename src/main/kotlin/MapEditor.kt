import java.awt.*
import java.awt.event.*
import javax.swing.*

class GridEditor : JPanel() {
    private val gridSize = 20 // Number of cells in each direction
    private val grid = Array(gridSize) { Array(gridSize) { CellType.EMPTY } }
    private var selectedCellType = CellType.WALL
    private var isDragging = false
    private var lastCell: Pair<Int, Int>? = null

    enum class CellType {
        EMPTY, WALL, FLOOR
    }

    init {
        background = Color(30, 33, 40)
        preferredSize = Dimension(400, 400)

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
    }

    private fun handleMouseEvent(e: MouseEvent) {
        val cellSize = getCellSize()
        val x = (e.x / cellSize).toInt().coerceIn(0, gridSize - 1)
        val y = (e.y / cellSize).toInt().coerceIn(0, gridSize - 1)

        val currentCell = Pair(x, y)
        if (currentCell != lastCell) {
            grid[x][y] = selectedCellType
            lastCell = currentCell
            repaint()

            // Notify listeners that the grid has changed
            firePropertyChange("gridChanged", null, grid)
        }
    }

    private fun getCellSize(): Double {
        return minOf(width.toDouble(), height.toDouble()) / gridSize
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val cellSize = getCellSize()

        // Draw cells
        for (x in 0 until gridSize) {
            for (y in 0 until gridSize) {
                val cellX = (x * cellSize).toInt()
                val cellY = (y * cellSize).toInt()

                when (grid[x][y]) {
                    CellType.WALL -> g2.color = Color(150, 0, 0)
                    CellType.FLOOR -> g2.color = Color(100, 100, 100)
                    CellType.EMPTY -> g2.color = Color(40, 44, 52)
                }

                g2.fillRect(cellX, cellY, cellSize.toInt(), cellSize.toInt())

                // Draw grid lines
                g2.color = Color(60, 63, 65)
                g2.drawRect(cellX, cellY, cellSize.toInt(), cellSize.toInt())
            }
        }
    }

    fun setSelectedType(type: CellType) {
        selectedCellType = type
    }

    fun clearGrid() {
        for (x in 0 until gridSize) {
            for (y in 0 until gridSize) {
                grid[x][y] = CellType.EMPTY
            }
        }
        repaint()
    }

    // Convert grid to game walls
    fun generateWalls(): List<Wall> {
        val walls = mutableListOf<Wall>()
        val scale = 0.5 // Scale factor to convert grid coordinates to game coordinates

        for (x in 0 until gridSize) {
            for (y in 0 until gridSize) {
                if (grid[x][y] == CellType.WALL) {
                    // Convert grid coordinates to game coordinates
                    val gameX = (x - gridSize/2) * scale
                    val gameZ = (y - gridSize/2) * scale

                    // Create a wall at this position
                    walls.add(Wall(
                        start = Vec3(gameX.toDouble(), 0.0, gameZ.toDouble()),
                        end = Vec3(gameX.toDouble() + scale, 0.0, gameZ.toDouble()),
                        height = 3.0,
                        color = Color(150, 0, 0)
                    ))
                }
            }
        }
        return walls
    }
}