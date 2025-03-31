package render

import Vec3
import WaterSurface
import player.Camera
import kotlin.math.*

class WaterProcessor(
    private val nearPlane: Double,
    private val width: Int,
    private val height: Int,
    private val scale: Double
) {
    fun processWater(water: WaterSurface, camera: Camera, renderQueue: MutableList<RenderableObject>) {
        // Calculate if player is colliding with the water
        val isPlayerColliding = isPlayerInWater(water, camera)

        // Apply wave effect to water (customize as needed)
        val waveY = water.y + sin(System.currentTimeMillis() * water.waveSpeed / 1000.0) * water.waveHeight

        val corners = listOf(
            Vec3(water.x1 - 0.01, waveY, water.z1 - 0.01),
            Vec3(water.x2 + 0.01, waveY, water.z1 - 0.01),
            Vec3(water.x2 + 0.01, waveY, water.z2 + 0.01),
            Vec3(water.x1 - 0.01, waveY, water.z2 + 0.01)
        )

        // Determine if we're viewing the water from below
        val viewingFromBelow = (waveY > camera.position.y)

        // Create two sets of corners: in clockwise and counter-clockwise orders
        val orderedCorners = if (!viewingFromBelow) {
            // Default order for viewing from above (counter-clockwise)
            corners
        } else {
            // Reversed order for viewing from below (clockwise)
            listOf(corners[0], corners[3], corners[2], corners[1])
        }

        // Transform the correctly ordered corners
        val orderedTransformedCorners = orderedCorners.map { GeometryUtils.transformPoint(it, camera) }

        // Adjust texture coordinates based on viewing direction
        val texCoords = if (!viewingFromBelow) {
            orderedCorners.map { vertex ->
                val u = (vertex.x - water.x1) / (water.x2 - water.x1)
                val v = (vertex.z - water.z1) / (water.z2 - water.z1)
                // Add water texture animation here if desired
                // For example, offsetting based on time:
                val timeOffset = (System.currentTimeMillis() % 10000) / 10000.0 * 0.1
                Pair(u + timeOffset, v)
            }
        } else {
            // When viewing from below, we need to adjust the texture coordinates
            orderedCorners.map { vertex ->
                val u = (vertex.x - water.x1) / (water.x2 - water.x1)
                val v = (vertex.z - water.z1) / (water.z2 - water.z1)
                // Add water texture animation here if desired
                val timeOffset = (System.currentTimeMillis() % 10000) / 10000.0 * 0.1
                // Flip the v-coordinate when viewing from below
                Pair(u + timeOffset, 1.0 - v)
            }
        }

        // More robust near plane checking - if any point is in front of the near plane, process the water
        if (orderedTransformedCorners.none { it.z > nearPlane }) return

        // Clip the water polygon against the near plane if needed
        val (clippedCorners, clippedTexCoords) = GeometryUtils.clipPolygonToNearPlane(
            orderedTransformedCorners, texCoords, nearPlane
        )
        if (clippedCorners.isEmpty()) return

        val screenPoints = clippedCorners.map {
            GeometryUtils.projectPoint(it, nearPlane, width, height, scale)
        }

        // Calculate center point for distance sorting
        val centerX = (water.x1 + water.x2) / 2
        val centerY = waveY
        val centerZ = (water.z1 + water.z2) / 2

        // Calculate distance to camera for sorting
        val dx = centerX - camera.position.x
        val dy = centerY - camera.position.y
        val dz = centerZ - camera.position.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)

        // Adjust sorting distance for water when player is very close
        val adjustedDistance = if (abs(dy) < 0.5) {
            // If very close to the water, adjust distance to ensure proper sorting
            distance - 1.0
        } else {
            distance
        }

        renderQueue.add(
            RenderableObject.WaterInfo(
                adjustedDistance,
                screenPoints,
                water.color,
                water.texture,
                clippedTexCoords,
                water,
                viewingFromBelow,
                isPlayerColliding
            )
        )
    }

    private fun isPlayerInWater(water: WaterSurface, camera: Camera): Boolean {
        // Check if player position is within water bounds
        val isWithinXBounds = camera.position.x >= water.x1 && camera.position.x <= water.x2
        val isWithinZBounds = camera.position.z >= water.z1 && camera.position.z <= water.z2
        val isWithinYBounds = camera.position.y >= water.y - water.depth && camera.position.y <= water.y

        return isWithinXBounds && isWithinZBounds && isWithinYBounds
    }
}