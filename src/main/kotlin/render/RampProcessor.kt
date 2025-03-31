package render

import Ramp
import player.Camera
import kotlin.math.*

class RampProcessor(
    private val nearPlane: Double,
    private val width: Int,
    private val height: Int,
    private val scale: Double
) {
    fun processRamp(ramp: Ramp, camera: Camera, renderQueue: MutableList<RenderableObject>) {
        // Get the 3D corners in the order they are defined in Ramp
        val corners3D = listOf(ramp.corner1, ramp.corner2, ramp.corner3, ramp.corner4)

        // Transform corners into camera space
        val transformedCorners = corners3D.map { GeometryUtils.transformPoint(it, camera) }

        val texCoords = listOf(
            Pair(0.0, 0.0), // Corresponds to corner1
            Pair(1.0, 0.0), // Corresponds to corner2
            Pair(1.0, 1.0), // Corresponds to corner3
            Pair(0.0, 1.0)  // Corresponds to corner4
        )

        // Check if the entire ramp is behind the near plane
        if (transformedCorners.all { it.z <= nearPlane }) {
            return // Don't render if completely behind
        }

        // Clip the ramp polygon against the near plane
        val (clippedCorners, clippedTexCoords) = GeometryUtils.clipPolygonToNearPlane(
            transformedCorners, texCoords, nearPlane
        )

        // If clipping resulted in no vertices, don't render
        if (clippedCorners.isEmpty() || clippedCorners.size < 3) {
            return
        }

        // Project the clipped 3D points to 2D screen coordinates
        val screenPoints = clippedCorners.map {
            GeometryUtils.projectPoint(it, nearPlane, width, height, scale)
        }

        // Calculate the center of the ramp for distance sorting
        // Average the original 3D corner positions
        val centerX = corners3D.sumOf { it.x } / corners3D.size
        val centerY = corners3D.sumOf { it.y } / corners3D.size
        val centerZ = corners3D.sumOf { it.z } / corners3D.size

        // Calculate distance from camera to the center of the ramp
        val dx = centerX - camera.position.x
        val dy = centerY - camera.position.y
        val dz = centerZ - camera.position.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)

        // Add the processed ramp to the render queue
        renderQueue.add(
            RenderableObject.RampInfo(
                distance = distance,
                screenPoints = screenPoints,
                color = ramp.color,
                texture = ramp.texture,
                textureCoords = clippedTexCoords, // Use clipped coordinates
                originalRamp = ramp
            )
        )
    }
}