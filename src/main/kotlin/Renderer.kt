import player.Camera
import render.FloorProcessor
import render.RenderableObject
import render.TextureRenderer
import render.WallProcessor
import java.awt.*
import kotlin.math.*

class Renderer(
    private var width: Int,
    private var height: Int
) {
    // Settings and properties
    private var fov = PI/3
    private var scale = 1.0 / tan(fov/2)
    private var nearPlane = 0.1
    private var farPlane = 100.0

    // Dimensions management
    fun updateDimensions(newWidth: Int, newHeight: Int) {
        this.width = newWidth
        this.height = newHeight
    }

    // Getters for the settings
    fun getFov(): Double = fov
    fun getScale(): Double = scale
    fun getNearPlane(): Double = nearPlane
    fun getFarPlane(): Double = farPlane

    // Setters for the settings
    fun setFov(value: Double) {
        fov = value
        // Recalculate the scale when FOV changes
        scale = 1.0 / tan(fov/2)
    }

    fun setScale(value: Double) {
        scale = value
    }

    fun setNearPlane(value: Double) {
        nearPlane = value
    }

    fun setFarPlane(value: Double) {
        farPlane = value
    }

    // Main scene drawing method
    fun drawScene(g2: Graphics2D, walls: List<Wall>, floors: List<Floor>, camera: Camera) {
        val renderQueue = mutableListOf<RenderableObject>()

        // Process floors
        for (floor in floors) {
            processFloor(floor, camera, renderQueue)
        }

        // Process walls
        for (wall in walls) {
            processWall(wall, camera, renderQueue)
        }

        // Sort all objects by distance (furthest first)
        renderQueue.sortByDescending { it.distance }

        // Draw all objects in sorted order
        for (renderable in renderQueue) {
            val polygon = Polygon(
                renderable.screenPoints.map { it.first }.toIntArray(),
                renderable.screenPoints.map { it.second }.toIntArray(),
                renderable.screenPoints.size
            )

            // Check if we have a texture for this object
            if (renderable.texture != null) {
                TextureRenderer.drawTexturedPolygon(
                    g2, polygon, renderable.texture!!,
                    renderable.textureCoords, renderable.screenPoints
                )
            } else {
                // Fall back to solid color if no texture
                g2.color = renderable.color
                g2.fill(polygon)
            }
        }
    }

    // Process a floor for rendering
    private fun processFloor(floor: Floor, camera: Camera, renderQueue: MutableList<RenderableObject>) {
        val floorProcessor = FloorProcessor(nearPlane, width, height, scale)
        floorProcessor.processFloor(floor, camera, renderQueue)
    }

    // Process a wall for rendering
    private fun processWall(wall: Wall, camera: Camera, renderQueue: MutableList<RenderableObject>) {
        val wallProcessor = WallProcessor(nearPlane, width, height, scale)
        wallProcessor.processWall(wall, camera, renderQueue)
    }
}