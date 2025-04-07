package player

import Vec3
import Wall
import Floor
import Ramp
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
    var gravityEnabled: Boolean = false
    private var verticalVelocity: Double = 0.0
    var gravity: Double = 0.01 // Acceleration due to gravity
    var jumpStrength: Double = 0.2
    var terminalVelocity: Double = -0.5

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
    fun move(forward: Double, right: Double, up: Double, walls: List<Wall>, floors: List<Floor>, ramps: List<Ramp>) {
        // Calculate movement based on camera direction
        val forwardX = -sin(camera.yaw)
        val forwardZ = cos(camera.yaw)
        val rightX = cos(camera.yaw)
        val rightZ = sin(camera.yaw)
        val potentialNewX = camera.position.x + (forward * forwardX + right * rightX) * moveSpeed
        val potentialNewZ = camera.position.z + (forward * forwardZ + right * rightZ) * moveSpeed

        // Calculate new position using forward and right vectors
        val currentY = camera.position.y
        var potentialNewY = currentY
        if (gravityEnabled) {
            // Apply gravity
            verticalVelocity -= gravity
            verticalVelocity = maxOf(verticalVelocity, terminalVelocity)
            potentialNewY += verticalVelocity
        } else {
            potentialNewY += up * moveSpeed
            verticalVelocity = 0.0
        }

        // Collision detection for walls
        val canMove = checkWallCollision(potentialNewX, potentialNewZ, walls)
        val finalX = if (canMove.first) potentialNewX else camera.position.x
        val finalZ = if (canMove.second) potentialNewZ else camera.position.z

        // Apply horizontal movement with wall collision detection
        val currentFeetY = currentY - playerHeight
        val newFeetY = potentialNewY - playerHeight
        var finalY = potentialNewY

        // Check for ramp collision at the new position
        val rampCollision = checkRampCollision(finalX, newFeetY, finalZ, ramps)

        // If on a ramp, adjust player height
        if (rampCollision.first) {
            // Handle landing/moving on ramp
            val rampSurfaceY = rampCollision.second
            if (newFeetY <= rampSurfaceY + 0.05) { // Check if moving onto or along ramp
                finalY = rampSurfaceY + playerHeight
                if (verticalVelocity < 0) {
                    verticalVelocity *= 0.5
                } else {
                    verticalVelocity = 0.0
                }
            }
        } else {
            // 2. Not on a ramp, check Ceilings (when moving up)
            val potentialHeadY = potentialNewY + headClearance
            val ceilingCheckResult = checkCeilingCollision(finalX, potentialHeadY, finalZ, floors)

            if (ceilingCheckResult.first) {
                // Check if moving upwards INTO the ceiling
                if (potentialNewY > currentY && potentialHeadY >= ceilingCheckResult.second) {
                    finalY = ceilingCheckResult.second - headClearance // Stop below ceiling
                    verticalVelocity = 0.0
                } else if (potentialNewY > currentY) {
                    // Prevent upward movement if head is already intersecting slightly
                    verticalVelocity = 0.0
                }
            }

            // 3. Check Floors (if not hitting a ceiling OR if moving down)
            if (!ceilingCheckResult.first || potentialNewY <= currentY) {

                val floorCollision = checkFloorCollision(finalX, currentFeetY, newFeetY, finalZ, floors)

                if (floorCollision.first) {
                    // Floor detected within the movement step
                    val collidingFloorY = floorCollision.second

                    // Head-bump + fly down fix: Allow moving down if trying to fly away from a floor above current pos
                    if (up < 0 && collidingFloorY > currentFeetY) {
                        finalY = potentialNewY // Allow flying down
                    }
                    // Standard landing: If target feet are at/below the detected floor
                    else if (newFeetY <= collidingFloorY) {
                        finalY = collidingFloorY + playerHeight // Land on floor
                        verticalVelocity = 0.0
                    }
                    // Else: Collision detected but somehow newFeetY > collidingFloorY? Use potentialNewY.
                    else {
                        finalY = potentialNewY
                    }
                } else {
                    // No floor collision during this step, allow movement to potentialNewY
                    finalY = potentialNewY
                }
            }
        }

        // Finally, update x and z position
        camera.position.x = finalX
        camera.position.y = finalY
        camera.position.z = finalZ

        // --- Standing Adjustment (Post-Movement Ground Snap) ---
        if (gravityEnabled && kotlin.math.abs(verticalVelocity) < 0.01 && !rampCollision.first) {
            val currentFeetNow = camera.position.y - playerHeight
            val slightlyBelowFeet = currentFeetNow - 0.05

            // Use modified check for the small downward step
            val standingCheck = checkFloorCollision(camera.position.x, currentFeetNow, slightlyBelowFeet, camera.position.z, floors)
            if (standingCheck.first) {
                camera.position.y = standingCheck.second + playerHeight // Snap to ground
                // Optionally ensure verticalVelocity is zero again here if needed
                // verticalVelocity = 0.0
            }
        }
    }

    private fun checkWallCollision(newX: Double, newZ: Double, walls: List<Wall>): Pair<Boolean, Boolean> {
        var canMoveX = true
        var canMoveZ = true

        // Calculate the player's vertical collision range
        val playerFeetY = camera.position.y - playerHeight
        val playerHeadY = camera.position.y // Eye level is the effective top for horizontal collision checks

        // Check each wall for collision
        for (wall in walls) {
            // Wall's vertical range
            val wallBaseY = wall.start.y
            val wallTopY = wall.start.y + wall.height

            // Check if player is NOT entirely above or entirely below the wall.
            val isVerticallyRelevant = playerHeadY > wallBaseY && playerFeetY < wallTopY

            if (!isVerticallyRelevant) {
                continue // Player is entirely above or below this wall, skip horizontal check
            }

            // Simple box collision check
            val wallMinX = minOf(wall.start.x, wall.end.x) - playerRadius
            val wallMaxX = maxOf(wall.start.x, wall.end.x) + playerRadius
            val wallMinZ = minOf(wall.start.z, wall.end.z) - playerRadius
            val wallMaxZ = maxOf(wall.start.z, wall.end.z) + playerRadius

            // Check potential X movement against current Z
            if (newX >= wallMinX && newX <= wallMaxX &&
                camera.position.z >= wallMinZ && camera.position.z <= wallMaxZ) {
                // simple check prevents moving X if current Z is within wall bounds
                canMoveX = false
            }

            // Check potential Z movement against current X
            if (camera.position.x >= wallMinX && camera.position.x <= wallMaxX &&
                newZ >= wallMinZ && newZ <= wallMaxZ) {
                // simple check prevents moving Z if current X is within wall bounds
                canMoveZ = false
            }

            // Optimization: If both are blocked, no need to check further walls
            if (!canMoveX && !canMoveZ) break
        }

        return Pair(canMoveX, canMoveZ)
    }

    private fun checkFloorCollision(x: Double, currentFeetY: Double, newFeetY: Double, z: Double, floors: List<Floor>): Pair<Boolean, Double> {
        var highestCollidingFloorY = Double.NEGATIVE_INFINITY
        var hasCollision = false
        val epsilon = 0.001 // Tolerance for floating point comparisons

        for (floor in floors) {
            // Check horizontal bounds first
            if (x >= floor.x1 - playerRadius && x <= floor.x2 + playerRadius &&
                z >= floor.z1 - playerRadius && z <= floor.z2 + playerRadius) {

                // Check if the floor's Y-level is BETWEEN the starting feet pos and the ending feet pos.
                if (floor.y <= currentFeetY + epsilon && floor.y >= newFeetY) {
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

    private fun checkRampCollision(x: Double, y: Double, z: Double, ramps: List<Ramp>): Pair<Boolean, Double> {
        var isOnRamp = false
        var rampHeightAtPosition = Double.NEGATIVE_INFINITY

        for (ramp in ramps) {
            // Check if player is within the ramp's horizontal boundaries
            if (x >= minOf(ramp.corner1.x, ramp.corner2.x, ramp.corner3.x, ramp.corner4.x) - playerRadius &&
                x <= maxOf(ramp.corner1.x, ramp.corner2.x, ramp.corner3.x, ramp.corner4.x) + playerRadius &&
                z >= minOf(ramp.corner1.z, ramp.corner2.z, ramp.corner3.z, ramp.corner4.z) - playerRadius &&
                z <= maxOf(ramp.corner1.z, ramp.corner2.z, ramp.corner3.z, ramp.corner4.z) + playerRadius) {

                // Calculate where on the ramp the player is
                val heightAtPosition = calculateHeightOnRamp(x, z, ramp)

                // Check if player's feet are at or below the ramp height
                if (y <= heightAtPosition && heightAtPosition > rampHeightAtPosition) {
                    rampHeightAtPosition = heightAtPosition
                    isOnRamp = true
                }
            }
        }

        return Pair(isOnRamp, rampHeightAtPosition)
    }

    // Helper function to calculate the height of a point on the ramp
    private fun calculateHeightOnRamp(x: Double, z: Double, ramp: Ramp): Double {
        // First, determine which way the ramp slopes:
        // Find the lowest and highest corners
        val corners = listOf(ramp.corner1, ramp.corner2, ramp.corner3, ramp.corner4)
        val minX = corners.minOf { it.x }
        val maxX = corners.maxOf { it.x }
        val minZ = corners.minOf { it.z }
        val maxZ = corners.maxOf { it.z }

        // Clamp the player's position to the ramp's boundaries
        val clampedX = x.coerceIn(minX, maxX)
        val clampedZ = z.coerceIn(minZ, maxZ)

        // Calculate normalized position on the ramp (0.0 to 1.0 in both directions)
        val normalizedX = if (maxX > minX) (clampedX - minX) / (maxX - minX) else 0.0
        val normalizedZ = if (maxZ > minZ) (clampedZ - minZ) / (maxZ - minZ) else 0.0

        // Find heights at the four corners
        val y00 = corners.find { it.x == minX && it.z == minZ }?.y ?: corners[0].y
        val y10 = corners.find { it.x == maxX && it.z == minZ }?.y ?: corners[1].y
        val y11 = corners.find { it.x == maxX && it.z == maxZ }?.y ?: corners[2].y
        val y01 = corners.find { it.x == minX && it.z == maxZ }?.y ?: corners[3].y

        // Bilinear interpolation of height
        val top = y00 * (1 - normalizedX) + y10 * normalizedX
        val bottom = y01 * (1 - normalizedX) + y11 * normalizedX
        val height = top * (1 - normalizedZ) + bottom * normalizedZ

        return height
    }
}