import java.awt.Color

data class Vec3(var x: Double, var y: Double, var z: Double)
data class Wall(val start: Vec3, val end: Vec3, val height: Double, val color: Color)
data class Floor(val x1: Double, val z1: Double, val x2: Double, val z2: Double, val y: Double, val color: Color)

enum class Direction {
    NORTH, EAST, SOUTH, WEST;

    fun rotate(): Direction = when (this) {
        NORTH -> EAST
        EAST -> SOUTH
        SOUTH -> WEST
        WEST -> NORTH
    }
}

data class CellData(
    val type: GridEditor.CellType,
    var color: Color = Color(150, 0, 0),
    var isBlockWall: Boolean = false,
    var height: Double = 3.0,
    var width: Double = 2.0,
    var direction: Direction = Direction.NORTH
)

data class WallCoords(
    val startX: Double,
    val startZ: Double,
    val endX: Double,
    val endZ: Double
)