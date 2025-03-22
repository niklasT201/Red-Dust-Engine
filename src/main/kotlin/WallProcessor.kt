import player.Camera
import kotlin.math.*

class WallProcessor(
    private val nearPlane: Double,
    private val width: Int,
    private val height: Int,
    private val scale: Double
) {
    fun processWall(wall: Wall, camera: Camera, renderQueue: MutableList<RenderableObject>) {
        // Calculate texture coordinates for the four corners
        val bottomStartTex = Pair(0.0, 1.0)  // Bottom-left
        val bottomEndTex = Pair(1.0, 1.0)    // Bottom-right
        val topStartTex = Pair(0.0, 0.0)     // Top-left
        val topEndTex = Pair(1.0, 0.0)       // Top-right

        // Transform the wall's four corners
        val bottomStart = GeometryUtils.transformPoint(wall.start, camera)
        val bottomEnd = GeometryUtils.transformPoint(wall.end, camera)
        val topStart = GeometryUtils.transformPoint(Vec3(wall.start.x, wall.start.y + wall.height, wall.start.z), camera)
        val topEnd = GeometryUtils.transformPoint(Vec3(wall.end.x, wall.end.y + wall.height, wall.end.z), camera)

        // Skip if all points are behind near plane
        if (listOf(bottomStart, bottomEnd, topStart, topEnd).all { it.z <= nearPlane }) {
            return
        }

        // Storage for clipped vertices and their texture coordinates
        val clippedVertices = mutableListOf<Vec3>()
        val clippedTexCoords = mutableListOf<Pair<Double, Double>>()

        // Clip each edge of the wall against the near plane
        GeometryUtils.clipLineToNearPlane(bottomStart, bottomEnd, nearPlane, bottomStartTex, bottomEndTex)?.let { (p1, p2, texPair) ->
            clippedVertices.add(p1)
            clippedVertices.add(p2)
            texPair.first?.let { clippedTexCoords.add(it) }
            texPair.second?.let { clippedTexCoords.add(it) }
        }

        GeometryUtils.clipLineToNearPlane(bottomEnd, topEnd, nearPlane, bottomEndTex, topEndTex)?.let { (p1, p2, texPair) ->
            if (!clippedVertices.contains(p1)) {
                clippedVertices.add(p1)
                texPair.first?.let { clippedTexCoords.add(it) }
            }
            if (!clippedVertices.contains(p2)) {
                clippedVertices.add(p2)
                texPair.second?.let { clippedTexCoords.add(it) }
            }
        }

        GeometryUtils.clipLineToNearPlane(topEnd, topStart, nearPlane, topEndTex, topStartTex)?.let { (p1, p2, texPair) ->
            if (!clippedVertices.contains(p1)) {
                clippedVertices.add(p1)
                texPair.first?.let { clippedTexCoords.add(it) }
            }
            if (!clippedVertices.contains(p2)) {
                clippedVertices.add(p2)
                texPair.second?.let { clippedTexCoords.add(it) }
            }
        }

        GeometryUtils.clipLineToNearPlane(topStart, bottomStart, nearPlane, topStartTex, bottomStartTex)?.let { (p1, p2, texPair) ->
            if (!clippedVertices.contains(p1)) {
                clippedVertices.add(p1)
                texPair.first?.let { clippedTexCoords.add(it) }
            }
            if (!clippedVertices.contains(p2)) {
                clippedVertices.add(p2)
                texPair.second?.let { clippedTexCoords.add(it) }
            }
        }

        // If we have no vertices after clipping, skip this wall
        if (clippedVertices.size < 3 || clippedTexCoords.size < 3) {
            return
        }

        // Project the clipped vertices to screen coordinates
        val screenPoints = clippedVertices.map {
            GeometryUtils.projectPoint(it, nearPlane, width, height, scale)
        }

        // If we have fewer than 3 points, we can't form a polygon
        if (screenPoints.size < 3) {
            return
        }

        // Calculate center point of wall for distance sorting
        val centerX = (wall.start.x + wall.end.x) / 2
        val centerY = (wall.start.y + wall.end.y) / 2 + wall.height / 2
        val centerZ = (wall.start.z + wall.end.z) / 2

        val distance = sqrt(
            (centerX - camera.position.x).pow(2) +
                    (centerY - camera.position.y).pow(2) +
                    (centerZ - camera.position.z).pow(2)
        )

        renderQueue.add(RenderableObject.WallInfo(
            distance,
            screenPoints,
            wall.color,
            wall.texture,
            clippedTexCoords,
            wall
        ))
    }
}