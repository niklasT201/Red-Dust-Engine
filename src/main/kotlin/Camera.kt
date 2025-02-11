import kotlin.math.*

class Camera(
    var position: Vec3,
    var yaw: Double = 0.0,
    var pitch: Double = 0.0
) {
    private val rotationSpeed = 0.003

    fun rotate(dx: Double, dy: Double) {
        // Fixed mouse movement - negative dx for correct left/right movement
        yaw -= dx * rotationSpeed
        pitch = (pitch + dy * rotationSpeed).coerceIn(-PI/3, PI/3)
    }
}