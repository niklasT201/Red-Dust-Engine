package render.optimization

import Vec3
import player.Camera
import kotlin.math.*

class ViewFrustum(
    private val fovX: Double,
    private val fovY: Double,
    private val nearPlane: Double,
    private val farPlane: Double
) {
    // Planes defined by normal vector and distance from origin
    private val planes = Array(6) { FloatArray(4) } // left, right, bottom, top, near, far

    // Update frustum planes based on camera position and orientation
    fun update(camera: Camera) {
        val cosYaw = cos(camera.yaw).toFloat()
        val sinYaw = sin(camera.yaw).toFloat()
        val cosPitch = cos(camera.pitch).toFloat()
        val sinPitch = sin(camera.pitch).toFloat()

        // Forward vector
        val forwardX = sinYaw * cosPitch
        val forwardY = sinPitch
        val forwardZ = cosYaw * cosPitch

        // Right vector
        val rightX = cosYaw
        val rightY = 0f
        val rightZ = -sinYaw

        // Up vector
        val upX = sinYaw * sinPitch
        val upY = cosPitch
        val upZ = cosYaw * sinPitch

        // Camera position
        val posX = camera.position.x.toFloat()
        val posY = camera.position.y.toFloat()
        val posZ = camera.position.z.toFloat()

        // Half angles
        val tanHalfFovX = tan(fovX / 2).toFloat()
        val tanHalfFovY = tan(fovY / 2).toFloat()

        // Near plane dimensions
        val nearHeight = 2 * nearPlane.toFloat() * tanHalfFovY
        val nearWidth = 2 * nearPlane.toFloat() * tanHalfFovX

        // Far plane dimensions
        val farHeight = 2 * farPlane.toFloat() * tanHalfFovY
        val farWidth = 2 * farPlane.toFloat() * tanHalfFovX

        // Calculate frustum corners
        val nearCenter = Vec3(
            (posX + forwardX * nearPlane.toFloat()).toDouble(),
            (posY + forwardY * nearPlane.toFloat()).toDouble(),
            (posZ + forwardZ * nearPlane.toFloat()).toDouble()
        )

        val farCenter = Vec3(
            posX + forwardX * farPlane.toFloat().toDouble(),
            posY + forwardY * farPlane.toFloat().toDouble(),
            posZ + forwardZ * farPlane.toFloat().toDouble()
        )

        // Calculate frustum planes

        // Near plane
        planes[4][0] = -forwardX
        planes[4][1] = -forwardY
        planes[4][2] = -forwardZ
        planes[4][3] = -(planes[4][0] * nearCenter.x.toFloat() +
                planes[4][1] * nearCenter.y.toFloat() +
                planes[4][2] * nearCenter.z.toFloat())

        // Far plane
        planes[5][0] = forwardX
        planes[5][1] = forwardY
        planes[5][2] = forwardZ
        planes[5][3] = -(planes[5][0] * farCenter.x.toFloat() +
                planes[5][1] * farCenter.y.toFloat() +
                planes[5][2] * farCenter.z.toFloat())

        // Left plane
        val leftNormal = Vec3(
            cosYaw * tanHalfFovX + sinYaw.toDouble(),
            sinPitch * tanHalfFovX.toDouble(),
            -sinYaw * tanHalfFovX + cosYaw.toDouble()
        )
        val leftNormalLength = sqrt(leftNormal.x * leftNormal.x +
                leftNormal.y * leftNormal.y +
                leftNormal.z * leftNormal.z).toFloat()

        planes[0][0] = leftNormal.x.toFloat() / leftNormalLength
        planes[0][1] = leftNormal.y.toFloat() / leftNormalLength
        planes[0][2] = leftNormal.z.toFloat() / leftNormalLength
        planes[0][3] = -(planes[0][0] * posX + planes[0][1] * posY + planes[0][2] * posZ)

        // Right plane
        val rightNormal = Vec3(
            -cosYaw * tanHalfFovX + sinYaw.toDouble(),
            -sinPitch * tanHalfFovX.toDouble(),
            sinYaw * tanHalfFovX + cosYaw.toDouble()
        )
        val rightNormalLength = sqrt(rightNormal.x * rightNormal.x +
                rightNormal.y * rightNormal.y +
                rightNormal.z * rightNormal.z).toFloat()

        planes[1][0] = rightNormal.x.toFloat() / rightNormalLength
        planes[1][1] = rightNormal.y.toFloat() / rightNormalLength
        planes[1][2] = rightNormal.z.toFloat() / rightNormalLength
        planes[1][3] = -(planes[1][0] * posX + planes[1][1] * posY + planes[1][2] * posZ)

        // Bottom plane
        val bottomNormal = Vec3(
            sinYaw * sinPitch * tanHalfFovY + sinYaw * cosPitch.toDouble(),
            cosPitch * tanHalfFovY - sinPitch.toDouble(),
            cosYaw * sinPitch * tanHalfFovY + cosYaw * cosPitch.toDouble()
        )
        val bottomNormalLength = sqrt(bottomNormal.x * bottomNormal.x +
                bottomNormal.y * bottomNormal.y +
                bottomNormal.z * bottomNormal.z).toFloat()

        planes[2][0] = bottomNormal.x.toFloat() / bottomNormalLength
        planes[2][1] = bottomNormal.y.toFloat() / bottomNormalLength
        planes[2][2] = bottomNormal.z.toFloat() / bottomNormalLength
        planes[2][3] = -(planes[2][0] * posX + planes[2][1] * posY + planes[2][2] * posZ)

        // Top plane
        val topNormal = Vec3(
            -sinYaw * sinPitch * tanHalfFovY + sinYaw * cosPitch.toDouble(),
            -cosPitch * tanHalfFovY - sinPitch.toDouble(),
            -cosYaw * sinPitch * tanHalfFovY + cosYaw * cosPitch.toDouble()
        )
        val topNormalLength = sqrt(topNormal.x * topNormal.x +
                topNormal.y * topNormal.y +
                topNormal.z * topNormal.z).toFloat()

        planes[3][0] = topNormal.x.toFloat() / topNormalLength
        planes[3][1] = topNormal.y.toFloat() / topNormalLength
        planes[3][2] = topNormal.z.toFloat() / topNormalLength
        planes[3][3] = -(planes[3][0] * posX + planes[3][1] * posY + planes[3][2] * posZ)
    }

    // Check if a point is inside the frustum
    fun isPointInFrustum(point: Vec3): Boolean {
        val x = point.x.toFloat()
        val y = point.y.toFloat()
        val z = point.z.toFloat()

        for (plane in planes) {
            if (plane[0] * x + plane[1] * y + plane[2] * z + plane[3] < 0) {
                return false
            }
        }
        return true
    }

    // Check if a sphere is visible in the frustum
    fun isSphereInFrustum(center: Vec3, radius: Float): Boolean {
        val x = center.x.toFloat()
        val y = center.y.toFloat()
        val z = center.z.toFloat()

        for (plane in planes) {
            if (plane[0] * x + plane[1] * y + plane[2] * z + plane[3] < -radius) {
                return false
            }
        }
        return true
    }

    // Check if a box is visible in the frustum
    fun isBoxInFrustum(min: Vec3, max: Vec3): Boolean {
        // Test each of the 8 corners of the box
        val corners = arrayOf(
            Vec3(min.x, min.y, min.z),
            Vec3(min.x, min.y, max.z),
            Vec3(min.x, max.y, min.z),
            Vec3(min.x, max.y, max.z),
            Vec3(max.x, min.y, min.z),
            Vec3(max.x, min.y, max.z),
            Vec3(max.x, max.y, min.z),
            Vec3(max.x, max.y, max.z)
        )

        // For each frustum plane
        for (i in 0 until 6) {
            var inside = false

            // Test all corners against this plane
            for (corner in corners) {
                if (planes[i][0] * corner.x.toFloat() +
                    planes[i][1] * corner.y.toFloat() +
                    planes[i][2] * corner.z.toFloat() +
                    planes[i][3] >= 0) {
                    inside = true
                    break
                }
            }

            // If all corners are outside this plane, the box is outside the frustum
            if (!inside) {
                return false
            }
        }

        return true
    }
}