package player.uis

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle

// A simple rectangle background component that can be placed independently
class BackgroundComponent(x: Int, y: Int, width: Int, height: Int) : UIComponent(x, y, width, height) {
    var backgroundColor: Color = Color(30, 30, 30)
    var borderColor: Color = Color(80, 80, 80)
    var borderThickness: Int = 1
    var cornerRadius: Int = 0 // For rounded corners

    override fun render(g2: Graphics2D, screenWidth: Int, screenHeight: Int) {
        if (!visible) return

        // Draw panel background
        g2.color = backgroundColor
        if (cornerRadius > 0) {
            g2.fillRoundRect(x, y, width, height, cornerRadius, cornerRadius)
        } else {
            g2.fillRect(x, y, width, height)
        }

        // Draw borders
        g2.color = borderColor
        if (cornerRadius > 0) {
            g2.drawRoundRect(x, y, width, height, cornerRadius, cornerRadius)
        } else {
            g2.drawRect(x, y, width, height)
        }
    }

    override fun clone(): UIComponent {
        val clone = BackgroundComponent(x, y, width, height)
        clone.visible = visible
        clone.id = id
        clone.backgroundColor = backgroundColor
        clone.borderColor = borderColor
        clone.borderThickness = borderThickness
        clone.cornerRadius = cornerRadius
        return clone
    }
}