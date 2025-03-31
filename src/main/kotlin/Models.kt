import java.awt.Color
import java.awt.Image

data class Vec3(var x: Double, var y: Double, var z: Double)
data class Wall(val start: Vec3, val end: Vec3, val height: Double, val color: Color, val texture: ImageEntry? = null,  val textureMapping: TextureMapping = TextureMapping())
data class Floor(val x1: Double, val z1: Double, val x2: Double, val z2: Double, val y: Double, val color: Color, val texture: ImageEntry? = null,  val textureMapping: TextureMapping = TextureMapping())
data class WaterSurface(val x1: Double, val z1: Double, val x2: Double, val z2: Double, val y: Double, val depth: Double, val waveHeight: Double, val waveSpeed: Double, val damagePerSecond: Double, val color: Color, val texture: ImageEntry? = null, val textureMapping: TextureMapping = TextureMapping())

data class TextureMapping(
    val scale: Double = 1.0,
    val offsetU: Double = 0.0,
    val offsetV: Double = 0.0,
    val rotation: Double = 0.0
)

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
    // List of surface types that should replace each other
    private val surfaceTypes = setOf(ObjectType.FLOOR, ObjectType.WATER)

    fun getObjectsForFloor(floor: Int): MutableList<GameObject> {
        return objectsByFloor.getOrPut(floor) { mutableListOf() }
    }

    fun addObject(floor: Int, obj: GameObject) {
        val floorObjects = getObjectsForFloor(floor)

        // Always remove existing object of same type
        floorObjects.removeIf { it.type == obj.type }

        // If this is a surface type, also remove other surface types
        if (surfaceTypes.contains(obj.type)) {
            floorObjects.removeIf { surfaceTypes.contains(it.type) }
        }
        floorObjects.add(obj)
    }

    fun removeObject(floor: Int, type: ObjectType) {
        objectsByFloor[floor]?.removeIf { it.type == type }
        // Remove floor entry if empty
        if (objectsByFloor[floor]?.isEmpty() == true) {
            objectsByFloor.remove(floor)
        }
    }

    // Method to get all objects across all floors
    fun getAllObjects(): List<GameObject> {
        return objectsByFloor.values.flatten()
    }

    fun getOccupiedFloors(): Set<Int> {
        // Simply return the keys from the objectsByFloor map
        return objectsByFloor.keys.toSet()
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
    PILLAR,
    WATER,
    PROP, // For future use with other objects
    PLAYER_SPAWN  // New object type for player spawn
}

data class PlayerSpawnObject(
    override val color: Color = Color.GREEN,
    override val height: Double = 1.0,
    override val width: Double = 1.0,
    override val direction: Direction = Direction.NORTH,
    val offsetX: Double = 0.5, // Default to cell center
    val offsetY: Double = 0.5  // Default to cell center
) : GameObject() {
    override val type = ObjectType.PLAYER_SPAWN
    override val texture: ImageEntry? = null
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

data class PillarObject(
    override val color: Color = Color(180, 170, 150),
    override val height: Double = 4.0,
    override val width: Double = 1.0,
    override val direction: Direction = Direction.NORTH,
    override val texture: ImageEntry? = null,
    val floorHeight: Double
) : GameObject() {
    override val type = ObjectType.PILLAR
}

data class WaterObject(
    override val color: Color = Color(0, 105, 148, 200), // Semi-transparent blue by default
    val floorHeight: Double,
    val depth: Double = 2.0,
    val waveHeight: Double = 0.1,
    val waveSpeed: Double = 1.0,
    val damagePerSecond: Double = 0.0, // Optional damage effect
    override val texture: ImageEntry? = null
) : GameObject() {
    override val type = ObjectType.WATER // Add WATER to your ObjectType enum
    override val height = 0.0
    override val width = 2.0
    override val direction = Direction.NORTH
}