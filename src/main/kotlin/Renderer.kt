import java.awt.Graphics2D
import java.awt.Color
import java.awt.Polygon
import kotlin.math.*

class Renderer(private val width: Int, private val height: Int) {
    private val fov = PI/3
    private val scale = 1.0 / tan(fov/2)

    fun drawFloor(g2: Graphics2D, floor: Floor, camera: Camera) {
        val corners = listOf(
            Vec3(floor.x1, floor.y, floor.z1),
            Vec3(floor.x2, floor.y, floor.z1),
            Vec3(floor.x2, floor.y, floor.z2),
            Vec3(floor.x1, floor.y, floor.z2)
        )

        val screenPoints = corners.map { point ->
            val transformed = transformPoint(point, camera)
            projectPoint(transformed)
        }

        if (corners.any { (it.z - camera.position.z) > 0.1 }) {
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

        if (startTransformed.z <= 0.1 && endTransformed.z <= 0.1) return

        val (screenX1, screenY1Top) = projectPoint(Vec3(startTransformed.x, startTransformed.y + wall.height, startTransformed.z))
        val (screenX2, screenY2Top) = projectPoint(Vec3(endTransformed.x, endTransformed.y + wall.height, endTransformed.z))
        val (_, screenY1Bottom) = projectPoint(startTransformed)
        val (_, screenY2Bottom) = projectPoint(endTransformed)

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

    private fun transformPoint(point: Vec3, camera: Camera): Vec3 {
        val relPoint = Vec3(
            point.x - camera.position.x,
            point.y - camera.position.y,
            point.z - camera.position.z
        )

        val cosYaw = cos(camera.yaw)
        val sinYaw = sin(camera.yaw)
        val cosPitch = cos(camera.pitch)
        val sinPitch = sin(camera.pitch)

        return Vec3(
            relPoint.x * cosYaw + relPoint.z * sinYaw,
            relPoint.y * cosPitch - (-relPoint.x * sinYaw + relPoint.z * cosYaw) * sinPitch,
            (-relPoint.x * sinYaw + relPoint.z * cosYaw) * cosPitch + relPoint.y * sinPitch
        )
    }

    private fun projectPoint(point: Vec3): Pair<Int, Int> {
        val z = max(point.z, 0.1)
        return Pair(
            (width/2 * (1 + scale * point.x / z)).toInt(),
            (height/2 * (1 - scale * point.y / z)).toInt()
        )
    }
}