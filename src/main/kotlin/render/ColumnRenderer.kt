package render

import ImageEntry
import Vec3
import Wall
import player.Camera
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.math.*

/**
 * A column-based renderer that uses raycasting techniques similar to those used in classic
 * FPS games like Wolfenstein 3D and DOOM. This approach is particularly efficient for
 * rendering vertical structures like walls and pillars.
 */
class ColumnRenderer(
    private var width: Int,
    private var height: Int,
    private val nearPlane: Double,
    private val farPlane: Double
) {
    // Field of view in radians (typically PI/3 for 60 degrees)
    private var fov = PI/3
    private var halfFov = fov / 2
    private var projectionPlaneWidth = 2.0 * tan(halfFov)

    // Precomputed angle for each column
    private var columnAngles = DoubleArray(width)

    // Cache for performance
    private val textureCache = mutableMapOf<String, Array<IntArray>>()

    // Shadow properties (synced with main renderer)
    var enableShadows = true
    var shadowDistance = 20.0
    var shadowIntensity = 0.7
    var ambientLight = 0.3
    var shadowColor = Color.BLACK

    init {
        updateColumnAngles()
    }

    /**
     * Updates internal dimensions and recalculates column angles
     */
    fun updateDimensions(newWidth: Int, newHeight: Int) {
        width = newWidth
        height = newHeight
        updateColumnAngles()
    }

    /**
     * Updates the field of view and related calculations
     */
    fun setFov(value: Double) {
        fov = value
        halfFov = fov / 2
        projectionPlaneWidth = 2.0 * tan(halfFov)
        updateColumnAngles()
    }

    /**
     * Precomputes the view angle for each screen column
     */
    private fun updateColumnAngles() {
        columnAngles = DoubleArray(width)
        for (x in 0 until width) {
            // Convert screen x to normalized device coordinate (-1 to 1)
            val normalizedX = 2.0 * x / width - 1.0
            // Calculate angle for this column
            columnAngles[x] = atan(normalizedX * tan(halfFov))
        }
    }

    /**
     * Renders a vertical structure (wall or pillar) using column-based rendering
     */
    fun renderVerticalObject(
        g2: Graphics2D,
        wall: Wall,
        camera: Camera,
        isTextured: Boolean = true
    ) {
        // Transform wall endpoints to camera space
        val startPos = transformToCameraSpace(wall.start, camera)
        val endPos = transformToCameraSpace(wall.end, camera)

        // Skip if wall is behind the camera
        if (startPos.z < nearPlane && endPos.z < nearPlane) return

        // Prepare texture if available
        val texture = if (isTextured && wall.texture != null)
            getProcessedTexture(wall.texture) else null

        // Calculate wall direction vector in camera space
        val wallDirX = endPos.x - startPos.x
        val wallDirZ = endPos.z - startPos.z
        val wallLength = sqrt(wallDirX * wallDirX + wallDirZ * wallDirZ)

        // For each screen column
        for (screenX in 0 until width) {
            // Calculate ray direction for this column
            val rayDirX = cos(camera.yaw + columnAngles[screenX])
            val rayDirZ = sin(camera.yaw + columnAngles[screenX])

            // Calculate ray intersection with wall line segment
            val intersection = calculateRayWallIntersection(
                camera.position.x, camera.position.z,
                rayDirX, rayDirZ,
                wall.start.x, wall.start.z,
                wall.end.x, wall.end.z
            )

            // If ray intersects with wall
            if (intersection != null) {
                val (intersectX, intersectZ, wallT) = intersection

                // Calculate perpendicular distance to prevent fisheye effect
                // This is the key to getting the correct perspective view
                val perpDistance = calculatePerpDistance(
                    camera.position.x, camera.position.z,
                    intersectX, intersectZ,
                    camera.yaw + columnAngles[screenX]
                )

                // Skip if too far or too close
                if (perpDistance <= nearPlane || perpDistance >= farPlane) continue

                // Calculate wall height on screen
                val wallHeight = wall.height
                val screenWallHeight = (height * wallHeight) / perpDistance

                // Calculate vertical positions on screen
                val centerY = height / 2
                val verticalOffset = (camera.position.y - wall.start.y) / perpDistance * height
                val wallTop = centerY - screenWallHeight / 2 + verticalOffset
                val wallBottom = centerY + screenWallHeight / 2 + verticalOffset

                // Calculate texture coordinates
                val texU = wallT // Normalized position along wall (0.0 to 1.0)

                // Calculate shading factor based on distance
                val shadowFactor = if (enableShadows) {
                    calculateShadowFactor(perpDistance)
                } else {
                    1.0
                }

                // Render the column
                if (texture != null && isTextured) {
                    // Textured rendering
                    drawTexturedColumn(
                        g2, screenX,
                        wallTop.toInt(), wallBottom.toInt(),
                        texU, texture, shadowFactor
                    )
                } else {
                    // Solid color rendering
                    val shadedColor = applyShadowToColor(wall.color, shadowFactor)
                    g2.color = shadedColor
                    g2.drawLine(screenX, wallTop.toInt(), screenX, wallBottom.toInt())
                }
            }
        }
    }

    /**
     * Renders a pillar (vertical cylinder) using column-based rendering
     */
    fun renderPillar(
        g2: Graphics2D,
        centerX: Double, centerZ: Double,
        radius: Double, height: Double,
        baseY: Double, color: Color,
        texture: ImageEntry?,
        camera: Camera
    ) {
        // Transform pillar center to camera space
        val center = transformToCameraSpace(Vec3(centerX, baseY, centerZ), camera)

        // Skip if center is behind the camera
        if (center.z < nearPlane) return

        // Process texture if available
        val processedTexture = if (texture != null)
            getProcessedTexture(texture) else null

        // For each screen column
        for (screenX in 0 until width) {
            // Calculate ray angle and direction
            val rayAngle = camera.yaw + columnAngles[screenX]
            val rayDirX = cos(rayAngle)
            val rayDirZ = sin(rayAngle)

            // Calculate distance to pillar center along ray direction
            val toCenterX = centerX - camera.position.x
            val toCenterZ = centerZ - camera.position.z

            // Project center onto ray
            val rayProjection = toCenterX * rayDirX + toCenterZ * rayDirZ

            // Calculate closest point on ray to pillar center
            val closestX = camera.position.x + rayDirX * rayProjection
            val closestZ = camera.position.z + rayDirZ * rayProjection

            // Calculate distance from closest point to center
            val distToCenter = sqrt(
                (closestX - centerX).pow(2) +
                        (closestZ - centerZ).pow(2)
            )

            // If ray hits pillar
            if (distToCenter <= radius) {
                // Calculate distance to pillar surface along ray
                val halfChordLength = sqrt(radius * radius - distToCenter * distToCenter)
                val surfaceDistance = rayProjection - halfChordLength

                // Skip if pillar is behind the camera or too far
                if (surfaceDistance < nearPlane || surfaceDistance > farPlane) continue

                // Calculate pillar height on screen
                val screenPillarHeight = (height * height) / surfaceDistance

                // Calculate vertical positions on screen
                val centerY = this.height / 2
                val verticalOffset = (camera.position.y - baseY) / surfaceDistance * this.height
                val pillarTop = centerY - screenPillarHeight / 2 + verticalOffset
                val pillarBottom = centerY + screenPillarHeight / 2 + verticalOffset

                // Calculate texture coordinate based on angle around pillar
                val angle = atan2(closestZ - centerZ, closestX - centerX)
                val texU = (angle / (2 * PI) + 0.5) % 1.0

                // Calculate shading factor based on distance
                val shadowFactor = if (enableShadows) {
                    calculateShadowFactor(surfaceDistance)
                } else {
                    1.0
                }

                // Render the column
                if (processedTexture != null) {
                    // Textured rendering
                    drawTexturedColumn(
                        g2, screenX,
                        pillarTop.toInt(), pillarBottom.toInt(),
                        texU, processedTexture, shadowFactor
                    )
                } else {
                    // Solid color rendering
                    val shadedColor = applyShadowToColor(color, shadowFactor)
                    g2.color = shadedColor
                    g2.drawLine(screenX, pillarTop.toInt(), screenX, pillarBottom.toInt())
                }
            }
        }
    }

    /**
     * Transforms a point from world space to camera space
     */
    private fun transformToCameraSpace(point: Vec3, camera: Camera): Vec3 {
        // First translate point relative to camera position
        val dx = point.x - camera.position.x
        val dy = point.y - camera.position.y
        val dz = point.z - camera.position.z

        // Calculate view matrix components
        val cosYaw = cos(camera.yaw)
        val sinYaw = sin(camera.yaw)
        val cosPitch = cos(camera.pitch)
        val sinPitch = sin(camera.pitch)

        // Apply rotation to align with camera orientation
        return Vec3(
            dx * cosYaw + dz * sinYaw,
            dy * cosPitch - (dx * sinYaw - dz * cosYaw) * sinPitch,
            dx * -sinYaw + dz * cosYaw
        )
    }

    /**
     * Calculates the intersection of a ray with a wall line segment
     * Returns (intersectX, intersectZ, wallT) where wallT is normalized position along wall
     */
    private fun calculateRayWallIntersection(
        rayStartX: Double, rayStartZ: Double,
        rayDirX: Double, rayDirZ: Double,
        wallStartX: Double, wallStartZ: Double,
        wallEndX: Double, wallEndZ: Double
    ): Triple<Double, Double, Double>? {
        // Calculate wall direction
        val wallDirX = wallEndX - wallStartX
        val wallDirZ = wallEndZ - wallStartZ

        // Calculate determinant
        val det = -rayDirX * wallDirZ + rayDirZ * wallDirX

        // If determinant is close to 0, ray is parallel to wall
        if (abs(det) < 0.0001) return null

        // Calculate s and t parameters
        val s = (-wallDirZ * (rayStartX - wallStartX) + wallDirX * (rayStartZ - wallStartZ)) / det
        val t = (-rayDirZ * (rayStartX - wallStartX) + rayDirX * (rayStartZ - wallStartZ)) / det

        // Check if intersection is within wall bounds
        if (s >= 0 && t >= 0 && t <= 1) {
            // Calculate intersection point
            val intersectX = rayStartX + s * rayDirX
            val intersectZ = rayStartZ + s * rayDirZ
            return Triple(intersectX, intersectZ, t)
        }

        return null
    }

    /**
     * Calculates perpendicular distance to wall to avoid fisheye effect
     */
    private fun calculatePerpDistance(
        cameraX: Double, cameraZ: Double,
        intersectX: Double, intersectZ: Double,
        rayAngle: Double
    ): Double {
        // Calculate actual distance
        val dx = intersectX - cameraX
        val dz = intersectZ - cameraZ
        val directDistance = sqrt(dx * dx + dz * dz)

        // Calculate angle between ray and camera-to-intersection vector
        val intersectAngle = atan2(dz, dx)
        val angleDiff = normalizeAngle(intersectAngle - rayAngle)

        // Return perpendicular distance
        return directDistance * cos(angleDiff)
    }

    /**
     * Normalizes an angle to be between -PI and PI
     */
    private fun normalizeAngle(angle: Double): Double {
        var result = angle
        while (result > PI) result -= 2 * PI
        while (result < -PI) result += 2 * PI
        return result
    }

    /**
     * Draws a textured vertical column
     */
    private fun drawTexturedColumn(
        g2: Graphics2D,
        screenX: Int,
        y1: Int, y2: Int,
        texU: Double,
        texture: Array<IntArray>,
        shadowFactor: Double
    ) {
        val texWidth = texture.size
        val texHeight = texture[0].size

        // Calculate texture X coordinate
        val texX = (texU * texWidth).toInt() % texWidth

        // Ensure y values are within screen
        val startY = maxOf(0, y1)
        val endY = minOf(height - 1, y2)

        // Draw the textured column
        for (y in startY..endY) {
            // Map screen y to texture y
            val texY = ((y - y1) / (y2 - y1).toDouble() * texHeight).toInt() % texHeight

            // Get texture color at (texX, texY) and apply shadow
            val texColor = texture[texX][texY]
            val shadedColor = applyShadowToRgb(texColor, shadowFactor)

            // Set pixel
            g2.color = Color(shadedColor)
            g2.drawLine(screenX, y, screenX, y)
        }
    }

    /**
     * Process and cache texture for faster rendering
     */
    private fun getProcessedTexture(textureEntry: ImageEntry): Array<IntArray> {
        // Check cache first
        val cachedTexture = textureCache[textureEntry.name]
        if (cachedTexture != null) return cachedTexture

        // Process and cache if not found
        val image = textureEntry.image
        val width = image.getWidth(null)
        val height = image.getHeight(null)

        // Convert to BufferedImage if needed
        val bufferedImage = if (image is BufferedImage) {
            image
        } else {
            val buffer = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val g = buffer.graphics
            g.drawImage(image, 0, 0, null)
            g.dispose()
            buffer
        }

        // Read pixels into array
        val pixels = Array(width) { IntArray(height) }
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixels[x][y] = bufferedImage.getRGB(x, y)
            }
        }

        // Cache and return
        textureCache[textureEntry.name] = pixels
        return pixels
    }

    /**
     * Calculate shadow factor based on distance
     */
    private fun calculateShadowFactor(distance: Double): Double {
        val distanceFactor = minOf(distance / shadowDistance, 1.0)
        return maxOf(1.0 - (distanceFactor * shadowIntensity), ambientLight)
    }

    /**
     * Apply shadow to a color
     */
    private fun applyShadowToColor(color: Color, shadowFactor: Double): Color {
        val interpolationFactor = 1.0 - shadowFactor

        val shadedRed = minOf(255, (color.red * (1 - interpolationFactor) +
                shadowColor.red * interpolationFactor).toInt())
        val shadedGreen = minOf(255, (color.green * (1 - interpolationFactor) +
                shadowColor.green * interpolationFactor).toInt())
        val shadedBlue = minOf(255, (color.blue * (1 - interpolationFactor) +
                shadowColor.blue * interpolationFactor).toInt())

        return Color(shadedRed, shadedGreen, shadedBlue, color.alpha)
    }

    /**
     * Apply shadow to an RGB color value
     */
    private fun applyShadowToRgb(rgb: Int, shadowFactor: Double): Int {
        val a = (rgb shr 24) and 0xFF
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF

        val interpolationFactor = 1.0 - shadowFactor
        val shadowR = shadowColor.red
        val shadowG = shadowColor.green
        val shadowB = shadowColor.blue

        val shadedR = minOf(255, (r * (1 - interpolationFactor) +
                shadowR * interpolationFactor).toInt())
        val shadedG = minOf(255, (g * (1 - interpolationFactor) +
                shadowG * interpolationFactor).toInt())
        val shadedB = minOf(255, (b * (1 - interpolationFactor) +
                shadowB * interpolationFactor).toInt())

        return (a shl 24) or (shadedR shl 16) or (shadedG shl 8) or shadedB
    }
}