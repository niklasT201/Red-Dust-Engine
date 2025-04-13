package player.uis.components

import player.uis.UIComponent
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D

// A component for displaying game stats (kills, items, secrets)
class StatComponent(x: Int, y: Int, width: Int, height: Int) : UIComponent(x, y, width, height) {
    var statType: String = "kills" // kills, items, secrets, armor
    var currentValue: Int = 0
    var maxValue: Int = 0
    var textColor: Color = Color(215, 186, 69)
    var showAsPercentage: Boolean = false

    override fun render(g2: Graphics2D, screenWidth: Int, screenHeight: Int) {
        if (!visible) return

        // Save original settings
        val originalFont = g2.font

        // Draw text
        g2.font = Font("Courier New", Font.BOLD, 14)
        g2.color = textColor

        val label = when(statType) {
            "kills" -> "K:"
            "items" -> "I:"
            "secrets" -> "S:"
            "armor" -> "ARMOR"
            "health" -> "Health"
            else -> statType.uppercase() + ":"
        }

        val displayValue = if (showAsPercentage) {
            val percentage = if (maxValue > 0) (currentValue * 100 / maxValue) else 0
            "$percentage%"
        } else {
            "$currentValue/$maxValue"
        }

        g2.drawString("$label $displayValue", x, y + 14)

        // Restore original settings
        g2.font = originalFont
    }

    override fun clone(): UIComponent {
        val clone = StatComponent(x, y, width, height)
        clone.visible = visible
        clone.id = id
        clone.statType = statType
        clone.currentValue = currentValue
        clone.maxValue = maxValue
        clone.textColor = textColor
        clone.showAsPercentage = showAsPercentage
        return clone
    }
}