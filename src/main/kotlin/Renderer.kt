import java.awt.Graphics2D
import java.awt.Color
import java.awt.Polygon
import kotlin.math.*

class Renderer(private val width: Int, private val height: Int) {
    private val fov = PI/3
    private val scale = 1.0 / tan(fov/2)
    private val nearPlane = 0.1
    private val farPlane = 100.0

    // Helper class to store wall drawing information
    private data class WallRenderInfo(
        val wall: Wall,
        val screenPoints: List<Pair<Int, Int>>,
        val distance: Double,
        val color: Color
    )

    fun drawFloor(g2: Graphics2D, floor: Floor, camera: Camera) {
        val corners = listOf(
            Vec3(floor.x1, floor.y, floor.z1),
            Vec3(floor.x2, floor.y, floor.z1),
            Vec3(floor.x2, floor.y, floor.z2),
            Vec3(floor.x1, floor.y, floor.z2)
        )

        // Transform all corners
        val transformedCorners = corners.map { transformPoint(it, camera) }

        // Check if floor is behind camera or outside view frustum
        if (transformedCorners.all { it.z <= nearPlane }) return

        // Calculate normal vector of floor
        val normal = Vec3(0.0, 1.0, 0.0)
        val transformedNormal = transformNormal(normal, camera)

        // Don't render if floor is facing away from camera (backface culling)
        if (transformedNormal.y < 0) return

        // Project corners to screen space
        val screenPoints = transformedCorners.map { projectPoint(it) }

        // Check if any points are within the view frustum
        val anyVisible = screenPoints.any { (x, y) ->
            x >= -width/2 && x <= width*1.5 && y >= -height/2 && y <= height*1.5
        }

        if (anyVisible) {
            val polygon = Polygon(
                screenPoints.map { it.first }.toIntArray(),
                screenPoints.map { it.second }.toIntArray(),
                4
            )

            val centerZ = (floor.z1 + floor.z2) / 2 - camera.position.z
            val centerX = (floor.x1 + floor.x2) / 2 - camera.position.x
            val distance = sqrt(centerX * centerX + centerZ * centerZ)
            val shade = (1.0 / (1.0 + distance * 0.1)).coerceIn(0.3, 1.0)

            g2.color = Color(
                (floor.color.red * shade).toInt(),
                (floor.color.green * shade).toInt(),
                (floor.color.blue * shade).toInt()
            )
            g2.fill(polygon)
        }
    }

    fun drawWall(g2: Graphics2D, walls: List<Wall>, camera: Camera) {
        val wallsToRender = mutableListOf<WallRenderInfo>()

        // Calculate view frustum bounds
        val frustumLeft = -width/2
        val frustumRight = (width * 1.5).toInt()
        val frustumTop = -height/2
        val frustumBottom = (height * 1.5).toInt()

        for (wall in walls) {
            val startTransformed = transformPoint(wall.start, camera)
            val endTransformed = transformPoint(wall.end, camera)

            // Get the wall corners in view space
            val topStart = Vec3(wall.start.x, wall.start.y + wall.height, wall.start.z)
            val topEnd = Vec3(wall.end.x, wall.end.y + wall.height, wall.end.z)

            val startTransformedTop = transformPoint(topStart, camera)
            val endTransformedTop = transformPoint(topEnd, camera)

            // Improved near plane clipping check
            val allPoints = listOf(startTransformed, endTransformed, startTransformedTop, endTransformedTop)
            if (allPoints.all { it.z <= nearPlane }) continue

            // Calculate wall normal
            val wallVector = Vec3(
                wall.end.x - wall.start.x,
                0.0,
                wall.end.z - wall.start.z
            )
            val normal = Vec3(-wallVector.z, 0.0, wallVector.x).let {
                // Normalize the normal vector
                val length = sqrt(it.x * it.x + it.z * it.z)
                Vec3(it.x / length, 0.0, it.z / length)
            }

            // Calculate view direction vector (from wall center to camera)
            val wallCenterX = (wall.start.x + wall.end.x) / 2
            val wallCenterZ = (wall.start.z + wall.end.z) / 2
            val toCameraX = camera.position.x - wallCenterX
            val toCameraZ = camera.position.z - wallCenterZ

            // Normalize the view direction vector
            val viewLength = sqrt(toCameraX * toCameraX + toCameraZ * toCameraZ)
            val normalizedToCameraX = toCameraX / viewLength
            val normalizedToCameraZ = toCameraZ / viewLength

            // Calculate dot product between normal and view direction
            val dotProduct = normal.x * normalizedToCameraX + normal.z * normalizedToCameraZ

            // Only cull if the wall is clearly facing away from the camera
            if (dotProduct < -0.1) continue

            // Project points to screen space
            val screenPoints = listOf(
                projectPoint(startTransformedTop),
                projectPoint(endTransformedTop),
                projectPoint(endTransformed),
                projectPoint(startTransformed)
            )

            // Improved view frustum check
            val anyPointInFrustum = screenPoints.any { (x, y) ->
                x >= frustumLeft && x <= frustumRight && y >= frustumTop && y <= frustumBottom
            }

            // Check if wall crosses the view frustum
            val crossesFrustum = screenPoints.zipWithNext { a, b ->
                val lineIntersectsFrustum = lineIntersectsFrustum(
                    a.first, a.second,
                    b.first, b.second,
                    frustumLeft, frustumTop,
                    frustumRight, frustumBottom
                )
                lineIntersectsFrustum
            }.any { it }

            if (anyPointInFrustum || crossesFrustum) {
                // Calculate distance for depth sorting
                val distance = sqrt(toCameraX * toCameraX + toCameraZ * toCameraZ)

                // Calculate shading based on angle to camera and distance
                val angleFactor = (dotProduct + 1) / 2 // Convert from [-1,1] to [0,1]
                val distanceFactor = (1.0 / (1.0 + distance * 0.1)).coerceIn(0.3, 1.0)
                val shade = (angleFactor * distanceFactor).coerceIn(0.3, 1.0)

                val shadedColor = Color(
                    (wall.color.red * shade).toInt(),
                    (wall.color.green * shade).toInt(),
                    (wall.color.blue * shade).toInt()
                )

                wallsToRender.add(WallRenderInfo(wall, screenPoints, distance, shadedColor))
            }
        }

        // Sort walls by distance (furthest first)
        wallsToRender.sortByDescending { it.distance }

        // Draw walls in sorted order
        for (wallInfo in wallsToRender) {
            val polygon = Polygon(
                wallInfo.screenPoints.map { it.first }.toIntArray(),
                wallInfo.screenPoints.map { it.second }.toIntArray(),
                4
            )
            g2.color = wallInfo.color
            g2.fill(polygon)
        }
    }

    // Helper function to check if a line segment intersects with the view frustum
    private fun lineIntersectsFrustum(
        x1: Int, y1: Int,
        x2: Int, y2: Int,
        frustumLeft: Int, frustumTop: Int,
        frustumRight: Int, frustumBottom: Int
    ): Boolean {
        // Check if line segment intersects with any of the frustum edges
        return lineIntersectsLine(x1, y1, x2, y2, frustumLeft, frustumTop, frustumRight, frustumTop) ||
                lineIntersectsLine(x1, y1, x2, y2, frustumRight, frustumTop, frustumRight, frustumBottom) ||
                lineIntersectsLine(x1, y1, x2, y2, frustumRight, frustumBottom, frustumLeft, frustumBottom) ||
                lineIntersectsLine(x1, y1, x2, y2, frustumLeft, frustumBottom, frustumLeft, frustumTop)
    }

    // Helper function to check if two line segments intersect
    private fun lineIntersectsLine(
        x1: Int, y1: Int,
        x2: Int, y2: Int,
        x3: Int, y3: Int,
        x4: Int, y4: Int
    ): Boolean {
        val denominator = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
        if (denominator == 0) return false

        val t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denominator.toDouble()
        val u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denominator.toDouble()

        return t in 0.0..1.0 && u in 0.0..1.0
    }

    private fun transformPoint(point: Vec3, camera: Camera): Vec3 {
        // Translate point relative to camera
        val relPoint = Vec3(
            point.x - camera.position.x,
            point.y - camera.position.y,
            point.z - camera.position.z
        )

        // Calculate view matrix components
        val cosYaw = cos(camera.yaw)
        val sinYaw = sin(camera.yaw)
        val cosPitch = cos(camera.pitch)
        val sinPitch = sin(camera.pitch)

        // Apply view transformation
        return Vec3(
            relPoint.x * cosYaw + relPoint.z * sinYaw,
            relPoint.y * cosPitch - (-relPoint.x * sinYaw + relPoint.z * cosYaw) * sinPitch,
            (-relPoint.x * sinYaw + relPoint.z * cosYaw) * cosPitch + relPoint.y * sinPitch
        )
    }

    private fun transformNormal(normal: Vec3, camera: Camera): Vec3 {
        val cosYaw = cos(camera.yaw)
        val sinYaw = sin(camera.yaw)
        val cosPitch = cos(camera.pitch)
        val sinPitch = sin(camera.pitch)

        return Vec3(
            normal.x * cosYaw + normal.z * sinYaw,
            normal.y * cosPitch - (-normal.x * sinYaw + normal.z * cosYaw) * sinPitch,
            (-normal.x * sinYaw + normal.z * cosYaw) * cosPitch + normal.y * sinPitch
        )
    }

    private fun projectPoint(point: Vec3): Pair<Int, Int> {
        // Ensure point is not behind near plane
        val z = maxOf(point.z, nearPlane)

        // Apply perspective projection
        return Pair(
            (width/2 * (1 + scale * point.x / z)).toInt(),
            (height/2 * (1 - scale * point.y / z)).toInt()
        )
    }
}