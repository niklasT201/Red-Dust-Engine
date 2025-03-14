package grideditor

import AddObjectCommand
import FloorObject
import GridCell
import PlayerSpawnObject
import RemoveObjectCommand
import WallObject
import controls.KeyBindings
import java.awt.Color
import java.awt.event.*
import javax.swing.*

/**
 * Handles all input events for the grid editor
 */
class GridInputHandler(private val editor: GridEditor) {

    fun setupListeners() {
        setupMouseListeners()
        setupKeyBindings()
        setupZoomControl()
    }

    private fun setupMouseListeners() {
        editor.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                when (editor.currentMode) {
                    GridEditor.EditMode.DRAW -> {
                        editor.isRightMouseButton = SwingUtilities.isRightMouseButton(e)
                        editor.handleMouseEvent(e)
                        editor.isDragging = true
                    }
                    GridEditor.EditMode.SELECT -> handleSelectMode(e)
                    GridEditor.EditMode.MOVE -> handleMoveMode(e)
                    GridEditor.EditMode.ROTATE -> handleRotateMode(e)
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                editor.isDragging = false
                editor.lastCell = null
                editor.isRightMouseButton = false
            }
        })

        editor.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                if (editor.isDragging && editor.currentMode == GridEditor.EditMode.DRAW) {
                    editor.handleMouseEvent(e)
                }
            }
        })
    }

    private fun handleSelectMode(e: MouseEvent) {
        val (gridX, gridY) = editor.screenToGrid(e.x, e.y)
        val clickedCell = Pair(gridX, gridY)

        // Update selection and notify listeners
        if (editor.grid.containsKey(clickedCell)) {
            editor.selectedCell = clickedCell
            editor.onCellSelected?.invoke(editor.grid[clickedCell])
        } else {
            editor.selectedCell = null
            editor.onCellSelected?.invoke(null)
        }
        editor.repaint()
    }

    private fun handleMoveMode(e: MouseEvent) {
        val (gridX, gridY) = editor.screenToGrid(e.x, e.y)
        val clickedCell = Pair(gridX, gridY)

        // Check if Control key is pressed for multi-select
        editor.isMultiSelectEnabled = e.isControlDown

        if (editor.grid.containsKey(clickedCell)) {
            if (!editor.isMultiSelectEnabled) {
                editor.selectedCells.clear()
            }
            editor.selectedCells.add(clickedCell)
            editor.moveStartPosition = clickedCell
        } else if (editor.selectedCells.isNotEmpty()) {
            // Move selected cells to new position
            val deltaX = gridX - editor.moveStartPosition!!.first
            val deltaY = gridY - editor.moveStartPosition!!.second

            // Create new cells at target positions
            val movedCells = mutableMapOf<Pair<Int, Int>, GridCell>()
            editor.selectedCells.forEach { cell ->
                val newPos = Pair(cell.first + deltaX, cell.second + deltaY)
                editor.grid[cell]?.let { cellData ->
                    // Create new GridCell with the same floor structure
                    val newCell = GridCell()
                    // Copy all objects for each floor
                    cellData.objectsByFloor.forEach { (floor, objects) ->
                        newCell.objectsByFloor[floor] = objects.toMutableList()
                    }
                    movedCells[newPos] = newCell
                }
            }

            // Remove old cells
            editor.selectedCells.forEach { editor.grid.remove(it) }

            // Add new cells
            editor.grid.putAll(movedCells)

            // Clear selection after move
            editor.selectedCells.clear()
            editor.moveStartPosition = null

            // Notify listeners that the grid has changed
            editor.notifyGridChanged()
        }
        editor.repaint()
    }

    private fun handleRotateMode(e: MouseEvent) {
        val (gridX, gridY) = editor.screenToGrid(e.x, e.y)
        val clickedCell = Pair(gridX, gridY)

        if (editor.grid.containsKey(clickedCell)) {
            editor.grid[clickedCell]?.let { cell ->
                // Get objects for current floor and find wall
                cell.getObjectsForFloor(editor.getCurrentFloor()).filterIsInstance<WallObject>().firstOrNull()?.let { wallObject ->
                    // Create new wall object with rotated direction
                    val newWall = wallObject.copy(direction = wallObject.direction.rotate())
                    // Remove old wall and add new one on current floor only
                    cell.removeObject(editor.getCurrentFloor(), ObjectType.WALL)
                    cell.addObject(editor.getCurrentFloor(), newWall)
                    editor.repaint()
                    editor.notifyGridChanged()
                }
            }
        }
    }

    private fun setupKeyBindings() {
        // Direction key bindings
        val windowInputMap = editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        windowInputMap.put(KeyStroke.getKeyStroke(KeyBindings.ROTATE_NORTH, 0), "rotate_north")
        windowInputMap.put(KeyStroke.getKeyStroke(KeyBindings.ROTATE_WEST, 0), "rotate_west")
        windowInputMap.put(KeyStroke.getKeyStroke(KeyBindings.ROTATE_SOUTH, 0), "rotate_south")
        windowInputMap.put(KeyStroke.getKeyStroke(KeyBindings.ROTATE_EAST, 0), "rotate_east")

        // Object type key bindings
        windowInputMap.put(KeyStroke.getKeyStroke(KeyBindings.WALL_SHORTCUT, 0), "select_wall")
        windowInputMap.put(KeyStroke.getKeyStroke(KeyBindings.FLOOR_SHORTCUT, 0), "select_floor")
        windowInputMap.put(KeyStroke.getKeyStroke(KeyBindings.PLAYER_SPAWN_SHORTCUT, 0), "select_player_spawn")

        val undoKeystroke = KeyStroke.getKeyStroke(KeyBindings.UNDO, KeyEvent.CTRL_DOWN_MASK)
        val redoKeystroke = KeyStroke.getKeyStroke(KeyBindings.REDO, KeyEvent.CTRL_DOWN_MASK)

        windowInputMap.put(undoKeystroke, "undo_action")
        windowInputMap.put(redoKeystroke, "redo_action")

        // Direction actions
        editor.actionMap.put("rotate_north", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                editor.setWallDirection(Direction.NORTH)
                editor.repaint()
            }
        })
        editor.actionMap.put("rotate_west", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                editor.setWallDirection(Direction.WEST)
                editor.repaint()
            }
        })
        editor.actionMap.put("rotate_south", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                editor.setWallDirection(Direction.SOUTH)
                editor.repaint()
            }
        })
        editor.actionMap.put("rotate_east", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                editor.setWallDirection(Direction.EAST)
                editor.repaint()
            }
        })

        // Object type actions
        editor.actionMap.put("select_wall", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                editor.setObjectType(ObjectType.WALL)
            }
        })
        editor.actionMap.put("select_floor", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                editor.setObjectType(ObjectType.FLOOR)
            }
        })
        editor.actionMap.put("select_player_spawn", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                editor.setObjectType(ObjectType.PLAYER_SPAWN)
            }
        })

        // Panning key bindings
        editor.inputMap.put(KeyStroke.getKeyStroke("LEFT"), "pan_left")
        editor.inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "pan_right")
        editor.inputMap.put(KeyStroke.getKeyStroke("UP"), "pan_up")
        editor.inputMap.put(KeyStroke.getKeyStroke("DOWN"), "pan_down")

        editor.actionMap.put("pan_left", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                editor.viewportX -= 1
                editor.repaint()
            }
        })
        editor.actionMap.put("pan_right", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                editor.viewportX += 1
                editor.repaint()
            }
        })
        editor.actionMap.put("pan_up", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                editor.viewportY -= 1
                editor.repaint()
            }
        })
        editor.actionMap.put("pan_down", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                editor.viewportY += 1
                editor.repaint()
            }
        })
        editor.actionMap.put("undo_action", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                if (editor.commandManager.undo()) {
                    editor.repaint()
                    editor.notifyGridChanged()
                }
            }
        })
        editor.actionMap.put("redo_action", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                if (editor.commandManager.redo()) {
                    editor.repaint()
                    editor.notifyGridChanged()
                }
            }
        })
    }

    private fun setupZoomControl() {
        editor.addMouseWheelListener { e ->
            val zoomFactor = if (e.wheelRotation < 0) 1.1 else 0.9
            editor.cellSize *= zoomFactor
            editor.cellSize = editor.cellSize.coerceIn(5.0, 100.0) // Limit zoom levels
            editor.repaint()
        }
    }

    fun handleCellEdit(e: MouseEvent) {
        val (gridX, gridY) = editor.screenToGrid(e.x, e.y)
        val currentCell = Pair(gridX, gridY)

        if (currentCell != editor.lastCell) {
            if (editor.isRightMouseButton) {
                // Create a remove command
                editor.grid[currentCell]?.let { cell ->
                    val objectType = editor.currentObjectType
                    if (cell.getObjectsForFloor(editor.getCurrentFloor()).any { it.type == objectType }) {
                        val command = RemoveObjectCommand(
                            editor.grid,
                            currentCell,
                            editor.getCurrentFloor(),
                            objectType
                        )
                        editor.commandManager.executeCommand(command)
                    }
                }
            } else {
                // Get or create cell
                val texture = editor.currentWallTexture
                println("Using texture for new wall: ${texture?.name ?: "null"}")

                // Create new object
                val newObject = when (editor.currentObjectType) {
                    ObjectType.WALL -> WallObject(
                        color = editor.currentWallColor,
                        height = editor.currentWallHeight,
                        width = editor.currentWallWidth,
                        direction = editor.currentDirection,
                        isBlockWall = editor.useBlockWalls,
                        floorHeight = editor.currentFloorHeight,
                        texture = editor.currentWallTexture
                    )
                    ObjectType.FLOOR -> FloorObject(
                        color = Color(100, 100, 100),
                        floorHeight = editor.currentFloorHeight,
                        texture = editor.currentFloorTexture,
                    )
                    ObjectType.PLAYER_SPAWN -> {
                        // Update camera position immediately
                        editor.cameraRef?.position?.x = -gridX * editor.baseScale
                        editor.cameraRef?.position?.z = gridY * editor.baseScale
                        PlayerSpawnObject()
                    }
                    ObjectType.PROP -> null
                }

                newObject?.let {
                    val command = AddObjectCommand(
                        editor.grid,
                        currentCell,
                        editor.getCurrentFloor(),
                        it
                    )
                    editor.commandManager.executeCommand(command)
                }
            }

            editor.lastCell = currentCell
            editor.repaint()

            // Notify listeners that the grid has changed
            editor.notifyGridChanged()
        }
    }
}