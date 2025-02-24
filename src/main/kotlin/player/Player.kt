package player

import Vec3
import Wall
import kotlin.math.cos
import kotlin.math.sin

class Player(
    val camera: Camera = Camera(Vec3(0.0, 1.7, -5.0)),
    private val moveSpeed: Double = 0.05,
    private val playerRadius: Double = 0.3,
    private val minHeight: Double = 0.5,
    private val maxHeight: Double = 2.5
) {
    // Getter for player position (via camera)
    val position: Vec3 get() = camera.position

    // player.Camera-related methods
    fun rotate(dx: Double, dy: Double) {
        camera.rotate(dx, dy)
    }

    fun getCardinalDirection(): String {
        return camera.getCardinalDirection()
    }

    fun getYawDegrees(): Int {
        return Math.toDegrees(camera.yaw).toInt()
    }

    fun getPitchDegrees(): Int {
        return Math.toDegrees(camera.pitch).toInt()
    }

    // Movement and collision
    fun move(forward: Double, right: Double, up: Double, walls: List<Wall>) {
        // Calculate movement based on camera direction
        val forwardX = -sin(camera.yaw)
        val forwardZ = cos(camera.yaw)
        val rightX = cos(camera.yaw)
        val rightZ = sin(camera.yaw)

        // Calculate new position using forward and right vectors
        val newX = camera.position.x + (forward * forwardX + right * rightX) * moveSpeed
        val newZ = camera.position.z + (forward * forwardZ + right * rightZ) * moveSpeed
        val newY = camera.position.y + up * moveSpeed

        // Collision detection
        val canMove = checkCollision(newX, newZ, walls)

        // Apply movement with collision detection
        if (canMove.first) {
            camera.position.x = newX
        }
        if (canMove.second) {
            camera.position.z = newZ
        }

        // Apply Y movement with bounds
        camera.position.y = newY.coerceIn(minHeight, maxHeight)
    }

    // Separate collision detection method
    private fun checkCollision(newX: Double, newZ: Double, walls: List<Wall>): Pair<Boolean, Boolean> {
        var canMoveX = true
        var canMoveZ = true

        // Check each wall for collision
        for (wall in walls) {
            // Simple box collision check
            val wallMinX = minOf(wall.start.x, wall.end.x) - playerRadius
            val wallMaxX = maxOf(wall.start.x, wall.end.x) + playerRadius
            val wallMinZ = minOf(wall.start.z, wall.end.z) - playerRadius
            val wallMaxZ = maxOf(wall.start.z, wall.end.z) + playerRadius

            // Check X collision
            if (newX in wallMinX..wallMaxX &&
                camera.position.z in wallMinZ..wallMaxZ) {
                canMoveX = false
            }

            // Check Z collision
            if (camera.position.x in wallMinX..wallMaxX &&
                newZ in wallMinZ..wallMaxZ) {
                canMoveZ = false
            }
        }

        return Pair(canMoveX, canMoveZ)
    }

    // Set player position from spawn point
    fun setPositionFromSpawn(x: Double, y: Double, floorHeight: Double = 0.0) {
        camera.position.x = x
        camera.position.z = y
        camera.position.y = 1.7 + floorHeight // Default player height
    }
}