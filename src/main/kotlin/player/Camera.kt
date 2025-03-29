package player

import Vec3
import kotlin.math.*

class Camera(
    var position: Vec3,
    var yaw: Double = 0.0,
    var pitch: Double = 0.0
) {
    // Change from private val to var to allow modification
    var rotationSpeed = 0.003
    var invertY: Boolean = false

    // Add setter method to allow controlled changes
    fun changeRotationSpeed(speed: Double) {
        rotationSpeed = speed
    }

    fun changeInvertY(inverted: Boolean) {
        this.invertY = inverted
    }

    // Getter for settings saving
    fun accessRotationSpeed(): Double = rotationSpeed
    fun accessInvertY(): Boolean = invertY

    fun rotate(dx: Double, dy: Double) {
        // Update yaw (horizontal rotation)
        yaw -= dx * rotationSpeed

        // Normalize yaw to stay within -PI to PI range
        yaw = normalizeAngle(yaw)

        val effectiveDy = if (invertY) -dy else dy

        // Update and clamp pitch (vertical rotation)
        pitch = (pitch - effectiveDy * rotationSpeed).coerceIn(-PI / 3, PI / 3)
    }

    private fun normalizeAngle(angle: Double): Double {
        var normalized = angle % (2 * PI)
        if (normalized > PI) {
            normalized -= 2 * PI
        } else if (normalized < -PI) {
            normalized += 2 * PI
        }
        return normalized
    }

    // Helper function to get cardinal direction
    fun getCardinalDirection(): String {
        // Convert yaw to 0-360 range for easier direction calculation
        val degrees = Math.toDegrees(yaw)
        val normalized = (degrees + 360) % 360

        return when {
            normalized >= 315 || normalized < 45 -> "South"
            normalized >= 45 && normalized < 135 -> "East"
            normalized >= 135 && normalized < 225 -> "North"
            else -> "West"
        }
    }
}