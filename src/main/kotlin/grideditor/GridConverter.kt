package grideditor

import Floor
import FloorObject
import Vec3
import Wall
import WallCoords
import WallObject
import java.awt.Color
import kotlin.math.floor

/**
 * Handles conversion between grid data and game world objects
 */
class GridConverter(private val editor: GridEditor) {

    fun worldToGrid(x: Double, z: Double): Pair<Double, Double> {
        // Added a +1 offset to correct the position
        return Pair((-x / editor.baseScale) + 1, z / editor.baseScale)
    }

    // Convert grid to game walls
    fun generateWalls(): List<Wall> {
        val walls = mutableListOf<Wall>()

        editor.grid.forEach { (pos, cell) ->
            // Get all floors that have walls in this cell
            val floorsWithWalls = cell.objectsByFloor.filterValues { objects ->
                objects.any { it.type == ObjectType.WALL }
            }.keys.sorted()

            floorsWithWalls.forEach { floor ->
                val wallsInFloor = cell.objectsByFloor[floor]?.filterIsInstance<WallObject>() ?: return@forEach

                wallsInFloor.forEach { obj ->
                    val (x, y) = pos
                    val gameX = -x * editor.baseScale
                    val gameZ = y * editor.baseScale

                    // Calculate Y position based on floor number
                    val floorHeight = floor * editor.floorHeight // Use consistent floor height calculation

                    if (obj.isBlockWall) {
                        // Block walls
                        walls.addAll(
                            listOf(
                                // North wall
                                Wall(
                                    start = Vec3(gameX, floorHeight, gameZ),
                                    end = Vec3(gameX + obj.width, floorHeight, gameZ),
                                    height = obj.height,
                                    color = obj.color,
                                    texture = obj.texture
                                ),
                                // East wall
                                Wall(
                                    start = Vec3(gameX + obj.width, floorHeight, gameZ),
                                    end = Vec3(gameX + obj.width, floorHeight, gameZ + obj.width),
                                    height = obj.height,
                                    color = obj.color,
                                    texture = obj.texture
                                ),
                                // South wall
                                Wall(
                                    start = Vec3(gameX + obj.width, floorHeight, gameZ + obj.width),
                                    end = Vec3(gameX, floorHeight, gameZ + obj.width),
                                    height = obj.height,
                                    color = obj.color,
                                    texture = obj.texture
                                ),
                                // West wall
                                Wall(
                                    start = Vec3(gameX, floorHeight, gameZ + obj.width),
                                    end = Vec3(gameX, floorHeight, gameZ),
                                    height = obj.height,
                                    color = obj.color,
                                    texture = obj.texture
                                )
                            )
                        )
                    } else {
                        // Flat walls with rotation support
                        val coords = when (obj.direction) {
                            Direction.NORTH -> WallCoords(gameX, gameZ, gameX + obj.width, gameZ)
                            Direction.EAST -> WallCoords(gameX, gameZ, gameX, gameZ + obj.width)
                            Direction.SOUTH -> WallCoords(gameX, gameZ + obj.width, gameX + obj.width, gameZ + obj.width)
                            Direction.WEST -> WallCoords(gameX + obj.width, gameZ, gameX + obj.width, gameZ + obj.width)
                        }

                        walls.add(
                            Wall(
                                start = Vec3(coords.startX, floorHeight, coords.startZ),
                                end = Vec3(coords.endX, floorHeight, coords.endZ),
                                height = obj.height,
                                color = obj.color,
                                texture = obj.texture
                            )
                        )
                    }
                }
            }
        }
        return walls
    }

    fun generateFloors(): List<Floor> {
        val floors = mutableListOf<Floor>()

        editor.grid.forEach { (pos, cell) ->
            // Get all floors that have floor objects in this cell
            val floorsWithTiles = cell.objectsByFloor.filterValues { objects ->
                objects.any { it.type == ObjectType.FLOOR }
            }.keys.sorted()

            floorsWithTiles.forEach { floorNum ->
                cell.objectsByFloor[floorNum]?.filterIsInstance<FloorObject>()?.forEach { obj ->
                    val (x, y) = pos
                    val gameX = -x * editor.baseScale
                    val gameZ = y * editor.baseScale

                    // Calculate Y position based on floor number using consistent floor height
                    val yPosition = floorNum * editor.floorHeight

                    floors.add(
                        Floor(
                            x1 = gameX,
                            z1 = gameZ,
                            x2 = gameX + editor.baseScale,
                            z2 = gameZ + editor.baseScale,
                            y = yPosition,
                            color = obj.color,
                            texture = obj.texture
                        )
                    )
                }
            }
        }
        return floors
    }
}