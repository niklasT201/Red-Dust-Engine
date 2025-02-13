import java.awt.Graphics2D
import java.awt.Color
import java.awt.Polygon
import kotlin.math.*

class Renderer(private val width: Int, private val height: Int) {
    private val fov = PI/3
    private val scale = 1.0 / tan(fov/2)
    private val nearPlane = 0.1
    private val farPlane = 100.0

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

    fun drawWall(g2: Graphics2D, wall: Wall, camera: Camera) {
        val startTransformed = transformPoint(wall.start, camera)
        val endTransformed = transformPoint(wall.end, camera)

        // Near plane clipping
        if (startTransformed.z <= nearPlane && endTransformed.z <= nearPlane) return

        // Calculate wall normal and do backface culling
        val wallVector = Vec3(
            wall.end.x - wall.start.x,
            0.0,
            wall.end.z - wall.start.z
        )
        val normal = Vec3(-wallVector.z, 0.0, wallVector.x)
        val transformedNormal = transformNormal(normal, camera)

        // Don't render if wall is facing away from camera
        if (transformedNormal.z < 0) return

        val topStart = Vec3(wall.start.x, wall.start.y + wall.height, wall.start.z)
        val topEnd = Vec3(wall.end.x, wall.end.y + wall.height, wall.end.z)

        val startTransformedTop = transformPoint(topStart, camera)
        val endTransformedTop = transformPoint(topEnd, camera)

        // Project points
        val (screenX1, screenY1Top) = projectPoint(startTransformedTop)
        val (screenX2, screenY2Top) = projectPoint(endTransformedTop)
        val (_, screenY1Bottom) = projectPoint(startTransformed)
        val (_, screenY2Bottom) = projectPoint(endTransformed)

        // Check if wall is in view frustum
        val inView = screenX1 <= width*1.5 && screenX2 >= -width/2 &&
                minOf(screenY1Top, screenY2Top) <= height*1.5 &&
                maxOf(screenY1Bottom, screenY2Bottom) >= -height/2

        if (inView) {
            val distance = sqrt(startTransformed.z * startTransformed.z + endTransformed.z * endTransformed.z)
            val shade = (1.0 / (1.0 + distance * 0.1)).coerceIn(0.3, 1.0)
            g2.color = Color(
                (wall.color.red * shade).toInt(),
                (wall.color.green * shade).toInt(),
                (wall.color.blue * shade).toInt()
            )

            val wallShape = Polygon(
                intArrayOf(screenX1, screenX2, screenX2, screenX1),
                intArrayOf(screenY1Top, screenY2Top, screenY2Bottom, screenY1Bottom),
                4
            )
            g2.fill(wallShape)
        }
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