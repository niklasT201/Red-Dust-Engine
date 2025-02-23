import java.awt.Color
import java.awt.Image

data class Vec3(var x: Double, var y: Double, var z: Double)
data class Wall(val start: Vec3, val end: Vec3, val height: Double, val color: Color, val texture: ImageEntry? = null)
data class Floor(val x1: Double, val z1: Double, val x2: Double, val z2: Double, val y: Double, val color: Color, val texture: ImageEntry? = null)

data class ImageEntry(
    val name: String,
    val path: String,
    val image: Image
)

enum class Direction {
    NORTH, EAST, SOUTH, WEST;

    fun rotate(): Direction = when (this) {
        NORTH -> EAST
        EAST -> SOUTH
        SOUTH -> WEST
        WEST -> NORTH
    }
}

data class WallCoords(
    val startX: Double,
    val startZ: Double,
    val endX: Double,
    val endZ: Double
)

data class GridCell(
    val objectsByFloor: MutableMap<Int, MutableList<GameObject>> = mutableMapOf()
) {
    fun getObjectsForFloor(floor: Int): MutableList<GameObject> {
        return objectsByFloor.getOrPut(floor) { mutableListOf() }
    }

    fun addObject(floor: Int, obj: GameObject) {
        val floorObjects = getObjectsForFloor(floor)
        // Only remove existing object of same type if it's on the same floor
        floorObjects.removeIf { it.type == obj.type }
        floorObjects.add(obj)
    }

    fun removeObject(floor: Int, type: ObjectType) {
        objectsByFloor[floor]?.removeIf { it.type == type }
        // Remove floor entry if empty
        if (objectsByFloor[floor]?.isEmpty() == true) {
            objectsByFloor.remove(floor)
        }
    }
}

sealed class GameObject {
    abstract val type: ObjectType
    abstract val color: Color
    abstract val height: Double
    abstract val width: Double
    abstract val direction: Direction
    open val texture: ImageEntry? = null
}

enum class ObjectType {
    FLOOR,
    WALL,
    PROP  // For future use with other objects
}

data class WallObject(
    override val color: Color,
    override val height: Double,
    override val width: Double,
    override val direction: Direction,
    override val texture: ImageEntry? = null,
    val isBlockWall: Boolean,
    val floorHeight: Double  // Add this new property
) : GameObject() {
    override val type = ObjectType.WALL
}

data class FloorObject(
    override val color: Color,
    val floorHeight: Double,
    override val texture: ImageEntry? = null
) : GameObject() {
    override val type = ObjectType.FLOOR
    override val height = 0.0
    override val width = 2.0
    override val direction = Direction.NORTH
}