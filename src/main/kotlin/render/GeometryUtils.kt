package render

import Vec3
import player.Camera
import kotlin.math.*

object GeometryUtils {
    // Transform a 3D point relative to camera
    fun transformPoint(point: Vec3, camera: Camera): Vec3 {
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

    // Project a 3D point to 2D screen coordinates
    fun projectPoint(point: Vec3, nearPlane: Double, width: Int, height: Int, scale: Double): Pair<Int, Int> {
        // Use max to ensure we don't divide by a very small number
        val z = maxOf(point.z, nearPlane * 1.01) // Add a small margin to avoid division problems

        // Apply perspective projection
        return Pair(
            (width/2 * (1 + scale * point.x / z)).toInt(),
            (height/2 * (1 - scale * point.y / z)).toInt()
        )
    }

    // Clip a line segment against the near plane with texture coordinates
    fun clipLineToNearPlane(
        p1: Vec3, p2: Vec3,
        nearPlane: Double,
        tex1: Pair<Double, Double>? = null,
        tex2: Pair<Double, Double>? = null
    ): Triple<Vec3, Vec3, Pair<Pair<Double, Double>?, Pair<Double, Double>?>>? {
        // If both points are in front of near plane, no clipping needed
        if (p1.z > nearPlane && p2.z > nearPlane) {
            return Triple(p1, p2, Pair(tex1, tex2))
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

        // Interpolate texture coordinate if provided
        val clippedTex = if (tex1 != null && tex2 != null) {
            // If this is a vertical edge (top-bottom), preserve horizontal coordinate
            if ((tex1.first == tex2.first) && (tex1.first == 0.0 || tex1.first == 1.0)) {
                Pair(tex1.first, tex1.second + t * (tex2.second - tex1.second))
            }
            // If this is a horizontal edge (left-right), preserve vertical coordinate
            else if ((tex1.second == tex2.second) && (tex1.second == 0.0 || tex1.second == 1.0)) {
                Pair(tex1.first + t * (tex2.first - tex1.first), tex1.second)
            }
            // Otherwise do regular interpolation
            else {
                Pair(
                    tex1.first + t * (tex2.first - tex1.first),
                    tex1.second + t * (tex2.second - tex1.second)
                )
            }
        } else {
            null
        }

        // Return the clipped line - ensure in front point is first
        return if (p1.z > nearPlane) {
            Triple(p1, clippedPoint, Pair(tex1, clippedTex))
        } else {
            Triple(clippedPoint, p2, Pair(clippedTex, tex2))
        }
    }

    // Clip a polygon against the near plane
    fun clipPolygonToNearPlane(
        vertices: List<Vec3>,
        texCoords: List<Pair<Double, Double>>,
        nearPlane: Double
    ): Pair<List<Vec3>, List<Pair<Double, Double>>> {
        if (vertices.isEmpty()) return Pair(emptyList(), emptyList())

        val resultVerts = mutableListOf<Vec3>()
        val resultTexCoords = mutableListOf<Pair<Double, Double>>()

        // For each edge in the polygon
        for (i in vertices.indices) {
            val current = vertices[i]
            val currentTex = texCoords[i]
            val next = vertices[(i + 1) % vertices.size]
            val nextTex = texCoords[(i + 1) % texCoords.size]

            // If current vertex is in front of near plane, include it
            if (current.z > nearPlane) {
                resultVerts.add(current)
                resultTexCoords.add(currentTex)
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

                // Interpolate texture coordinates
                val interpTex = Pair(
                    currentTex.first + t * (nextTex.first - currentTex.first),
                    currentTex.second + t * (nextTex.second - currentTex.second)
                )

                resultVerts.add(intersect)
                resultTexCoords.add(interpTex)
            }
        }

        return Pair(resultVerts, resultTexCoords)
    }
}