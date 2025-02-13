import kotlin.math.*

class Camera(
    var position: Vec3,
    var yaw: Double = 0.0,
    var pitch: Double = 0.0
) {
    private val rotationSpeed = 0.003

    fun rotate(dx: Double, dy: Double) {
        // Update yaw (horizontal rotation)
        yaw -= dx * rotationSpeed

        // Normalize yaw to stay within -PI to PI range
        yaw = normalizeAngle(yaw)

        // Update and clamp pitch (vertical rotation)
        pitch = (pitch + dy * rotationSpeed).coerceIn(-PI/3, PI/3)
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
            normalized >= 45 && normalized < 135 -> "East"   // Correct position - right of South
            normalized >= 135 && normalized < 225 -> "North" // Top of compass
            else -> "West"  // Correct position - left of South
        }
    }
}