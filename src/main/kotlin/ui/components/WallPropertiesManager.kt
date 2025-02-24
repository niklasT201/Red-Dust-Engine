package ui.components

import grideditor.GridEditor
import ui.components.ButtonFactory
import java.awt.Color
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JOptionPane
import javax.swing.JPanel

class WallPropertiesManager(private val gridEditor: GridEditor) {
    var currentWallColor = Color(150, 0, 0)  // Default wall color
    var currentWallHeight = 3.0
    var currentWallWidth = 2.0

    lateinit var colorButton: JButton
    lateinit var heightButton: JButton
    lateinit var widthButton: JButton

    fun createColorButton(): JButton {
        colorButton = ButtonFactory.createColorButton("Wall Color", currentWallColor) { newColor ->
            currentWallColor = newColor
            gridEditor.updateSelectedCell(color = newColor)
        }
        return colorButton
    }

    fun createHeightButton(): JButton {
        heightButton = JButton("Wall Height: $currentWallHeight").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                val input = JOptionPane.showInputDialog(
                    this,
                    "Enter wall height (0.5 - 10.0):",
                    currentWallHeight
                )
                try {
                    val newHeight = input?.toDoubleOrNull()
                    if (newHeight != null && newHeight in 0.5..10.0) {
                        currentWallHeight = newHeight
                        text = "Wall Height: $currentWallHeight"
                        gridEditor.setWallHeight(currentWallHeight)
                        gridEditor.updateSelectedCell(height = newHeight)
                    }
                } catch (e: NumberFormatException) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Please enter a valid number between 0.5 and 10.0",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
        return heightButton
    }

    fun createWidthButton(): JButton {
        widthButton = JButton("Wall Width: $currentWallWidth").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                val input = JOptionPane.showInputDialog(
                    this,
                    "Enter wall width (0.5 - 5.0):",
                    currentWallWidth
                )
                try {
                    val newWidth = input?.toDoubleOrNull()
                    if (newWidth != null && newWidth in 0.5..5.0) {
                        currentWallWidth = newWidth
                        text = "Wall Width: $currentWallWidth"
                        gridEditor.setWallWidth(currentWallWidth)
                        gridEditor.updateSelectedCell(width = newWidth)
                    }
                } catch (e: NumberFormatException) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Please enter a valid number between 0.5 and 5.0",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
        return widthButton
    }

    fun updateProperties(color: Color? = null, height: Double? = null, width: Double? = null) {
        if (color != null) {
            colorButton.background = color
            currentWallColor = color
        }

        if (height != null) {
            heightButton.text = "Wall Height: $height"
            currentWallHeight = height
        }

        if (width != null) {
            widthButton.text = "Wall Width: $width"
            currentWallWidth = width
        }
    }
}