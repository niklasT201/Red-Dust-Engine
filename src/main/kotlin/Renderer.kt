import player.Camera
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
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
        abstract val texture: ImageEntry?
        abstract val textureCoords: List<Pair<Double, Double>>

        data class WallInfo(
            override val distance: Double,
            override val screenPoints: List<Pair<Int, Int>>,
            override val color: Color,
            override val texture: ImageEntry?,
            override val textureCoords: List<Pair<Double, Double>>,
            val wall: Wall
        ) : RenderableObject()

        data class FloorInfo(
            override val distance: Double,
            override val screenPoints: List<Pair<Int, Int>>,
            override val color: Color,
            override val texture: ImageEntry?,
            override val textureCoords: List<Pair<Double, Double>>,
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

            // More robust near plane checking - if any point is in front of the near plane, process the floor
            if (transformedCorners.none { it.z > nearPlane }) continue

            // Clip the floor polygon against the near plane if needed
            val clippedCorners = clipPolygonToNearPlane(transformedCorners)
            if (clippedCorners.isEmpty()) continue

            val screenPoints = clippedCorners.map { projectPoint(it) }

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
            val viewLength = sqrt(dx * dx + dy * dy + dz * dz)
            val dotProduct = viewY / viewLength

            /* Shading disabled as requested
            val shade = (1.0 / (1.0 + distance * 0.1)).coerceIn(0.3, 1.0)
            val shadedColor = Color(
                (floor.color.red * shade).toInt(),
                (floor.color.green * shade).toInt(),
                (floor.color.blue * shade).toInt()
            )
            */

            // Determine if we're viewing the floor from below (for texture orientation)
            val viewingFromBelow = (floor.y > camera.position.y)

            // Adjust texture coordinates based on viewing direction
            val textureCoords = clippedCorners.map { vertex ->
                // Calculate relative position within the floor
                val u = (vertex.x - floor.x1) / (floor.x2 - floor.x1)
                val v = (vertex.z - floor.z1) / (floor.z2 - floor.z1)

                // Flip texture coordinates when viewing from below
                if (viewingFromBelow) Pair(u, 1.0 - v) else Pair(u, v)
            }

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
                textureCoords,
                floor
            ))
        }

        // Process walls
        for (wall in walls) {
            // Transform the wall's four corners
            val bottomStart = transformPoint(wall.start, camera)
            val bottomEnd = transformPoint(wall.end, camera)
            val topStart = transformPoint(Vec3(wall.start.x, wall.start.y + wall.height, wall.start.z), camera)
            val topEnd = transformPoint(Vec3(wall.end.x, wall.end.y + wall.height, wall.end.z), camera)

            // Skip if all points are behind near plane
            if (listOf(bottomStart, bottomEnd, topStart, topEnd).all { it.z <= nearPlane }) {
                continue
            }

            // Clip each edge of the wall against the near plane
            val clippedEdges = mutableListOf<Pair<Vec3, Vec3>>()

            // Try to clip each edge
            clipLineToNearPlane(bottomStart, bottomEnd)?.let { clippedEdges.add(it) }
            clipLineToNearPlane(bottomEnd, topEnd)?.let { clippedEdges.add(it) }
            clipLineToNearPlane(topEnd, topStart)?.let { clippedEdges.add(it) }
            clipLineToNearPlane(topStart, bottomStart)?.let { clippedEdges.add(it) }

            // If we have no edges after clipping, skip this wall
            if (clippedEdges.isEmpty()) {
                continue
            }

            // Collect all unique vertices after clipping
            val clippedVertices = clippedEdges.flatMap { listOf(it.first, it.second) }.distinctBy { Triple(it.x, it.y, it.z) }

            // Project the clipped vertices to screen coordinates
            val screenPoints = clippedVertices.map { projectPoint(it) }

            // If we have fewer than 3 points, we can't form a polygon
            if (screenPoints.size < 3) {
                continue
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

            /* Shading disabled as requested
            val angleFactor = abs(dotProduct)
            val distanceFactor = (1.0 / (1.0 + distance * 0.1)).coerceIn(0.3, 1.0)
            val shade = (angleFactor * distanceFactor).coerceIn(0.3, 1.0)

            val shadedColor = Color(
                (wall.color.red * shade).toInt(),
                (wall.color.green * shade).toInt(),
                (wall.color.blue * shade).toInt()
            )
            */

            // Calculate texture coordinates based on wall dimensions
            val wallLength = sqrt(
                (wall.end.x - wall.start.x).pow(2) +
                        (wall.end.z - wall.start.z).pow(2)
            )

            // Texture coordinates for wall
            val textureCoords = clippedVertices.map { vertex ->
                // Calculate relative position within the wall
                val horzPos = if (wall.end.x != wall.start.x || wall.end.z != wall.start.z) {
                    val wallVec = Vec3(wall.end.x - wall.start.x, 0.0, wall.end.z - wall.start.z)
                    val pointVec = Vec3(vertex.x - wall.start.x, 0.0, vertex.z - wall.start.z)
                    val wallLength = sqrt(wallVec.x * wallVec.x + wallVec.z * wallVec.z)
                    val projection = (pointVec.x * wallVec.x + pointVec.z * wallVec.z) / wallLength
                    projection / wallLength
                } else {
                    0.0
                }

                // Calculate vertical position (0 at bottom, 1 at top)
                val vertPos = (vertex.y - wall.start.y) / wall.height

                Pair(horzPos, vertPos)
            }

            renderQueue.add(RenderableObject.WallInfo(
                distance,
                screenPoints,
                wall.color,
                wall.texture,
                textureCoords,
                wall
            ))
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

            // Check if we have a texture for this object
            if (renderable.texture != null) {
                drawTexturedPolygon(g2, polygon, renderable.texture as ImageEntry, renderable.textureCoords, renderable.screenPoints)
            } else {
                // Fall back to solid color if no texture
                g2.color = renderable.color
                g2.fill(polygon)
            }
        }
    }

    private fun drawTexturedPolygon(
        g2: Graphics2D,
        polygon: Polygon,
        textureEntry: ImageEntry,
        textureCoords: List<Pair<Double, Double>>,
        screenPoints: List<Pair<Int, Int>>
    ) {
        if (screenPoints.size < 3 || textureCoords.size != screenPoints.size) return

        val image = textureEntry.image
        val imageWidth = image.getWidth(null)
        val imageHeight = image.getHeight(null)

        // Set up clipping to the polygon
        val originalClip = g2.clip
        g2.clip = polygon

        try {
            // More stable texture mapping for complex polygons
            val srcPoints = Array(screenPoints.size.coerceAtMost(3)) { i ->
                val (u, v) = textureCoords[i]
                // Map texture coordinates directly to image dimensions
                Point2D.Double(u * imageWidth, v * imageHeight)
            }

            val dstPoints = Array(screenPoints.size.coerceAtMost(3)) { i ->
                val (x, y) = screenPoints[i]
                Point2D.Double(x.toDouble(), y.toDouble())
            }

            val transform = createTransform(srcPoints, dstPoints)

            // Draw the transformed image
            g2.drawImage(
                image,
                AffineTransform(
                    transform[0], transform[3],
                    transform[1], transform[4],
                    transform[2], transform[5]
                ),
                null
            )
        } finally {
            // Restore original clip
            g2.clip = originalClip
        }
    }

    // Create a transform matrix from source to destination points
    private fun createTransform(src: Array<Point2D.Double>, dst: Array<Point2D.Double>): DoubleArray {
        val matrix = DoubleArray(6)

        val x1 = src[0].x
        val y1 = src[0].y
        val x2 = src[1].x
        val y2 = src[1].y
        val x3 = src[2].x
        val y3 = src[2].y

        val u1 = dst[0].x
        val v1 = dst[0].y
        val u2 = dst[1].x
        val v2 = dst[1].y
        val u3 = dst[2].x
        val v3 = dst[2].y

        // Compute the adjoint matrix
        val det = (y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3)
        if (abs(det) < 1e-10) {
            // Determinant too small, use identity transform
            matrix[0] = 1.0
            matrix[1] = 0.0
            matrix[2] = 0.0
            matrix[3] = 0.0
            matrix[4] = 1.0
            matrix[5] = 0.0
            return matrix
        }

        val invDet = 1.0 / det

        // Compute matrix elements
        matrix[0] = ((y2 - y3) * (u1 - u3) + (u3 - u2) * (y1 - y3)) * invDet
        matrix[1] = ((x3 - x2) * (u1 - u3) + (u2 - u3) * (x1 - x3)) * invDet
        matrix[2] = u1
        matrix[3] = ((y2 - y3) * (v1 - v3) + (v3 - v2) * (y1 - y3)) * invDet
        matrix[4] = ((x3 - x2) * (v1 - v3) + (v2 - v3) * (x1 - x3)) * invDet
        matrix[5] = v1

        return matrix
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

    private fun clipLineToNearPlane(p1: Vec3, p2: Vec3): Pair<Vec3, Vec3>? {
        // If both points are in front of near plane, no clipping needed
        if (p1.z > nearPlane && p2.z > nearPlane) {
            return Pair(p1, p2)
        }

        // If both points are behind near plane, line is not visible
        if (p1.z <= nearPlane && p2.z <= nearPlane) {
            return null
        }

        // One point is behind, one is in front - clip line
        val t = (nearPlane - p1.z) / (p2.z - p1.z)

        // The clipped point
        val clippedPoint = Vec3(
            p1.x + t * (p2.x - p1.x),
            p1.y + t * (p2.y - p1.y),
            nearPlane // Set exactly to near plane
        )

        // Return the clipped line - ensure in front point is first
        return if (p1.z > nearPlane) {
            Pair(p1, clippedPoint)
        } else {
            Pair(clippedPoint, p2)
        }
    }

    private fun clipPolygonToNearPlane(vertices: List<Vec3>): List<Vec3> {
        if (vertices.isEmpty()) return emptyList()

        val result = mutableListOf<Vec3>()

        // For each edge in the polygon
        for (i in vertices.indices) {
            val current = vertices[i]
            val next = vertices[(i + 1) % vertices.size]

            // If current vertex is in front of near plane, include it
            if (current.z > nearPlane) {
                result.add(current)
            }

            // Check if edge crosses the near plane
            if ((current.z > nearPlane && next.z <= nearPlane) ||
                (current.z <= nearPlane && next.z > nearPlane)) {

                // Calculate intersection point with near plane
                val t = (nearPlane - current.z) / (next.z - current.z)
                val intersect = Vec3(
                    current.x + t * (next.x - current.x),
                    current.y + t * (next.y - current.y),
                    nearPlane
                )
                result.add(intersect)
            }
        }

        return result
    }

    private fun projectPoint(point: Vec3): Pair<Int, Int> {
        // Use max to ensure we don't divide by a very small number
        val z = maxOf(point.z, nearPlane * 1.01) // Add a small margin to avoid division problems

        // Apply perspective projection
        return Pair(
            (width/2 * (1 + scale * point.x / z)).toInt(),
            (height/2 * (1 - scale * point.y / z)).toInt()
        )
    }
}