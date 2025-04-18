package saving

import Direction
import FloorObject
import GridCell
import ImageEntry
import ObjectType
import PillarObject
import PlayerSpawnObject
import RampObject
import WallObject
import WaterObject
import java.io.*
import java.awt.Color
import grideditor.GridEditor

class WorldSaver {
    /**
     * Saves the current grid state to a file
     * @param grid The grid data to save
     * @param fileName The name of the file to save to
     * @return True if save was successful, false otherwise
     */
    fun saveWorld(gridEditor: GridEditor, fileName: String): Boolean {
        try {
            val file = File(fileName)
            val outputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))

            // Write version marker for future compatibility
            outputStream.writeInt(1) // Version 1 of the file format

            // Write grid size
            outputStream.writeInt(gridEditor.grid.size)

            // Write each cell in the grid
            gridEditor.grid.forEach { (position, cell) ->
                // Write position
                outputStream.writeInt(position.first)  // x coordinate
                outputStream.writeInt(position.second) // y coordinate

                // Write number of floors containing objects in this cell
                outputStream.writeInt(cell.objectsByFloor.size)

                // For each floor in this cell
                cell.objectsByFloor.forEach { (floor, objects) ->
                    // Write floor level
                    outputStream.writeInt(floor)

                    // Write number of objects on this floor
                    outputStream.writeInt(objects.size)

                    // Write each object
                    objects.forEach { gameObject ->
                        // Write object type enum ordinal
                        outputStream.writeInt(gameObject.type.ordinal)

                        // Write common GameObject properties
                        writeColor(outputStream, gameObject.color)
                        outputStream.writeDouble(gameObject.height)
                        outputStream.writeDouble(gameObject.width)
                        outputStream.writeInt(gameObject.direction.ordinal)

                        // Write texture name and path (if exists)
                        val hasTexture = gameObject.texture != null
                        outputStream.writeBoolean(hasTexture)

                        if (hasTexture) {
                            val texture = gameObject.texture!!
                            outputStream.writeUTF(texture.name)
                            outputStream.writeUTF(texture.path)
                        }

                        // Write specific object type properties
                        when (gameObject) {
                            is WallObject -> {
                                outputStream.writeBoolean(gameObject.isBlockWall)
                                outputStream.writeDouble(gameObject.floorHeight)
                            }
                            is FloorObject -> {
                                outputStream.writeDouble(gameObject.floorHeight)
                            }
                            is PlayerSpawnObject -> {
                                // No additional properties for player spawn
                            }
                            is PillarObject -> {
                                outputStream.writeDouble(gameObject.floorHeight)
                            }
                            is WaterObject -> {
                                outputStream.writeDouble(gameObject.floorHeight)
                                outputStream.writeDouble(gameObject.depth)
                                outputStream.writeDouble(gameObject.waveHeight)
                                outputStream.writeDouble(gameObject.waveSpeed)
                                outputStream.writeDouble(gameObject.damagePerSecond)
                            }
                            is RampObject -> {
                                outputStream.writeDouble(gameObject.floorHeight)
                                outputStream.writeDouble(gameObject.rampHeight)
                                outputStream.writeInt(gameObject.slopeDirection.ordinal)
                            }
                        }
                    }
                }
            }

            // Write editor settings
            outputStream.writeInt(gridEditor.useCurrentFloor())
            outputStream.writeDouble(gridEditor.currentFloorHeight)
            outputStream.writeBoolean(gridEditor.useBlockWalls)

            outputStream.close()
            return true
        } catch (e: Exception) {
            println("Error saving world: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Loads a world from a file
     * @param gridEditor The grid editor to load the data into
     * @param fileName The name of the file to load from
     * @return True if load was successful, false otherwise
     */
    fun loadWorld(gridEditor: GridEditor, fileName: String): Boolean {
        try {
            val file = File(fileName)
            if (!file.exists()) {
                println("Save file does not exist: $fileName")
                return false
            }

            val inputStream = DataInputStream(BufferedInputStream(FileInputStream(file)))

            // Read and check version
            val version = inputStream.readInt()
            if (version != 1) {
                println("Unsupported save file version: $version")
                inputStream.close()
                return false
            }

            // Clear existing grid
            gridEditor.grid.clear()

            // Track all floor levels found during loading
            val floorLevels = mutableSetOf<Int>()

            // Read grid size
            val gridSize = inputStream.readInt()

            // Read each cell
            for (i in 0 until gridSize) {
                // Read position
                val x = inputStream.readInt()
                val y = inputStream.readInt()
                val position = Pair(x, y)

                // Create new cell
                val cell = GridCell()

                // Read number of floors
                val floorCount = inputStream.readInt()

                // For each floor
                for (j in 0 until floorCount) {
                    // Read floor level
                    val floor = inputStream.readInt()
                    // Add floor level to tracking set
                    floorLevels.add(floor)

                    // Read number of objects
                    val objectCount = inputStream.readInt()

                    // For each object
                    for (k in 0 until objectCount) {
                        // Read object type
                        val objectTypeOrdinal = inputStream.readInt()
                        val objectType = ObjectType.entries[objectTypeOrdinal]

                        // Read common properties
                        val color = readColor(inputStream)
                        val height = inputStream.readDouble()
                        val width = inputStream.readDouble()
                        val directionOrdinal = inputStream.readInt()
                        val direction = Direction.entries[directionOrdinal]

                        // Read texture
                        val hasTexture = inputStream.readBoolean()
                        var texture: ImageEntry? = null

                        if (hasTexture) {
                            val textureName = inputStream.readUTF()
                            val texturePath = inputStream.readUTF()

                            // First try to find the texture by name in the resource manager
                            texture = gridEditor.resourceManager?.getImageByName(textureName)

                            // If not found and the path exists, try to load it directly
                            if (texture == null) {
                                val file = File(texturePath)
                                if (file.exists()) {
                                    val image = javax.imageio.ImageIO.read(file)
                                    if (image != null) {
                                        // Add the texture to resource manager
                                        texture = ImageEntry(textureName, texturePath, image)
                                        gridEditor.resourceManager?.addImage(textureName, texturePath, image)
                                    }
                                }
                            }
                        }

                        // Create the appropriate GameObject based on type
                        val gameObject = when (objectType) {
                            ObjectType.WALL -> {
                                val isBlockWall = inputStream.readBoolean()
                                val floorHeight = inputStream.readDouble()
                                WallObject(color, height, width, direction, texture, isBlockWall, floorHeight)
                            }
                            ObjectType.FLOOR -> {
                                val floorHeight = inputStream.readDouble()
                                FloorObject(color, floorHeight, texture)
                            }
                            ObjectType.PLAYER_SPAWN -> {
                                PlayerSpawnObject(color, height, width, direction)
                            }
                            ObjectType.PILLAR -> {
                                val floorHeight = inputStream.readDouble()
                                PillarObject(color, height, width, direction, texture, floorHeight)
                            }
                            ObjectType.WATER -> {
                                val floorHeight = inputStream.readDouble()
                                val depth = inputStream.readDouble()
                                val waveHeight = inputStream.readDouble()
                                val waveSpeed = inputStream.readDouble()
                                val damagePerSecond = inputStream.readDouble()
                                WaterObject(color, floorHeight, depth, waveHeight, waveSpeed, damagePerSecond, texture)
                            }
                            ObjectType.RAMP -> {
                                val floorHeight = inputStream.readDouble()
                                val rampHeight = inputStream.readDouble()
                                val slopeDirectionOrdinal = inputStream.readInt()
                                val slopeDirection = Direction.entries[slopeDirectionOrdinal]
                                RampObject(color, floorHeight, rampHeight, slopeDirection, texture)
                            }
                            else -> {
                                // Default for unknown types
                                PlayerSpawnObject(color, height, width, direction)
                            }
                        }

                        // Add object to the cell
                        cell.addObject(floor, gameObject)
                    }
                }

                // Add cell to grid
                gridEditor.grid[position] = cell
            }

            // Read editor settings
            val currentFloor = inputStream.readInt()
            val currentFloorHeight = inputStream.readDouble()
            val useBlockWalls = inputStream.readBoolean()

            // Apply editor settings
            gridEditor.changeCurrentFloor(currentFloor)
            gridEditor.updateCurrentFloorHeight(currentFloorHeight)
            gridEditor.useBlockWalls = useBlockWalls

            inputStream.close()

            // Store the floor levels for menu system to use
            gridEditor.setDiscoveredFloors(floorLevels)

            // Notify that grid has changed
            gridEditor.notifyGridChanged()

            return true
        } catch (e: Exception) {
            println("Error loading world: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Helper function to write a Color to DataOutputStream
     */
    private fun writeColor(out: DataOutputStream, color: Color) {
        out.writeInt(color.red)
        out.writeInt(color.green)
        out.writeInt(color.blue)
        out.writeInt(color.alpha)
    }

    /**
     * Helper function to read a Color from DataInputStream
     */
    private fun readColor(input: DataInputStream): Color {
        val red = input.readInt()
        val green = input.readInt()
        val blue = input.readInt()
        val alpha = input.readInt()
        return Color(red, green, blue, alpha)
    }
}