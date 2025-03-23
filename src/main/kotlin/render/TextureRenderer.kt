package render

import ImageEntry
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import kotlin.math.*

object TextureRenderer {
    // Original method now with an optional shadow factor parameter
    fun drawTexturedPolygon(
        g2: Graphics2D,
        polygon: Polygon,
        textureEntry: ImageEntry,
        textureCoords: List<Pair<Double, Double>>,
        screenPoints: List<Pair<Int, Int>>,
        shadowFactor: Double = 1.0
    ) {
        if (screenPoints.size < 3 || textureCoords.size != screenPoints.size) return

        val image = textureEntry.image
        val imageWidth = image.getWidth(null)
        val imageHeight = image.getHeight(null)

        // Set up clipping to the polygon
        val originalClip = g2.clip
        g2.clip = polygon

        // Save the original composite
        val originalComposite = g2.composite

        try {
            // Create triangular mesh
            val triangles = triangulatePolygon(screenPoints)

            // Find corresponding texture coordinates for each triangle
            for (triangle in triangles) {
                val srcTex = Array(3) { Point2D.Double() }
                val dstPoints = Array(3) { Point2D.Double() }
                var validMapping = true

                // For each vertex in the triangle
                for (i in 0..2) {
                    val screenPoint = triangle[i]

                    // Find the matching texture coordinate
                    var texCoord: Pair<Double, Double>?
                    val idx = screenPoints.indexOfFirst { it.first == screenPoint.first && it.second == screenPoint.second }

                    if (idx >= 0 && idx < textureCoords.size) {
                        texCoord = textureCoords[idx]
                    } else {
                        // If precise match not found, find the closest point
                        var minDist = Double.MAX_VALUE
                        var closestIdx = -1

                        for (j in screenPoints.indices) {
                            val pt = screenPoints[j]
                            val dist = sqrt(
                                (pt.first - screenPoint.first).toDouble().pow(2) +
                                        (pt.second - screenPoint.second).toDouble().pow(2)
                            )

                            if (dist < minDist) {
                                minDist = dist
                                closestIdx = j
                            }
                        }

                        if (closestIdx >= 0) {
                            texCoord = textureCoords[closestIdx]
                        } else {
                            validMapping = false
                            break
                        }
                    }

                    srcTex[i] = Point2D.Double(texCoord.first * imageWidth, texCoord.second * imageHeight)
                    dstPoints[i] = Point2D.Double(screenPoint.first.toDouble(), screenPoint.second.toDouble())
                }

                if (validMapping) {
                    // Create a triangle for this specific part
                    val trianglePoly = Polygon(
                        intArrayOf(triangle[0].first, triangle[1].first, triangle[2].first),
                        intArrayOf(triangle[0].second, triangle[1].second, triangle[2].second),
                        3
                    )

                    // Save the current clip and set it to this triangle
                    val triangleClip = g2.clip
                    g2.clip = trianglePoly

                    // Calculate transform
                    val transform = perspectiveTransform(srcTex, dstPoints)

                    // Draw the transformed image for this triangle
                    g2.drawImage(
                        image,
                        AffineTransform(
                            transform[0], transform[3],
                            transform[1], transform[4],
                            transform[2], transform[5]
                        ),
                        null
                    )

                    // Restore the clip
                    g2.clip = triangleClip
                }
            }

            // Apply shadow to the entire polygon AFTER all triangles are drawn
            if (shadowFactor < 1.0) {
                // Create a color with alpha based on the shadow factor
                val shadowAlpha = ((1.0 - shadowFactor) * 255).toInt()
                if (shadowAlpha > 0) {
                    val shadowColor = Color(0, 0, 0, shadowAlpha)
                    g2.composite = AlphaComposite.SrcOver
                    g2.color = shadowColor
                    g2.fill(polygon)  // Fill the entire polygon at once
                }
            }
        } finally {
            // Restore original clip and composite
            g2.clip = originalClip
            g2.composite = originalComposite
        }
    }

    // Helper function to triangulate a polygon
    private fun triangulatePolygon(points: List<Pair<Int, Int>>): List<List<Pair<Int, Int>>> {
        val triangles = mutableListOf<List<Pair<Int, Int>>>()

        // Simple ear-clipping algorithm for convex polygons
        if (points.size == 3) {
            // Already a triangle
            triangles.add(points)
        } else if (points.size == 4) {
            // For quadrilaterals (most walls and floors), split into two triangles
            // This maintains better texture mapping by creating triangles along the diagonal
            triangles.add(listOf(points[0], points[1], points[2]))
            triangles.add(listOf(points[0], points[2], points[3]))
        } else if (points.size > 4) {
            // For polygons with more than 4 sides (rare in your case),
            // use ear clipping or fan triangulation

            // Find approximate center point
            val centerX = points.sumOf { it.first } / points.size
            val centerY = points.sumOf { it.second } / points.size
            val center = Pair(centerX, centerY)

            // Create triangles in fan pattern from center
            for (i in 0 until points.size) {
                val nextIdx = (i + 1) % points.size
                triangles.add(listOf(center, points[i], points[nextIdx]))
            }
        }

        return triangles
    }

    // Improved perspective transform calculation
    private fun perspectiveTransform(src: Array<Point2D.Double>, dst: Array<Point2D.Double>): DoubleArray {
        // Use a more robust algorithm for perspective transform
        val matrix = DoubleArray(6)

        try {
            // For triangles, use a simpler and more stable approach
            // Calculate transformation matrix for texture mapping
            val x0 = src[0].x
            val y0 = src[0].y
            val x1 = src[1].x
            val y1 = src[1].y
            val x2 = src[2].x
            val y2 = src[2].y

            val u0 = dst[0].x
            val v0 = dst[0].y
            val u1 = dst[1].x
            val v1 = dst[1].y
            val u2 = dst[2].x
            val v2 = dst[2].y

            // Calculate determinant
            val det = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
            if (abs(det) < 0.0001) {
                throw IllegalArgumentException("Determinant is too small")
            }

            // Calculate matrix elements
            matrix[0] = ((u1 - u0) * (y2 - y0) - (u2 - u0) * (y1 - y0)) / det
            matrix[1] = ((u2 - u0) * (x1 - x0) - (u1 - u0) * (x2 - x0)) / det
            matrix[2] = u0 - matrix[0] * x0 - matrix[1] * y0

            matrix[3] = ((v1 - v0) * (y2 - y0) - (v2 - v0) * (y1 - y0)) / det
            matrix[4] = ((v2 - v0) * (x1 - x0) - (v1 - v0) * (x2 - x0)) / det
            matrix[5] = v0 - matrix[3] * x0 - matrix[4] * y0

        } catch (e: Exception) {
            // Fallback to identity transform if calculation fails
            matrix[0] = 1.0
            matrix[1] = 0.0
            matrix[2] = 0.0
            matrix[3] = 0.0
            matrix[4] = 1.0
            matrix[5] = 0.0
        }

        return matrix
    }
}