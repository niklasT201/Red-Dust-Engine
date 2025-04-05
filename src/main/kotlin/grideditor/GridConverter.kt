package grideditor

import Floor
import FloorObject
import PillarObject
import Ramp
import RampObject
import Vec3
import Wall
import WallCoords
import WallObject
import WaterObject
import WaterSurface

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

                    // Calculate base Y position based on floor number
                    val baseYPosition = floorNum * editor.floorHeight

                    // Apply the floor's height offset
                    val finalYPosition = baseYPosition + obj.floorHeight

                    floors.add(
                        Floor(
                            x1 = gameX,
                            z1 = gameZ,
                            x2 = gameX + editor.baseScale,
                            z2 = gameZ + editor.baseScale,
                            y = finalYPosition,  // Use the adjusted Y position
                            color = obj.color,
                            texture = obj.texture
                        )
                    )
                }
            }
        }
        return floors
    }

    fun generatePillars(): List<Wall> {
        val pillarWalls = mutableListOf<Wall>()

        editor.grid.forEach { (pos, cell) ->
            cell.objectsByFloor.keys.sorted().forEach { floor ->
                val objectsInFloor = cell.objectsByFloor[floor] ?: return@forEach
                val floorHeight = floor * editor.floorHeight // Calculate Y position for this floor level

                // Process ONLY PillarObjects on this floor
                objectsInFloor.filterIsInstance<PillarObject>().forEach { obj ->
                    val (x, y) = pos // Grid coordinates
                    // Calculate center of the grid cell in game coordinates
                    val cellCenterX = -x * editor.baseScale + editor.baseScale / 2.0
                    val cellCenterZ = y * editor.baseScale + editor.baseScale / 2.0

                    // The base of the pillar (slightly wider)
                    val baseHeight = 0.5
                    val baseWidth = obj.width * 1.2
                    val baseHalfWidth = baseWidth / 2.0

                    // The main shaft height (reserving space for base and top)
                    val shaftHeight = obj.height - 1.0
                    val shaftHalfWidth = obj.width / 2.0

                    // The top of the pillar (slightly wider than shaft, narrower than base)
                    val topHeight = 0.5
                    val topWidth = obj.width * 1.1
                    val topHalfWidth = topWidth / 2.0
                    val topY = floorHeight + baseHeight + shaftHeight

                    // BASE walls
                    pillarWalls.add(Wall(
                        Vec3(cellCenterX - baseHalfWidth, floorHeight, cellCenterZ - baseHalfWidth),
                        Vec3(cellCenterX + baseHalfWidth, floorHeight, cellCenterZ - baseHalfWidth),
                        baseHeight, obj.color, obj.texture
                    ))
                    pillarWalls.add(Wall(
                        Vec3(cellCenterX + baseHalfWidth, floorHeight, cellCenterZ - baseHalfWidth),
                        Vec3(cellCenterX + baseHalfWidth, floorHeight, cellCenterZ + baseHalfWidth),
                        baseHeight, obj.color, obj.texture
                    ))
                    pillarWalls.add(Wall(
                        Vec3(cellCenterX + baseHalfWidth, floorHeight, cellCenterZ + baseHalfWidth),
                        Vec3(cellCenterX - baseHalfWidth, floorHeight, cellCenterZ + baseHalfWidth),
                        baseHeight, obj.color, obj.texture
                    ))
                    pillarWalls.add(Wall(
                        Vec3(cellCenterX - baseHalfWidth, floorHeight, cellCenterZ + baseHalfWidth),
                        Vec3(cellCenterX - baseHalfWidth, floorHeight, cellCenterZ - baseHalfWidth),
                        baseHeight, obj.color, obj.texture
                    ))

                    // SHAFT walls (positioned on top of the base)
                    pillarWalls.add(Wall(
                        Vec3(cellCenterX - shaftHalfWidth, floorHeight + baseHeight, cellCenterZ - shaftHalfWidth),
                        Vec3(cellCenterX + shaftHalfWidth, floorHeight + baseHeight, cellCenterZ - shaftHalfWidth),
                        shaftHeight, obj.color, obj.texture
                    ))
                    pillarWalls.add(Wall(
                        Vec3(cellCenterX + shaftHalfWidth, floorHeight + baseHeight, cellCenterZ - shaftHalfWidth),
                        Vec3(cellCenterX + shaftHalfWidth, floorHeight + baseHeight, cellCenterZ + shaftHalfWidth),
                        shaftHeight, obj.color, obj.texture
                    ))
                    pillarWalls.add(Wall(
                        Vec3(cellCenterX + shaftHalfWidth, floorHeight + baseHeight, cellCenterZ + shaftHalfWidth),
                        Vec3(cellCenterX - shaftHalfWidth, floorHeight + baseHeight, cellCenterZ + shaftHalfWidth),
                        shaftHeight, obj.color, obj.texture
                    ))
                    pillarWalls.add(Wall(
                        Vec3(cellCenterX - shaftHalfWidth, floorHeight + baseHeight, cellCenterZ + shaftHalfWidth),
                        Vec3(cellCenterX - shaftHalfWidth, floorHeight + baseHeight, cellCenterZ - shaftHalfWidth),
                        shaftHeight, obj.color, obj.texture
                    ))

                    // TOP walls (positioned on top of the shaft)
                    pillarWalls.add(Wall(
                        Vec3(cellCenterX - topHalfWidth, topY, cellCenterZ - topHalfWidth),
                        Vec3(cellCenterX + topHalfWidth, topY, cellCenterZ - topHalfWidth),
                        topHeight, obj.color, obj.texture
                    ))
                    pillarWalls.add(Wall(
                        Vec3(cellCenterX + topHalfWidth, topY, cellCenterZ - topHalfWidth),
                        Vec3(cellCenterX + topHalfWidth, topY, cellCenterZ + topHalfWidth),
                        topHeight, obj.color, obj.texture
                    ))
                    pillarWalls.add(Wall(
                        Vec3(cellCenterX + topHalfWidth, topY, cellCenterZ + topHalfWidth),
                        Vec3(cellCenterX - topHalfWidth, topY, cellCenterZ + topHalfWidth),
                        topHeight, obj.color, obj.texture
                    ))
                    pillarWalls.add(Wall(
                        Vec3(cellCenterX - topHalfWidth, topY, cellCenterZ + topHalfWidth),
                        Vec3(cellCenterX - topHalfWidth, topY, cellCenterZ - topHalfWidth),
                        topHeight, obj.color, obj.texture
                    ))
                }
            }
        }

        return pillarWalls
    }

    fun generateWaters(): List<WaterSurface> {
        val waters = mutableListOf<WaterSurface>()

        editor.grid.forEach { (pos, cell) ->
            // Get all floors that have water objects in this cell
            val floorsWithWater = cell.objectsByFloor.filterValues { objects ->
                objects.any { it.type == ObjectType.WATER }
            }.keys.sorted()

            floorsWithWater.forEach { floorNum ->
                cell.objectsByFloor[floorNum]?.filterIsInstance<WaterObject>()?.forEach { obj ->
                    val (x, y) = pos
                    val gameX = -x * editor.baseScale
                    val gameZ = y * editor.baseScale

                    // Calculate Y position based on floor number
                    val yPosition = floorNum * editor.floorHeight

                    waters.add(
                        WaterSurface(
                            x1 = gameX,
                            z1 = gameZ,
                            x2 = gameX + editor.baseScale,
                            z2 = gameZ + editor.baseScale,
                            y = yPosition,
                            depth = obj.depth,
                            waveHeight = obj.waveHeight,
                            waveSpeed = obj.waveSpeed,
                            damagePerSecond = obj.damagePerSecond,
                            color = obj.color,
                            texture = obj.texture
                        )
                    )
                }
            }
        }
        return waters
    }

    fun generateRamps(): List<Ramp> {
        val ramps = mutableListOf<Ramp>()

        editor.grid.forEach { (pos, cell) ->
            // Iterate through floors that have ramp objects
            val floorsWithRamps = cell.objectsByFloor.filterValues { objects ->
                objects.any { it.type == ObjectType.RAMP }
            }.keys.sorted()

            floorsWithRamps.forEach { floorNum -> // Using floorNum for clarity, but obj.floorHeight is used for Y
                cell.objectsByFloor[floorNum]?.filterIsInstance<RampObject>()?.forEach { obj ->
                    val (x, y) = pos // Grid coordinates
                    val gameX = -x * editor.baseScale
                    val gameZ = y * editor.baseScale
                    val cellSize = editor.baseScale

                    val yLow = obj.floorHeight // Base height from the object itself
                    val yHigh = obj.floorHeight + obj.rampHeight // Top height

                    // Define corners in XZ plane (counter-clockwise from top-left in editor view)
                    val xz1 = Pair(gameX, gameZ)                     // NW corner in XZ
                    val xz2 = Pair(gameX + cellSize, gameZ)          // NE corner in XZ
                    val xz3 = Pair(gameX + cellSize, gameZ + cellSize) // SE corner in XZ
                    val xz4 = Pair(gameX, gameZ + cellSize)          // SW corner in XZ

                    // Determine Y coordinates based on slope direction
                    val c1: Vec3
                    val c2: Vec3
                    val c3: Vec3
                    val c4: Vec3

                    when (obj.slopeDirection) {
                        // Slopes UP towards North (+Z)
                        Direction.NORTH -> {
                            c1 = Vec3(xz1.first, yLow, xz1.second) // NW Low
                            c2 = Vec3(xz2.first, yLow, xz2.second) // NE Low
                            c3 = Vec3(xz3.first, yHigh, xz3.second) // SE High
                            c4 = Vec3(xz4.first, yHigh, xz4.second) // SW High
                        }
                        Direction.SOUTH -> {
                            c1 = Vec3(xz1.first, yHigh, xz1.second) // NW High
                            c2 = Vec3(xz2.first, yHigh, xz2.second) // NE High
                            c3 = Vec3(xz3.first, yLow, xz3.second) // SE Low
                            c4 = Vec3(xz4.first, yLow, xz4.second) // SW Low
                        }
                        Direction.EAST -> { // Slopes UP towards East (+X)
                            c1 = Vec3(xz1.first, yLow, xz1.second) // NW Low
                            c2 = Vec3(xz2.first, yHigh, xz2.second) // NE High
                            c3 = Vec3(xz3.first, yHigh, xz3.second) // SE High
                            c4 = Vec3(xz4.first, yLow, xz4.second) // SW Low
                        }
                        Direction.WEST -> { // Slopes UP towards West (-X)
                            c1 = Vec3(xz1.first, yHigh, xz1.second) // NW High
                            c2 = Vec3(xz2.first, yLow, xz2.second) // NE Low
                            c3 = Vec3(xz3.first, yLow, xz3.second) // SE Low
                            c4 = Vec3(xz4.first, yHigh, xz4.second) // SW High
                        }
                    }

                    ramps.add(
                        Ramp(
                            corner1 = c1,
                            corner2 = c2,
                            corner3 = c3,
                            corner4 = c4,
                            color = obj.color,
                            texture = obj.texture
                        )
                    )
                }
            }
        }
        return ramps
    }
}