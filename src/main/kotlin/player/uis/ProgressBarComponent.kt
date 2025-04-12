package player.uis

import java.awt.Color
import java.awt.Graphics2D

// A standalone progress bar component that can be placed anywhere
class ProgressBarComponent(x: Int, y: Int, width: Int, height: Int) : UIComponent(x, y, width, height) {
    var barColor: Color = Color(215, 0, 0) // Default red for health
    var backgroundColor: Color = Color(15, 15, 15)
    var borderColor: Color = Color(0, 0, 0)
    var fillPercentage: Int = 100
    var showNotches: Boolean = true
    var notchCount: Int = 10

    override fun render(g2: Graphics2D, screenWidth: Int, screenHeight: Int) {
        if (!visible) return

        // Draw background
        g2.color = backgroundColor
        g2.fillRect(x, y, width, height)

        // Draw bar
        g2.color = barColor
        val fillWidth = (width * fillPercentage / 100).coerceIn(0, width)
        g2.fillRect(x, y, fillWidth, height)

        // Draw notches if enabled
        if (showNotches && notchCount > 0) {
            g2.color = borderColor
            for (i in 1 until notchCount) {
                val notchX = x + (width * i / notchCount)
                g2.drawLine(notchX, y, notchX, y + height)
            }
        }

        // Draw border
        g2.color = borderColor
        g2.drawRect(x, y, width, height)
    }

    override fun clone(): UIComponent {
        val clone = ProgressBarComponent(x, y, width, height)
        clone.visible = visible
        clone.id = id
        clone.barColor = barColor
        clone.backgroundColor = backgroundColor
        clone.borderColor = borderColor
        clone.fillPercentage = fillPercentage
        clone.showNotches = showNotches
        clone.notchCount = notchCount
        return clone
    }
}