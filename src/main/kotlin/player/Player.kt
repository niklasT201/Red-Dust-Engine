package player

import Vec3
import Wall
import Floor
import kotlin.math.cos
import kotlin.math.sin

class Player(
    val camera: Camera = Camera(Vec3(0.0, 1.7, -5.0)),
    var moveSpeed: Double = 0.05,
    var playerRadius: Double = 0.3,
    var playerHeight: Double = 1.7,  // Distance from eyes to feet
    var headClearance: Double = 0.3   // Extra space needed above the head
) {
    // Gravity-related properties
    private var gravityEnabled: Boolean = false
    private var verticalVelocity: Double = 0.0
    private val gravity: Double = 0.01 // Acceleration due to gravity
    private val jumpStrength: Double = 0.2
    private val terminalVelocity: Double = -0.5

    // Getter for player position (via camera)
    val position: Vec3 get() = camera.position

    // Camera-related methods
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

    // Method to update all movement settings at once
    fun setMovementSettings(
        moveSpeed: Double,
        playerRadius: Double,
        playerHeight: Double,
        headClearance: Double
    ) {
        this.moveSpeed = moveSpeed
        this.playerRadius = playerRadius
        this.playerHeight = playerHeight
        this.headClearance = headClearance
    }

    fun setGravity(enabled: Boolean) {
        gravityEnabled = enabled
        // Reset vertical velocity when toggling gravity
        verticalVelocity = 0.0
    }

    // New method to handle jumping
    fun jump() {
        // Only jump if on ground (vertically stationary)
        if (gravityEnabled && verticalVelocity == 0.0) {
            verticalVelocity = jumpStrength
        }
    }

    // Movement and collision
    fun move(forward: Double, right: Double, up: Double, walls: List<Wall>, floors: List<Floor>) {
        // Calculate movement based on camera direction
        val forwardX = -sin(camera.yaw)
        val forwardZ = cos(camera.yaw)
        val rightX = cos(camera.yaw)
        val rightZ = sin(camera.yaw)

        // Calculate new position using forward and right vectors
        val newX = camera.position.x + (forward * forwardX + right * rightX) * moveSpeed
        val newZ = camera.position.z + (forward * forwardZ + right * rightZ) * moveSpeed

        // Modify vertical movement for gravity
        var newY = camera.position.y
        if (gravityEnabled) {
            // Apply gravity
            verticalVelocity -= gravity
            verticalVelocity = maxOf(verticalVelocity, terminalVelocity)
            newY += verticalVelocity
        } else {
            newY += up * moveSpeed
            verticalVelocity = 0.0
        }

        // Collision detection for walls
        val canMove = checkWallCollision(newX, newZ, walls)

        // Apply horizontal movement with wall collision detection
        if (canMove.first) {
            camera.position.x = newX
        }
        if (canMove.second) {
            camera.position.z = newZ
        }

        // Get player's feet position (camera is at eye level)
        val currentFeetY = camera.position.y - playerHeight
        val newFeetY = newY - playerHeight

        // Check if moving up through a floor
        if (up > 0) {
            // When moving up, check if head will hit a ceiling
            val headCollision = checkCeilingCollision(camera.position.x, newY + headClearance, camera.position.z, floors)
            if (headCollision.first) {
                // Hit ceiling, stop at collision point minus head clearance
                camera.position.y = headCollision.second - headClearance
            } else {
                // No ceiling collision, move freely upward
                camera.position.y = newY
            }
        }
        // Check if moving down onto or through a floor
        else if (up < 0 || gravityEnabled) {
            // When moving down, check if feet will hit a floor
            val floorCollision = checkFloorCollision(camera.position.x, newFeetY, camera.position.z, floors)
            if (floorCollision.first) {
                // Hit floor, place feet exactly on floor
                camera.position.y = floorCollision.second + playerHeight
                verticalVelocity = 0.0
            } else {
                // No floor collision, move freely downward
                camera.position.y = newY
            }
        }
        // Not moving vertically, still check if we're standing on a floor
        else {
            // Check if standing on floor (a tiny bit below feet to account for precision)
            val standingCheck = checkFloorCollision(camera.position.x, currentFeetY - 0.01, camera.position.z, floors)
            if (standingCheck.first && standingCheck.second - currentFeetY < 0.2) {
                camera.position.y = standingCheck.second + playerHeight
            }
        }
    }

    private fun checkWallCollision(newX: Double, newZ: Double, walls: List<Wall>): Pair<Boolean, Boolean> {
        var canMoveX = true
        var canMoveZ = true

        // Check each wall for collision
        for (wall in walls) {
            // Only consider walls if player is on or near the same floor
            // Allow for a bit of vertical movement (jumping, stairs, etc)
            val playerY = camera.position.y
            val wallY = wall.start.y
            val wallTopY = wall.start.y + wall.height

            // Skip walls that aren't on the player's current floor
            // Player can be anywhere between the floor and ceiling of a wall
            if (playerY < wallY || playerY > wallTopY) {
                continue  // Wall is on a different floor, skip collision check
            }

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

    private fun checkFloorCollision(x: Double, feetY: Double, z: Double, floors: List<Floor>): Pair<Boolean, Double> {
        // Find the highest floor at or below the player's potential feet position
        var highestCollidingFloorY = Double.NEGATIVE_INFINITY // Use a more descriptive name
        var hasCollision = false

        for (floor in floors) {
            // Check if player is within the floor's horizontal boundaries
            if (x >= floor.x1 - playerRadius && x <= floor.x2 + playerRadius &&
                z >= floor.z1 - playerRadius && z <= floor.z2 + playerRadius) {

                // Check if the potential feet position is at or below this floor's level.
                if (feetY <= floor.y) {
                    // This floor is a potential candidate for collision.
                    if (floor.y > highestCollidingFloorY) {
                        highestCollidingFloorY = floor.y
                        hasCollision = true
                    }
                }
            }
        }

        return Pair(hasCollision, highestCollidingFloorY)
    }

    private fun checkCeilingCollision(x: Double, headY: Double, z: Double, floors: List<Floor>): Pair<Boolean, Double> {
        // Find the lowest ceiling above the player's head
        var hasCollision = false
        var ceilingY = Double.POSITIVE_INFINITY

        for (floor in floors) {
            // Check if player is within the floor's horizontal boundaries
            if (x >= floor.x1 - playerRadius && x <= floor.x2 + playerRadius &&
                z >= floor.z1 - playerRadius && z <= floor.z2 + playerRadius) {

                // Only consider floors that are above the player's current position
                val floorAboveHead = floor.y > camera.position.y && headY >= floor.y
                if (floorAboveHead && floor.y < ceilingY) {
                    ceilingY = floor.y
                    hasCollision = true
                }
            }
        }

        return Pair(hasCollision, ceilingY)
    }

    // Set player position from spawn point
    fun setPositionFromSpawn(x: Double, y: Double, floorHeight: Double = 0.0) {
        camera.position.x = x
        camera.position.z = y
        camera.position.y = playerHeight + floorHeight // Eye level above floor
    }
}