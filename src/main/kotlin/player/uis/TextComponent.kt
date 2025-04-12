package player.uis

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D

// A standalone text component that can be placed anywhere
class TextComponent(x: Int, y: Int, width: Int, height: Int) : UIComponent(x, y, width, height) {
    var text: String = "Text"
    var textColor: Color = Color(215, 186, 69)
    var fontSize: Int = 14
    var fontStyle: Int = Font.BOLD
    var fontName: String = "Courier New"

    override fun render(g2: Graphics2D, screenWidth: Int, screenHeight: Int) {
        if (!visible) return

        // Save original settings
        val originalFont = g2.font

        // Draw text
        g2.font = Font(fontName, fontStyle, fontSize)
        g2.color = textColor
        g2.drawString(text, x, y + fontSize) // Position text using baseline

        // Restore original settings
        g2.font = originalFont
    }

    override fun clone(): UIComponent {
        val clone = TextComponent(x, y, width, height)
        clone.visible = visible
        clone.id = id
        clone.text = text
        clone.textColor = textColor
        clone.fontSize = fontSize
        clone.fontStyle = fontStyle
        clone.fontName = fontName
        return clone
    }
}