package grideditor

import player.Camera
import Direction
import Floor
import GridCell
import ObjectType
import Wall
import WallObject
import texturemanager.ResourceManager
import texturemanager.TextureManagerPanel
import ImageEntry
import java.awt.*
import javax.swing.*

class GridEditor : JPanel() {
    val grid = mutableMapOf<Pair<Int, Int>, GridCell>()
    var currentObjectType = ObjectType.WALL
    var isDragging = false
    var lastCell: Pair<Int, Int>? = null
    var isRightMouseButton = false
    var useBlockWalls = false
    var showFlatWallsAsLines = false

    // View properties
    var viewportX = 0.0 // Center of viewport in grid coordinates
    var viewportY = 0.0
    var cellSize = 20.0 // Initial cell size in pixels
    val visibleCellPadding = 2 // Extra cells to render beyond viewport

    // player.Camera reference for player position
    var cameraRef: Camera? = null
    val baseScale = 2.0  // constant for grid calculations
    val floorHeight = 3.0

    // Grid editing state
    var currentWallHeight = 3.0
    var currentWallWidth = 2.0
    var currentWallColor = Color(150, 0, 0)
    var currentDirection = Direction.NORTH
    var currentFloorHeight = 0.0
    private var currentFloor = 0

    // Texture management
    var resourceManager: ResourceManager? = null
    private var textureManagerPanel: TextureManagerPanel? = null
    var currentWallTexture: ImageEntry? = null
    var currentFloorTexture: ImageEntry? = null

    // View properties
    var currentMode = EditMode.DRAW
    var selectedCell: Pair<Int, Int>? = null
    var onCellSelected: ((GridCell?) -> Unit)? = null
    val selectedCells = mutableSetOf<Pair<Int, Int>>()
    var moveStartPosition: Pair<Int, Int>? = null
    var isMultiSelectEnabled = false

    // Delegate components
    private val inputHandler = GridInputHandler(this)
    private val renderer = GridRenderer(this)
    private val converter = GridConverter(this)

    init {
        background = Color(30, 33, 40)
        preferredSize = Dimension(400, 400)

        // Request focus immediately when created
        SwingUtilities.invokeLater {
            requestFocusInWindow()
        }

        // Setup input handlers
        inputHandler.setupListeners()

        isFocusable = true
    }

    // Set resource manager
    fun initializeResourceManager(manager: ResourceManager) {
        resourceManager = manager
    }

    // Set texture manager panel
    fun initializeTextureManagerPanel(panel: TextureManagerPanel) {
        textureManagerPanel = panel
    }

    // Grid coordinate conversions
    fun screenToGrid(screenX: Int, screenY: Int): Pair<Int, Int> {
        return renderer.screenToGrid(screenX, screenY)
    }

    // World coordinate conversions
    fun worldToGrid(x: Double, z: Double): Pair<Double, Double> {
        return converter.worldToGrid(x, z)
    }

    // State setters
    fun setObjectType(type: ObjectType) {
        currentObjectType = type
        repaint()
    }

    fun getCurrentFloor(): Int {
        return currentFloor
    }

    fun setCurrentFloor(floor: Int) {
        currentFloor = floor
        repaint()
    }

    fun setWallColor(color: Color) {
        currentWallColor = color
        repaint()
    }

    fun setWallHeight(height: Double) {
        currentWallHeight = height
        repaint()
    }

    fun setWallWidth(width: Double) {
        currentWallWidth = width
        repaint()
    }

    fun setWallTexture(texture: ImageEntry?) {
        println("Setting wall texture to: ${texture?.name ?: "null"}")
        currentWallTexture = texture
        repaint()
    }

    fun setFloorTexture(texture: ImageEntry?) {
        currentFloorTexture = texture
        repaint()
    }

    fun setWallDirection(direction: Direction) {
        if (currentMode == EditMode.DRAW) {
            currentDirection = direction
        } else if (currentMode == EditMode.SELECT && selectedCell != null) {
            grid[selectedCell]?.let { cell ->
                cell.getObjectsForFloor(currentFloor).filterIsInstance<WallObject>().firstOrNull()?.let { wallObject ->
                    // Create new wall object with rotated direction
                    val newWall = wallObject.copy(direction = direction)
                    // Replace old wall with new one on current floor
                    cell.removeObject(currentFloor, ObjectType.WALL)
                    cell.addObject(currentFloor, newWall)
                    repaint()
                    firePropertyChange("gridChanged", null, grid)
                }
            }
        }
    }

    fun setCamera(camera: Camera) {
        cameraRef = camera
        repaint()
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

    fun setOnCellSelectedListener(listener: (GridCell?) -> Unit) {
        onCellSelected = listener
    }

    fun setFlatWallVisualization(showAsLines: Boolean) {
        showFlatWallsAsLines = showAsLines
        repaint()
    }

    fun getScaledTexture(image: Image, size: Int): Image {
        // Create scaled version for better performance, especially when zooming
        return image.getScaledInstance(size, size, Image.SCALE_FAST)
    }

    fun updateSelectedCell(color: Color? = null, height: Double? = null, width: Double? = null, texture: ImageEntry? = null) {
        selectedCell?.let { cell ->
            grid[cell]?.let { gridCell ->
                gridCell.getObjectsForFloor(currentFloor).filterIsInstance<WallObject>().firstOrNull()?.let { wallObject ->
                    // Remove old wall object from current floor
                    gridCell.removeObject(currentFloor, ObjectType.WALL)
                    // Add new wall object with updated properties to current floor
                    gridCell.addObject(currentFloor, wallObject.copy(
                        color = color ?: wallObject.color,
                        height = height ?: wallObject.height,
                        width = width ?: wallObject.width,
                        texture = texture ?: wallObject.texture
                    ))
                    repaint()
                    firePropertyChange("gridChanged", null, grid)
                }
            }
        }
    }

    // Cell editing methods
    fun handleMouseEvent(e: java.awt.event.MouseEvent) {
        inputHandler.handleCellEdit(e)
    }

    fun updateCurrentFloorHeight(height: Double) {
        currentFloorHeight = height
        repaint()
    }

    // Grid to game world conversion
    fun generateWalls(): List<Wall> {
        return converter.generateWalls()
    }

    fun generateFloors(): List<Floor> {
        return converter.generateFloors()
    }

    fun notifyGridChanged() {
        firePropertyChange("gridChanged", null, grid)
    }

    fun clearGrid() {
        grid.clear()
        repaint()
        // Notify listeners that the grid has changed
        firePropertyChange("gridChanged", null, grid)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        renderer.render(g as Graphics2D)
    }

    enum class EditMode {
        DRAW, SELECT, MOVE, ROTATE
    }
}