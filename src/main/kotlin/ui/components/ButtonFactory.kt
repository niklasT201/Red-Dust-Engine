package ui.components

import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import javax.swing.JButton

object ButtonFactory {
    // Default colors and styles
    val defaultButtonColor = Color(60, 63, 65)
    val selectedButtonColor = Color(100, 100, 255)

    fun createButton(text: String): JButton {
        return JButton(text).apply {
            background = defaultButtonColor
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
        }
    }

    fun createColorButton(text: String, initialColor: Color, onColorChange: (Color) -> Unit): JButton {
        return JButton(text).apply {
            background = initialColor
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                val newColor = javax.swing.JColorChooser.showDialog(
                    this,
                    "Choose Wall Color",
                    background
                )
                if (newColor != null) {
                    background = newColor
                    onColorChange(newColor)
                }
            }
        }
    }

    // Add other button creation methods here
}