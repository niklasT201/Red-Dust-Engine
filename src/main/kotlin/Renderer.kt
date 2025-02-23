import java.awt.*
import kotlin.math.*

class Renderer(private val width: Int, private val height: Int) {
    private val fov = PI/3
    private val scale = 1.0 / tan(fov/2)
    private val nearPlane = 0.1
    private val farPlane = 100.0

    // Combined class to store rendering info for both walls and floors
    private sealed class RenderableObject {
        abstract val distance: Double
        abstract val screenPoints: List<Pair<Int, Int>>
        abstract val color: Color

        data class WallInfo(
            override val distance: Double,
            override val screenPoints: List<Pair<Int, Int>>,
            override val color: Color,
            val wall: Wall
        ) : RenderableObject()

        data class FloorInfo(
            override val distance: Double,
            override val screenPoints: List<Pair<Int, Int>>,
            override val color: Color,
            val floor: Floor
        ) : RenderableObject()
    }

    fun drawScene(g2: Graphics2D, walls: List<Wall>, floors: List<Floor>, camera: Camera) {
        val renderQueue = mutableListOf<RenderableObject>()

        // Process floors
        for (floor in floors) {
            val corners = listOf(
                Vec3(floor.x1, floor.y, floor.z1),
                Vec3(floor.x2, floor.y, floor.z1),
                Vec3(floor.x2, floor.y, floor.z2),
                Vec3(floor.x1, floor.y, floor.z2)
            )

            val transformedCorners = corners.map { transformPoint(it, camera) }
            if (transformedCorners.all { it.z <= nearPlane }) continue

            val screenPoints = transformedCorners.map { projectPoint(it) }

            // Calculate center point for distance sorting
            val centerX = (floor.x1 + floor.x2) / 2
            val centerY = floor.y
            val centerZ = (floor.z1 + floor.z2) / 2

            // Calculate distance to camera for sorting
            val dx = centerX - camera.position.x
            val dy = centerY - camera.position.y
            val dz = centerZ - camera.position.z
            val distance = sqrt(dx * dx + dy * dy + dz * dz)

            // Calculate view direction and normal dot product for backface culling
            val viewY = camera.position.y - centerY
            val viewLength = sqrt(dx * dx + dy * dy + dz * dz)
            val dotProduct = viewY / viewLength

            // Skip if viewing from wrong side
            if (floor.y > camera.position.y && dotProduct > 0) continue
            if (floor.y < camera.position.y && dotProduct < 0) continue

            // Calculate shading
            val shade = (1.0 / (1.0 + distance * 0.1)).coerceIn(0.3, 1.0)
            val shadedColor = Color(
                (floor.color.red * shade).toInt(),
                (floor.color.green * shade).toInt(),
                (floor.color.blue * shade).toInt()
            )

            renderQueue.add(RenderableObject.FloorInfo(distance, screenPoints, shadedColor, floor))
        }

        // Process walls
        for (wall in walls) {
            val startTransformed = transformPoint(wall.start, camera)
            val endTransformed = transformPoint(wall.end, camera)

            // Get the wall corners in view space
            val topStart = Vec3(wall.start.x, wall.start.y + wall.height, wall.start.z)
            val topEnd = Vec3(wall.end.x, wall.end.y + wall.height, wall.end.z)

            val startTransformedTop = transformPoint(topStart, camera)
            val endTransformedTop = transformPoint(topEnd, camera)

            if (listOf(startTransformed, endTransformed, startTransformedTop, endTransformedTop)
                    .all { it.z <= nearPlane }) continue

            val screenPoints = listOf(
                projectPoint(startTransformedTop),
                projectPoint(endTransformedTop),
                projectPoint(endTransformed),
                projectPoint(startTransformed)
            )

            // Calculate center point of wall for distance sorting
            val centerX = (wall.start.x + wall.end.x) / 2
            val centerY = (wall.start.y + wall.end.y) / 2 + wall.height / 2
            val centerZ = (wall.start.z + wall.end.z) / 2

            val distance = sqrt(
                (centerX - camera.position.x).pow(2) +
                        (centerY - camera.position.y).pow(2) +
                        (centerZ - camera.position.z).pow(2)
            )

            // Calculate wall normal and view direction for shading
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

            val toCameraX = camera.position.x - centerX
            val toCameraZ = camera.position.z - centerZ
            val viewLength = sqrt(toCameraX * toCameraX + toCameraZ * toCameraZ)
            val dotProduct = (normal.x * toCameraX + normal.z * toCameraZ) / viewLength

            // Calculate shading
            val angleFactor = abs(dotProduct)
            val distanceFactor = (1.0 / (1.0 + distance * 0.1)).coerceIn(0.3, 1.0)
            val shade = (angleFactor * distanceFactor).coerceIn(0.3, 1.0)

            val shadedColor = Color(
                (wall.color.red * shade).toInt(),
                (wall.color.green * shade).toInt(),
                (wall.color.blue * shade).toInt()
            )

            renderQueue.add(RenderableObject.WallInfo(distance, screenPoints, shadedColor, wall))
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
            g2.color = renderable.color
            g2.fill(polygon)
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