package render.optimization

import Vec3
import Wall
import Floor
import WaterSurface
import Ramp
import kotlin.math.*

class Quadtree(
    private val bounds: AABBBounds,
    private val maxDepth: Int = 5,
    private val maxObjects: Int = 10,
    private val currentDepth: Int = 0
) {
    // Axis-aligned bounding box
    data class AABBBounds(
        val minX: Double,
        val minZ: Double,
        val maxX: Double,
        val maxZ: Double
    ) {
        val centerX: Double get() = (minX + maxX) / 2
        val centerZ: Double get() = (minZ + maxZ) / 2
        val width: Double get() = maxX - minX
        val depth: Double get() = maxZ - minZ

        fun contains(x: Double, z: Double): Boolean {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ
        }

        fun intersects(other: AABBBounds): Boolean {
            return !(other.minX > maxX ||
                    other.maxX < minX ||
                    other.minZ > maxZ ||
                    other.maxZ < minZ)
        }
    }

    // Different types of objects that can be stored
    sealed class QuadObject {
        abstract val bounds: AABBBounds
        abstract val originalObject: Any

        data class WallObject(
            override val bounds: AABBBounds,
            override val originalObject: Wall
        ) : QuadObject()

        data class FloorObject(
            override val bounds: AABBBounds,
            override val originalObject: Floor
        ) : QuadObject()

        data class WaterObject(
            override val bounds: AABBBounds,
            override val originalObject: WaterSurface
        ) : QuadObject()

        data class RampObject(
            override val bounds: AABBBounds,
            override val originalObject: Ramp
        ) : QuadObject()
    }

    private val objects = mutableListOf<QuadObject>()
    private var children: Array<Quadtree?> = arrayOfNulls(4)
    private var divided = false

    // Insert an object into the quadtree
    fun insert(obj: QuadObject): Boolean {
        // If object doesn't intersect with this node, don't insert
        if (!bounds.intersects(obj.bounds)) {
            return false
        }

        // If we have space and at max depth, add here
        if (objects.size < maxObjects || currentDepth >= maxDepth) {
            objects.add(obj)
            return true
        }

        // Otherwise, subdivide if needed and add to children
        if (!divided) {
            subdivide()
        }

        // Try to insert into children
        var inserted = false
        for (child in children) {
            if (child?.insert(obj) == true) {
                inserted = true
            }
        }

        // If not inserted in any child, add to this node
        if (!inserted) {
            objects.add(obj)
        }

        return true
    }

    // Subdivide this node into four children
    private fun subdivide() {
        val x = bounds.centerX
        val z = bounds.centerZ
        val halfWidth = bounds.width / 2
        val halfDepth = bounds.depth / 2

        // Top left
        children[0] = Quadtree(
            AABBBounds(bounds.minX, bounds.minZ, x, z),
            maxDepth,
            maxObjects,
            currentDepth + 1
        )

        // Top right
        children[1] = Quadtree(
            AABBBounds(x, bounds.minZ, bounds.maxX, z),
            maxDepth,
            maxObjects,
            currentDepth + 1
        )

        // Bottom left
        children[2] = Quadtree(
            AABBBounds(bounds.minX, z, x, bounds.maxZ),
            maxDepth,
            maxObjects,
            currentDepth + 1
        )

        // Bottom right
        children[3] = Quadtree(
            AABBBounds(x, z, bounds.maxX, bounds.maxZ),
            maxDepth,
            maxObjects,
            currentDepth + 1
        )

        divided = true

        // Redistribute objects to children
        val objectsToRedistribute = objects.toList()
        objects.clear()

        for (obj in objectsToRedistribute) {
            insert(obj)
        }
    }

    // Query objects within a range
    fun query(range: AABBBounds, found: MutableList<QuadObject> = mutableListOf()): List<QuadObject> {
        // If range doesn't intersect this node, return empty list
        if (!bounds.intersects(range)) {
            return found
        }

        // Add objects that intersect with the range
        for (obj in objects) {
            if (range.intersects(obj.bounds)) {
                found.add(obj)
            }
        }

        // If subdivided, check children
        if (divided) {
            for (child in children) {
                child?.query(range, found)
            }
        }

        return found
    }

    // Query objects within view frustum
    fun queryFrustum(
        frustum: ViewFrustum,
        cameraPosition: Vec3,
        maxDistance: Double = Double.MAX_VALUE,
        found: MutableList<QuadObject> = mutableListOf()
    ): List<QuadObject> {
        // Create a bounding box for quick check
        val nodeBounds = AABBBounds(
            bounds.minX, bounds.minZ,
            bounds.maxX, bounds.maxZ
        )

        // Create min/max Y values for the box (simplified approach)
        val minY = -1000.0 // Adjust based on your world height
        val maxY = 1000.0  // Adjust based on your world height

        // Check if this node's bounding box is in the frustum
        if (!frustum.isBoxInFrustum(
                Vec3(nodeBounds.minX, minY, nodeBounds.minZ),
                Vec3(nodeBounds.maxX, maxY, nodeBounds.maxZ)
            )) {
            return found
        }

        // Calculate center and approximate radius for distance check
        val centerX = (nodeBounds.minX + nodeBounds.maxX) / 2
        val centerZ = (nodeBounds.minZ + nodeBounds.maxZ) / 2
        val centerY = (minY + maxY) / 2

        // Check if this node is within max distance
        val dx = centerX - cameraPosition.x
        val dy = centerY - cameraPosition.y
        val dz = centerZ - cameraPosition.z
        val distSq = dx * dx + dy * dy + dz * dz

        if (distSq > maxDistance * maxDistance) {
            return found
        }

        // Add objects that are within the frustum and distance
        for (obj in objects) {
            // Create a bounding sphere for the object
            val objBounds = obj.bounds
            val objCenterX = (objBounds.minX + objBounds.maxX) / 2
            val objCenterZ = (objBounds.minZ + objBounds.maxZ) / 2
            val objCenterY = when (obj) {
                is QuadObject.FloorObject -> (obj.originalObject as Floor).y
                is QuadObject.WallObject -> {
                    val wall = obj.originalObject as Wall
                    (wall.start.y + wall.end.y + wall.height) / 2
                }
                is QuadObject.WaterObject -> (obj.originalObject as WaterSurface).y
                is QuadObject.RampObject -> {
                    val ramp = obj.originalObject as Ramp
                    (ramp.corner1.y + ramp.corner2.y + ramp.corner3.y + ramp.corner4.y) / 4
                }
            }

            // Calculate radius as half the diagonal of the bounding box
            val width = objBounds.maxX - objBounds.minX
            val depth = objBounds.maxZ - objBounds.minZ
            val radius = sqrt(width * width + depth * depth) / 2

            // Check if object is in frustum
            if (frustum.isSphereInFrustum(Vec3(objCenterX, objCenterY, objCenterZ), radius.toFloat())) {
                // Check distance
                val objDx = objCenterX - cameraPosition.x
                val objDy = objCenterY - cameraPosition.y
                val objDz = objCenterZ - cameraPosition.z
                val objDistSq = objDx * objDx + objDy * objDy + objDz * objDz

                if (objDistSq <= maxDistance * maxDistance) {
                    found.add(obj)
                }
            }
        }

        // If subdivided, check children
        if (divided) {
            for (child in children) {
                child?.queryFrustum(frustum, cameraPosition, maxDistance, found)
            }
        }

        return found
    }

    // Clear the quadtree
    fun clear() {
        objects.clear()
        for (i in children.indices) {
            children[i] = null
        }
        divided = false
    }
}