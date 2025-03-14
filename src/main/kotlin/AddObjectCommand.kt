import java.util.*

// Command interface
interface EditorCommand {
    fun execute()
    fun undo()
}

// Command for adding objects
class AddObjectCommand(
    private val grid: MutableMap<Pair<Int, Int>, GridCell>,
    private val position: Pair<Int, Int>,
    private val floor: Int,
    private val gameObject: GameObject
) : EditorCommand {
    private var oldObject: GameObject? = null

    override fun execute() {
        val cell = grid.getOrPut(position) { GridCell() }
        // Save the old object for undo (if any)
        oldObject = cell.getObjectsForFloor(floor).find { it.type == gameObject.type }
        // Add the new object
        cell.addObject(floor, gameObject)
    }

    override fun undo() {
        grid[position]?.let { cell ->
            cell.removeObject(floor, gameObject.type)
            // Restore old object if it existed
            oldObject?.let { cell.addObject(floor, it) }

            // Remove cell if empty
            if (cell.objectsByFloor.isEmpty()) {
                grid.remove(position)
            }
        }
    }
}

// Command for removing objects
class RemoveObjectCommand(
    private val grid: MutableMap<Pair<Int, Int>, GridCell>,
    private val position: Pair<Int, Int>,
    private val floor: Int,
    private val objectType: ObjectType
) : EditorCommand {
    private var removedObject: GameObject? = null

    override fun execute() {
        grid[position]?.let { cell ->
            // Save the object we're about to remove
            removedObject = cell.getObjectsForFloor(floor).find { it.type == objectType }
            // Remove it
            cell.removeObject(floor, objectType)

            // Remove cell if empty
            if (cell.objectsByFloor.isEmpty()) {
                grid.remove(position)
            }
        }
    }

    override fun undo() {
        removedObject?.let {
            // Recreate cell if needed
            val cell = grid.getOrPut(position) { GridCell() }
            cell.addObject(floor, it)
        }
    }
}

class CommandManager {
    private val undoStack = Stack<EditorCommand>()
    private val redoStack = Stack<EditorCommand>()

    fun executeCommand(command: EditorCommand) {
        command.execute()
        undoStack.push(command)
        redoStack.clear() // Clear redo stack when new command is executed
    }

    fun undo(): Boolean {
        if (undoStack.isNotEmpty()) {
            val command = undoStack.pop()
            command.undo()
            redoStack.push(command)
            return true
        }
        return false
    }

    fun redo(): Boolean {
        if (redoStack.isNotEmpty()) {
            val command = redoStack.pop()
            command.execute()
            undoStack.push(command)
            return true
        }
        return false
    }
}