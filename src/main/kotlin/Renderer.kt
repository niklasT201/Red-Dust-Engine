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
    var enableRenderDistance = true
    var maxRenderDistance = 50.0

    // Border properties
    var borderColor = Color.BLACK
    var borderThickness = 2.0f
    var drawBorders = false

    // Distance-based shadows
    var enableShadows = true
    var shadowDistance = 20.0  // Distance at which maximum darkening occurs
    var shadowIntensity = 0.7  // How much objects darken at max distance (0.0-1.0)
    var ambientLight = 0.3     // Minimum light level (0.0-1.0)
    var shadowColor = Color.BLACK

    // Radius properties
    var enableVisibilityRadius = false
    var visibilityRadius = 50.0 // Distance within which objects are visible
    var visibilityFalloff = 1.0 // Width of the gradient edge (0 = hard edge, higher = more gradual falloff)
    var outsideColor = Color.BLACK // Color for areas outside visibility radius

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

        // Filter objects based on render distance (only if enabled)
        val filteredQueue = if (enableRenderDistance) {
            renderQueue.filter { it.distance <= maxRenderDistance }
        } else {
            renderQueue
        }

        // Sort all objects by distance (furthest first)
        val sortedQueue = filteredQueue.sortedByDescending { it.distance }

        // Store original rendering hints and stroke
        val originalAntialiasingHint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING)
        val originalStroke = g2.stroke
        val originalComposite = g2.composite

        // Set up anti-aliasing for smoother borders
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // If visibility radius is enabled, first draw the scene completely black
        if (enableVisibilityRadius && enableRenderDistance) {
            g2.color = outsideColor
            g2.fillRect(0, 0, width, height)
        }

        // Draw all objects in sorted order
        for (renderable in sortedQueue) {
            val polygon = Polygon(
                renderable.screenPoints.map { it.first }.toIntArray(),
                renderable.screenPoints.map { it.second }.toIntArray(),
                renderable.screenPoints.size
            )

            // Calculate shadow factor based on distance (1.0 = no shadow, closer to 0.0 = darker)
            val shadowFactor = if (enableShadows) {
                calculateShadowFactor(renderable.distance)
            } else {
                1.0 // No shadow if disabled
            }

            // Calculate visibility factor based on distance (for visibility radius feature)
            val visibilityFactor = if (enableVisibilityRadius && enableRenderDistance) {
                calculateVisibilityFactor(renderable.distance)
            } else {
                1.0 // Fully visible if visibility radius is disabled
            }

            // Skip rendering objects that are completely invisible (optimization)
            if (visibilityFactor <= 0.01) continue

            // Apply visibility factor using AlphaComposite for smooth blending
            if (enableVisibilityRadius && enableRenderDistance && visibilityFactor < 1.0) {
                g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, visibilityFactor.toFloat())
            }

            // Check if we have a texture for this object
            if (renderable.texture != null) {
                // Draw textured polygon with shadow factor
                TextureRenderer.drawTexturedPolygon(
                    g2, polygon, renderable.texture!!,
                    renderable.textureCoords, renderable.screenPoints,
                    shadowFactor
                )
            } else {
                // For solid colors, apply shadow directly to the color
                val shadedColor = if (enableShadows) {
                    applyShadowToColor(renderable.color, shadowFactor)
                } else {
                    renderable.color
                }

                g2.color = shadedColor
                g2.fill(polygon)
            }

            // Draw the border if enabled
            if (drawBorders) {
                // Set border stroke and color
                g2.stroke = BasicStroke(borderThickness)
                g2.color = borderColor
                g2.draw(polygon)
            }

            // Reset composite if it was changed
            if (enableVisibilityRadius && enableRenderDistance && visibilityFactor < 1.0) {
                g2.composite = originalComposite
            }
        }

        // Restore original rendering hints and stroke
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalAntialiasingHint)
        g2.stroke = originalStroke
        g2.composite = originalComposite
    }

    // Calculate shadow factor based on distance
    private fun calculateShadowFactor(distance: Double): Double {
        // Calculate how much light reaches the object (1.0 = full brightness, 0.0 = complete darkness)
        val distanceFactor = minOf(distance / shadowDistance, 1.0)

        // Apply shadow intensity and ensure we don't go below ambient light level
        return maxOf(1.0 - (distanceFactor * shadowIntensity), ambientLight)
    }

    // Calculate visibility factor based on distance (for visibility radius feature)
    private fun calculateVisibilityFactor(distance: Double): Double {
        // If distance is less than visibility radius - falloff, it's fully visible
        if (distance < visibilityRadius - visibilityFalloff) {
            return 1.0
        }

        // If distance is more than visibility radius, it's not visible
        if (distance > visibilityRadius) {
            return 0.0
        }

        // Otherwise, calculate a smooth falloff using the distance from the edge
        val edgeDistance = visibilityRadius - distance
        return edgeDistance / visibilityFalloff
    }

    // Apply shadow factor to a color
    private fun applyShadowToColor(color: Color, shadowFactor: Double): Color {
        // Interpolate between the original color and the shadow color based on shadow factor
        val interpolationFactor = 1.0 - shadowFactor

        val shadedRed = minOf(255, (color.red * (1 - interpolationFactor) + shadowColor.red * interpolationFactor).toInt())
        val shadedGreen = minOf(255, (color.green * (1 - interpolationFactor) + shadowColor.green * interpolationFactor).toInt())
        val shadedBlue = minOf(255, (color.blue * (1 - interpolationFactor) + shadowColor.blue * interpolationFactor).toInt())

        return Color(
            shadedRed,
            shadedGreen,
            shadedBlue,
            color.alpha
        )
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

    fun repaint() {
        // placeholder comment for now
    }
}