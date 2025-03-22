import player.Camera
import kotlin.math.*

class FloorProcessor(
    private val nearPlane: Double,
    private val width: Int,
    private val height: Int,
    private val scale: Double
) {
    fun processFloor(floor: Floor, camera: Camera, renderQueue: MutableList<RenderableObject>) {
        val corners = listOf(
            Vec3(floor.x1 - 0.01, floor.y, floor.z1 - 0.01),
            Vec3(floor.x2 + 0.01, floor.y, floor.z1 - 0.01),
            Vec3(floor.x2 + 0.01, floor.y, floor.z2 + 0.01),
            Vec3(floor.x1 - 0.01, floor.y, floor.z2 + 0.01)
        )

        // Determine if we're viewing the floor from below (for texture orientation)
        val viewingFromBelow = (floor.y > camera.position.y)

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
                val u = (vertex.x - floor.x1) / (floor.x2 - floor.x1)
                val v = (vertex.z - floor.z1) / (floor.z2 - floor.z1)
                Pair(u, v)
            }
        } else {
            // When viewing from below, we need to adjust the texture coordinates
            // to prevent the texture from appearing mirrored
            orderedCorners.map { vertex ->
                val u = (vertex.x - floor.x1) / (floor.x2 - floor.x1)
                val v = (vertex.z - floor.z1) / (floor.z2 - floor.z1)
                // Flip the v-coordinate
                Pair(u, 1.0 - v)
            }
        }

        // More robust near plane checking - if any point is in front of the near plane, process the floor
        if (orderedTransformedCorners.none { it.z > nearPlane }) return

        // Clip the floor polygon against the near plane if needed
        val (clippedCorners, clippedTexCoords) = GeometryUtils.clipPolygonToNearPlane(
            orderedTransformedCorners, texCoords, nearPlane
        )
        if (clippedCorners.isEmpty()) return

        val screenPoints = clippedCorners.map {
            GeometryUtils.projectPoint(it, nearPlane, width, height, scale)
        }

        // Calculate center point for distance sorting
        val centerX = (floor.x1 + floor.x2) / 2
        val centerY = floor.y
        val centerZ = (floor.z1 + floor.z2) / 2

        // Calculate distance to camera for sorting
        val dx = centerX - camera.position.x
        val dy = centerY - camera.position.y
        val dz = centerZ - camera.position.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)

        // Calculate view direction and normal dot product
        val viewY = camera.position.y - centerY

        // Adjust sorting distance for floors when player is very close
        // This helps with extreme close-up viewing angles
        val adjustedDistance = if (abs(viewY) < 0.5) {
            // If very close to the floor, adjust distance to ensure proper sorting
            distance - 1.0
        } else {
            distance
        }

        renderQueue.add(RenderableObject.FloorInfo(
            adjustedDistance,
            screenPoints,
            floor.color,
            floor.texture,
            clippedTexCoords,  // Use the clipped texture coordinates
            floor,
            viewingFromBelow  // Pass the viewing direction flag
        ))
    }
}