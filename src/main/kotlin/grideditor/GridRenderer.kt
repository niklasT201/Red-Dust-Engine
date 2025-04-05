package grideditor

import FloorObject
import PillarObject
import PlayerSpawnObject
import RampObject
import WallObject
import WaterObject
import java.awt.*
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Handles all rendering operations for the grid editor
 */
class GridRenderer(private val editor: GridEditor) {

    fun screenToGrid(screenX: Int, screenY: Int): Pair<Int, Int> {
        val gridX = floor((screenX / editor.cellSize + editor.viewportX - editor.width / (2 * editor.cellSize))).toInt()
        val gridY = floor((screenY / editor.cellSize + editor.viewportY - editor.height / (2 * editor.cellSize))).toInt()
        return Pair(gridX, gridY)
    }

    fun gridToScreen(gridX: Int, gridY: Int): Pair<Int, Int> {
        val screenX = ((gridX - editor.viewportX + editor.width / (2 * editor.cellSize)) * editor.cellSize).toInt()
        val screenY = ((gridY - editor.viewportY + editor.height / (2 * editor.cellSize)) * editor.cellSize).toInt()
        return Pair(screenX, screenY)
    }

    fun render(g2: Graphics2D) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Calculate visible grid bounds
        val (minX, minY) = screenToGrid(0, 0)
        val (maxX, maxY) = screenToGrid(editor.width, editor.height)

        // Draw grid and cells
        for (x in (minX - editor.visibleCellPadding)..(maxX + editor.visibleCellPadding)) {
            for (y in (minY - editor.visibleCellPadding)..(maxY + editor.visibleCellPadding)) {
                val (screenX, screenY) = gridToScreen(x, y)

                // Get objects only for current floor
                val cell = editor.grid[Pair(x, y)]
                val floorObjects = cell?.getObjectsForFloor(editor.useCurrentFloor())
                val wallObject = floorObjects?.firstOrNull { it.type == ObjectType.WALL } as? WallObject
                val floorObject = floorObjects?.firstOrNull { it.type == ObjectType.FLOOR } as? FloorObject
                val playerSpawnObject = floorObjects?.firstOrNull { it.type == ObjectType.PLAYER_SPAWN } as? PlayerSpawnObject
                val pillarObject = floorObjects?.firstOrNull { it.type == ObjectType.PILLAR } as? PillarObject
                val waterObject = floorObjects?.firstOrNull { it.type == ObjectType.WATER } as? WaterObject
                val rampObject = floorObjects?.firstOrNull { it.type == ObjectType.RAMP } as? RampObject

                // Draw floor or background
                if (floorObject != null) {
                    if (floorObject.texture != null) {
                        // Draw floor texture
                        val texture = floorObject.texture.image
                        g2.drawImage(
                            texture,
                            screenX,
                            screenY,
                            editor.cellSize.toInt(),
                            editor.cellSize.toInt(),
                            null
                        )
                    } else {
                        // Fall back to color if no texture
                        g2.color = floorObject.color
                        g2.fillRect(
                            screenX,
                            screenY,
                            editor.cellSize.toInt(),
                            editor.cellSize.toInt()
                        )
                    }
                } else {
                    // Background color for empty cells
                    g2.color = Color(40, 44, 52)
                    g2.fillRect(
                        screenX,
                        screenY,
                        editor.cellSize.toInt(),
                        editor.cellSize.toInt()
                    )
                }

                // Draw direction indicators for walls
                if (wallObject != null) {
                    if (wallObject.isBlockWall || !editor.showFlatWallsAsLines) {
                        // Block walls - draw full cell with texture or color
                        if (wallObject.texture != null) {
                            // Draw wall texture
                            val texture = wallObject.texture.image
                            g2.drawImage(
                                texture,
                                screenX,
                                screenY,
                                editor.cellSize.toInt(),
                                editor.cellSize.toInt(),
                                null
                            )
                        } else {
                            // Fall back to color if no texture
                            g2.color = wallObject.color
                            g2.fillRect(
                                screenX,
                                screenY,
                                editor.cellSize.toInt(),
                                editor.cellSize.toInt()
                            )
                        }
                    } else {
                        // Flat walls - draw a line with a gradient or solid color
                        if (wallObject.texture != null) {
                            // For flat walls, we could draw a small strip of the texture
                            g2.color = wallObject.color  // Use color as fallback for line visual
                            g2.stroke = BasicStroke((editor.cellSize * 0.2).toFloat())

                            val padding = editor.cellSize * 0.1
                            when (wallObject.direction) {
                                Direction.NORTH -> {
                                    // Could create a texture strip here if needed
                                    g2.drawLine(
                                        screenX + padding.toInt(),
                                        screenY + padding.toInt(),
                                        screenX + editor.cellSize.toInt() - padding.toInt(),
                                        screenY + padding.toInt()
                                    )
                                }
                                Direction.SOUTH -> {
                                    g2.drawLine(
                                        screenX + padding.toInt(),
                                        screenY + editor.cellSize.toInt() - padding.toInt(),
                                        screenX + editor.cellSize.toInt() - padding.toInt(),
                                        screenY + editor.cellSize.toInt() - padding.toInt()
                                    )
                                }
                                Direction.WEST -> {
                                    g2.drawLine(
                                        screenX + padding.toInt(),
                                        screenY + padding.toInt(),
                                        screenX + padding.toInt(),
                                        screenY + editor.cellSize.toInt() - padding.toInt()
                                    )
                                }
                                Direction.EAST -> {
                                    g2.drawLine(
                                        screenX + editor.cellSize.toInt() - padding.toInt(),
                                        screenY + padding.toInt(),
                                        screenX + editor.cellSize.toInt() - padding.toInt(),
                                        screenY + editor.cellSize.toInt() - padding.toInt()
                                    )
                                }
                            }
                        } else {
                            // Draw line for flat wall
                            g2.color = wallObject.color
                            g2.stroke = BasicStroke((editor.cellSize * 0.2).toFloat())

                            val padding = editor.cellSize * 0.1
                            when (wallObject.direction) {
                                Direction.NORTH -> g2.drawLine(
                                    screenX + padding.toInt(),
                                    screenY + padding.toInt(),
                                    screenX + editor.cellSize.toInt() - padding.toInt(),
                                    screenY + padding.toInt()
                                )
                                Direction.SOUTH -> g2.drawLine(
                                    screenX + padding.toInt(),
                                    screenY + editor.cellSize.toInt() - padding.toInt(),
                                    screenX + editor.cellSize.toInt() - padding.toInt(),
                                    screenY + editor.cellSize.toInt() - padding.toInt()
                                )
                                Direction.WEST -> g2.drawLine(
                                    screenX + padding.toInt(),
                                    screenY + padding.toInt(),
                                    screenX + padding.toInt(),
                                    screenY + editor.cellSize.toInt() - padding.toInt()
                                )
                                Direction.EAST -> g2.drawLine(
                                    screenX + editor.cellSize.toInt() - padding.toInt(),
                                    screenY + padding.toInt(),
                                    screenX + editor.cellSize.toInt() - padding.toInt(),
                                    screenY + editor.cellSize.toInt() - padding.toInt()
                                )
                            }
                        }
                    }
                }

                if (pillarObject != null) {
                    val insetRatio = 0.15 // Make it visually smaller than a full block wall cell
                    val insetPixels = (editor.cellSize * insetRatio).toInt()
                    val pillarScreenX = screenX + insetPixels
                    val pillarScreenY = screenY + insetPixels
                    val pillarScreenSize = (editor.cellSize - 2 * insetPixels).toInt().coerceAtLeast(1) // Ensure size > 0

                    if (pillarObject.texture != null) {
                        val texture = pillarObject.texture.image // Use original image or scaled if needed
                        g2.drawImage(texture, pillarScreenX, pillarScreenY, pillarScreenSize, pillarScreenSize, null)
                    } else {
                        g2.color = pillarObject.color
                        g2.fillRect(pillarScreenX, pillarScreenY, pillarScreenSize, pillarScreenSize)
                    }

                    g2.color = Color.DARK_GRAY // Border color
                    g2.stroke = BasicStroke(1f)
                    g2.drawRect(pillarScreenX, pillarScreenY, pillarScreenSize, pillarScreenSize)

                    // Optional: Draw 'P' symbol
                    g2.color = Color.WHITE
                    val fontSize = (pillarScreenSize * 0.6).toInt().coerceAtLeast(8)
                    g2.font = Font("Monospace", Font.BOLD, fontSize)
                    val metrics = g2.fontMetrics
                    val letter = "P"
                    val letterX = pillarScreenX + (pillarScreenSize - metrics.stringWidth(letter)) / 2
                    val letterY = pillarScreenY + (pillarScreenSize - metrics.ascent + metrics.descent) / 2 + metrics.ascent
                    g2.drawString(letter, letterX, letterY)

                    g2.stroke = BasicStroke(1f) // Reset stroke
                }

                if (waterObject != null) {
                    if (waterObject.texture != null) {
                        // Draw water texture (similar to floor)
                        val texture = waterObject.texture.image
                        g2.drawImage(texture, screenX, screenY, editor.cellSize.toInt(), editor.cellSize.toInt(), null)
                    } else {
                        g2.color = waterObject.color
                        g2.fillRect(screenX, screenY, editor.cellSize.toInt(), editor.cellSize.toInt())
                    }
                    // Optional: Draw a subtle wave pattern
                    g2.color = Color.WHITE // Example: Draw 'W'
                    val fontSize = (editor.cellSize * 0.4).toInt().coerceAtLeast(8)
                    g2.font = Font("Monospace", Font.PLAIN, fontSize)
                    val metrics = g2.fontMetrics
                    val letter = "~" // Use '~' or 'W'
                    val letterX = screenX + (editor.cellSize - metrics.stringWidth(letter)) / 2
                    val letterY = screenY + (editor.cellSize - metrics.ascent + metrics.descent) / 2 + metrics.ascent
                    g2.drawString(letter, letterX.toInt(), letterY.toInt())

                    g2.font = Font("Monospace", Font.BOLD, 12) // Reset font if needed elsewhere
                }

                if (rampObject != null) {
                    // 1. Draw Base (Texture or Color)
                    if (rampObject.texture != null) {
                        val texture = rampObject.texture.image
                        g2.drawImage(texture, screenX, screenY, editor.cellSize.toInt(), editor.cellSize.toInt(), null)
                    } else {
                        g2.color = rampObject.color
                        g2.fillRect(screenX, screenY, editor.cellSize.toInt(), editor.cellSize.toInt())
                    }

                    // 2. Draw Slope Indicator
                    g2.color = Color.WHITE // Or a contrasting color
                    g2.stroke = BasicStroke(2f)
                    val centerX = screenX + editor.cellSize / 2
                    val centerY = screenY + editor.cellSize / 2
                    val arrowLength = editor.cellSize * 0.3

                    when (rampObject.slopeDirection) {
                        Direction.NORTH -> { // Arrow pointing UP (towards +Z)
                            g2.drawLine(centerX.toInt(), (centerY + arrowLength).toInt(), centerX.toInt(), (centerY - arrowLength).toInt())
                            g2.drawLine(centerX.toInt(), (centerY - arrowLength).toInt(), (centerX - arrowLength / 2).toInt(), centerY.toInt())
                            g2.drawLine(centerX.toInt(), (centerY - arrowLength).toInt(), (centerX + arrowLength / 2).toInt(), centerY.toInt())
                        }
                        Direction.SOUTH -> { // Arrow pointing DOWN (towards -Z)
                            g2.drawLine(centerX.toInt(), (centerY - arrowLength).toInt(), centerX.toInt(), (centerY + arrowLength).toInt())
                            g2.drawLine(centerX.toInt(), (centerY + arrowLength).toInt(), (centerX - arrowLength / 2).toInt(), centerY.toInt())
                            g2.drawLine(centerX.toInt(), (centerY + arrowLength).toInt(), (centerX + arrowLength / 2).toInt(), centerY.toInt())
                        }
                        Direction.EAST -> { // Arrow pointing RIGHT (towards +X)
                            g2.drawLine((centerX - arrowLength).toInt(), centerY.toInt(), (centerX + arrowLength).toInt(), centerY.toInt())
                            g2.drawLine((centerX + arrowLength).toInt(), centerY.toInt(), centerX.toInt(), (centerY - arrowLength / 2).toInt())
                            g2.drawLine((centerX + arrowLength).toInt(), centerY.toInt(), centerX.toInt(), (centerY + arrowLength / 2).toInt())
                        }
                        Direction.WEST -> { // Arrow pointing LEFT (towards -X)
                            g2.drawLine((centerX + arrowLength).toInt(), centerY.toInt(), (centerX - arrowLength).toInt(), centerY.toInt())
                            g2.drawLine((centerX - arrowLength).toInt(), centerY.toInt(), centerX.toInt(), (centerY - arrowLength / 2).toInt())
                            g2.drawLine((centerX - arrowLength).toInt(), centerY.toInt(), centerX.toInt(), (centerY + arrowLength / 2).toInt())
                        }
                    }
                    g2.stroke = BasicStroke(1f) // Reset stroke
                }

                // Draw grid lines
                g2.color = Color(60, 63, 65)
                g2.stroke = BasicStroke(1f) // Reset stroke to default
                g2.drawRect(
                    screenX,
                    screenY,
                    editor.cellSize.toInt(),
                    editor.cellSize.toInt()
                )
            }
        }

        // Draw selection highlight
        editor.selectedCell?.let { (x, y) ->
            val (screenX, screenY) = gridToScreen(x, y)
            g2.color = Color(255, 255, 0, 100) // Semi-transparent yellow
            g2.stroke = BasicStroke(2f)
            g2.drawRect(
                screenX,
                screenY,
                editor.cellSize.toInt(),
                editor.cellSize.toInt()
            )
        }

        if (editor.labelVisibility["mode"] == true) {
            g2.color = Color.WHITE
            g2.font = Font("Monospace", Font.BOLD, 12)
            g2.drawString(
                "Mode: ${editor.currentObjectType.name}",
                10, // X position for the text
                15  // Y position for the text
            )
        }

        // Draw direction text
        if (editor.labelVisibility["direction"] == true) {
            g2.color = Color.WHITE // Set text color to white
            g2.font = Font("Monospace", Font.BOLD, 12)
            g2.drawString(
                editor.currentDirection.name,
                10, // X position for the text
                30 // Y position for the text
            )
        }

        // Texture name display
        if (editor.labelVisibility["texture"] == true) {
            g2.color = Color.WHITE // Add this line
            g2.font = Font("Monospace", Font.BOLD, 12) // Ensure font is set
            val textureName = when(editor.currentObjectType) {
                ObjectType.WALL -> editor.currentWallTexture?.name ?: "None"
                ObjectType.FLOOR -> editor.currentFloorTexture?.name ?: "None"
                ObjectType.PILLAR -> editor.currentWallTexture?.name ?: "None"
                else -> "None"
            }
            g2.drawString(
                "Current ${editor.currentObjectType.name} Image: $textureName",
                10,
                45
            )
        }

        // Stats label
        if (editor.labelVisibility["stats"] == true) {
            g2.color = Color.WHITE // Add this line
            g2.font = Font("Monospace", Font.BOLD, 12) // Ensure font is set
            val statsText = StringBuilder("Objects: ")
            editor.objectStats.forEach { (type, count) ->
                statsText.append("${type.name}: $count, ")
            }
            // Trim the last comma and space
            if (statsText.length > 9) {
                statsText.setLength(statsText.length - 2)
            }
            g2.drawString(statsText.toString(), 10, 60)
        }

        // Player position label
        if (editor.labelVisibility["player"] == true) {
            g2.color = Color.WHITE // Add this line
            g2.font = Font("Monospace", Font.BOLD, 12) // Ensure font is set
            editor.cameraRef?.let { camera ->
                g2.drawString(
                    "Player: (${String.format("%.2f", camera.position.x)}, " +
                            "${String.format("%.2f", camera.position.y)}, " +
                            "${String.format("%.2f", camera.position.z)})",
                    10,
                    75
                )
            }
        }

        // Draw direction letters on wall tiles
        editor.grid.forEach { (pos, cell) ->
            cell.getObjectsForFloor(editor.useCurrentFloor()).filterIsInstance<WallObject>().firstOrNull()?.let { wallObject ->
                if (!wallObject.isBlockWall) {
                    val (x, y) = pos
                    val (screenX, screenY) = gridToScreen(x, y)

                    // Draw direction letter
                    g2.color = Color.WHITE
                    g2.font = Font("Monospace", Font.BOLD, (editor.cellSize * 0.4).toInt())
                    val letter = wallObject.direction.name.first().toString()
                    val metrics = g2.fontMetrics
                    val letterX = screenX + (editor.cellSize - metrics.stringWidth(letter)) / 2
                    val letterY = screenY + (editor.cellSize + metrics.height) / 2 - metrics.descent
                    g2.drawString(letter, letterX.toInt(), letterY.toInt())
                }
            }
        }

        // Draw player if camera reference exists
        editor.cameraRef?.let { camera ->
            // Convert world coordinates to grid coordinates
            val (gridX, gridZ) = editor.worldToGrid(camera.position.x, camera.position.z)
            val (screenX, screenY) = gridToScreen(floor(gridX).toInt(), floor(gridZ).toInt())

            // Calculate player position within the cell
            val xOffset = ((gridX % 1) * editor.cellSize).toInt()
            val yOffset = ((gridZ % 1) * editor.cellSize).toInt()

            // Draw player body (green circle)
            g2.color = Color(0, 255, 0)
            val playerSize = (editor.cellSize * 0.3).toInt()
            g2.fillOval(
                screenX + xOffset - playerSize / 2,
                screenY + yOffset - playerSize / 2,
                playerSize,
                playerSize
            )

            // Draw direction indicator (line)
            val lineLength = editor.cellSize * 0.5
            val dirX = sin(camera.yaw) * lineLength
            val dirZ = cos(camera.yaw) * lineLength
            g2.drawLine(
                screenX + xOffset,
                screenY + yOffset,
                screenX + xOffset + dirX.toInt(),
                screenY + yOffset + dirZ.toInt()
            )
        }
    }
}