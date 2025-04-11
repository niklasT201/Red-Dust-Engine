package player.uis

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Rectangle
import java.io.Serializable

// Base class for all UI components
abstract class UIComponent(
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int
) : Serializable {
    var visible: Boolean = true
    var id: String = ""  // Unique identifier for this component

    // Render the component
    abstract fun render(g2: Graphics2D, screenWidth: Int, screenHeight: Int)

    // Check if a point is inside this component
    fun contains(px: Int, py: Int): Boolean {
        return px >= x && px <= x + width && py >= y && py <= y + height
    }

    // Clone this component
    abstract fun clone(): UIComponent
}

// Health bar component
class HealthBarComponent(x: Int, y: Int, width: Int, height: Int) : UIComponent(x, y, width, height) {
    var healthColor: Color = Color(215, 0, 0)
    var backgroundColor: Color = Color(30, 30, 30)
    var borderColor: Color = Color(80, 80, 80)
    var textColor: Color = Color(215, 186, 69)

    override fun render(g2: Graphics2D, screenWidth: Int, screenHeight: Int) {
        if (!visible) return

        // Save original settings
        val originalFont = g2.font

        // Draw panel background
        g2.color = backgroundColor
        g2.fillRect(x, y, width, height)

        // Draw borders
        g2.color = borderColor
        g2.drawRect(x, y, width, height)

        // Draw label
        g2.font = Font("Courier New", Font.BOLD, 14)
        g2.color = textColor
        g2.drawString("HEALTH", x + 10, y + 20)

        // Draw value with larger font
        g2.font = Font("Courier New", Font.BOLD, 28)
        g2.drawString("100", x + 10, y + 46)

        // Draw health bar
        g2.color = Color(15, 15, 15)
        g2.fillRect(x + 10, y + 50, width - 20, 16)

        g2.color = healthColor
        g2.fillRect(x + 10, y + 50, width - 20, 16)

        // Draw notches
        g2.color = Color(0, 0, 0)
        for (i in 1..9) {
            val notchX = x + 10 + ((width - 20) * i / 10)
            g2.drawLine(notchX, y + 50, notchX, y + 66)
        }

        // Restore original settings
        g2.font = originalFont
    }

    override fun clone(): UIComponent {
        val clone = HealthBarComponent(x, y, width, height)
        clone.visible = visible
        clone.id = id
        clone.healthColor = healthColor
        clone.backgroundColor = backgroundColor
        clone.borderColor = borderColor
        clone.textColor = textColor
        return clone
    }
}

// Ammo bar component
class AmmoBarComponent(x: Int, y: Int, width: Int, height: Int) : UIComponent(x, y, width, height) {
    var ammoColor: Color = Color(0, 96, 176)
    var backgroundColor: Color = Color(30, 30, 30)
    var borderColor: Color = Color(80, 80, 80)
    var textColor: Color = Color(215, 186, 69)

    override fun render(g2: Graphics2D, screenWidth: Int, screenHeight: Int) {
        if (!visible) return

        // Save original settings
        val originalFont = g2.font

        // Draw panel background
        g2.color = backgroundColor
        g2.fillRect(x, y, width, height)

        // Draw borders
        g2.color = borderColor
        g2.drawRect(x, y, width, height)

        // Draw label
        g2.font = Font("Courier New", Font.BOLD, 14)
        g2.color = textColor
        g2.drawString("AMMO", x + 10, y + 20)

        // Draw value with larger font
        g2.font = Font("Courier New", Font.BOLD, 28)
        g2.drawString("48", x + 10, y + 46)

        // Draw ammo bar
        g2.color = Color(15, 15, 15)
        g2.fillRect(x + 10, y + 50, width - 20, 16)

        g2.color = ammoColor
        g2.fillRect(x + 10, y + 50, width - 20, 16)

        // Draw notches
        g2.color = Color(0, 0, 0)
        for (i in 1..9) {
            val notchX = x + 10 + ((width - 20) * i / 10)
            g2.drawLine(notchX, y + 50, notchX, y + 66)
        }

        // Restore original settings
        g2.font = originalFont
    }

    override fun clone(): UIComponent {
        val clone = AmmoBarComponent(x, y, width, height)
        clone.visible = visible
        clone.id = id
        clone.ammoColor = ammoColor
        clone.backgroundColor = backgroundColor
        clone.borderColor = borderColor
        clone.textColor = textColor
        return clone
    }
}

// Face component
class FaceComponent(x: Int, y: Int, width: Int, height: Int) : UIComponent(x, y, width, height) {
    var backgroundColor: Color = Color(30, 30, 30)
    var borderColor: Color = Color(80, 80, 80)
    var textColor: Color = Color(215, 186, 69)

    // Pre-rendered face image
    private var faceImage = createFaceImage(64)

    private fun createFaceImage(size: Int): java.awt.image.BufferedImage {
        val image = java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)

        // Face background
        g.color = Color(231, 181, 131) // Skin tone
        g.fillOval(2, 2, size-4, size-4)

        // Black border
        g.color = Color.BLACK
        g.drawOval(2, 2, size-4, size-4)

        // Eyes
        g.color = Color.WHITE
        g.fillOval(size/4, size/3, size/5, size/6)
        g.fillOval(size*9/16, size/3, size/5, size/6)

        g.color = Color(30, 50, 210) // Blue eyes
        g.fillOval(size*9/32, size*11/30, size/8, size/8)
        g.fillOval(size*19/32, size*11/30, size/8, size/8)

        // Mouth
        g.color = Color(180, 50, 50) // Reddish mouth
        g.fillArc(size/3, size*19/32, size/3, size/5, 0, 180)

        g.dispose()
        return image
    }

    override fun render(g2: Graphics2D, screenWidth: Int, screenHeight: Int) {
        if (!visible) return

        // Save original settings
        val originalFont = g2.font

        // Draw panel background
        g2.color = backgroundColor
        g2.fillRect(x, y, width, height)

        // Draw borders
        g2.color = borderColor
        g2.drawRect(x, y, width, height)

        // Draw stats
        g2.font = Font("Courier New", Font.BOLD, 14)
        g2.color = textColor
        g2.drawString("K: 14/45", x + 10, y + 18)
        g2.drawString("I: 3/12", x + 10, y + 34)
        g2.drawString("S: 1/5", x + 10, y + 50)
        g2.drawString("ARMOR 75%", x + 10, y + 76)

        // Draw the face
        g2.drawImage(faceImage, x + width - 74, y + (height - 64) / 2, null)

        // Restore original settings
        g2.font = originalFont
    }

    override fun clone(): UIComponent {
        val clone = FaceComponent(x, y, width, height)
        clone.visible = visible
        clone.id = id
        clone.backgroundColor = backgroundColor
        clone.borderColor = borderColor
        clone.textColor = textColor
        return clone
    }
}

// Weapon selector component
class WeaponSelectorComponent(x: Int, y: Int, width: Int = 60, height: Int = 50) : UIComponent(x, y, width, height) {
    var backgroundColor: Color = Color(30, 30, 30)
    var borderColor: Color = Color(80, 80, 80)
    var textColor: Color = Color(215, 186, 69)
    var currentWeapon: Int = 2

    override fun render(g2: Graphics2D, screenWidth: Int, screenHeight: Int) {
        if (!visible) return

        // Save original settings
        val originalFont = g2.font

        // Draw panel background
        g2.color = backgroundColor
        g2.fillRect(x, y, width, height)

        // Draw borders
        g2.color = borderColor
        g2.drawRect(x, y, width, height)

        // Draw weapon number
        g2.font = Font("Courier New", Font.BOLD, 36)
        g2.color = textColor
        g2.drawString("$currentWeapon", x + 22, y + 36)

        // Draw small triangles indicating available weapons
        for (i in 1..5) {
            if (i == currentWeapon) {
                g2.color = Color(255, 255, 0) // Highlight selected
            } else {
                g2.color = Color(180, 180, 180)
            }

            // Small triangle indicator
            val triangle = java.awt.Polygon()
            val triangleX = x + 10 + (i-1) * 9
            triangle.addPoint(triangleX, y - 4)
            triangle.addPoint(triangleX + 5, y - 10)
            triangle.addPoint(triangleX + 10, y - 4)
            g2.fillPolygon(triangle)
        }

        // Restore original settings
        g2.font = originalFont
    }

    override fun clone(): UIComponent {
        val clone = WeaponSelectorComponent(x, y, width, height)
        clone.visible = visible
        clone.id = id
        clone.backgroundColor = backgroundColor
        clone.borderColor = borderColor
        clone.textColor = textColor
        clone.currentWeapon = currentWeapon
        return clone
    }
}