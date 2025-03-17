package ui.components

import grideditor.GridEditor
import java.awt.Color
import java.awt.Dimension
import javax.swing.*

class WallPropertiesPanel : JPanel() {
    // Properties
    private var currentWallColor = Color(150, 0, 0)  // Default wall color
    private var currentWallHeight = 3.0
    private var currentWallWidth = 2.0

    // UI Components
    private val colorButton: JButton
    private val heightButton: JButton
    private val widthButton: JButton

    // Listeners
    private var onColorChange: ((Color) -> Unit)? = null

    // Reference to grid editor
    private var gridEditor: GridEditor? = null

    init {
        // Setup panel
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)

        // Create buttons
        colorButton = createColorButton("Wall Color", currentWallColor)
        heightButton = createHeightButton()
        widthButton = createWidthButton()

        // Add components
        add(colorButton)
        add(Box.createVerticalStrut(5))
        add(heightButton)
        add(Box.createVerticalStrut(5))
        add(widthButton)
    }

    // Interface for property change listener
    interface WallPropertyChangeListener {
        fun onWallColorChanged(color: Color)
        fun onWallHeightChanged(height: Double)
        fun onWallWidthChanged(width: Double)
    }

    // Property change listener
    private var propertyChangeListener: WallPropertyChangeListener? = null

    fun setWallPropertyChangeListener(listener: WallPropertyChangeListener) {
        propertyChangeListener = listener
    }

    fun setGridEditor(editor: GridEditor) {
        gridEditor = editor
    }

    fun updateProperties(color: Color?, height: Double?, width: Double?) {
        color?.let {
            currentWallColor = it
            colorButton.background = it
        }

        height?.let {
            currentWallHeight = it
            heightButton.text = "Wall Height: $it"
        }

        width?.let {
            currentWallWidth = it
            widthButton.text = "Wall Width: $it"
        }
    }

    private fun createColorButton(text: String, initialColor: Color): JButton {
        return JButton(text).apply {
            background = initialColor
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                val newColor = JColorChooser.showDialog(
                    this,
                    "Choose Wall Color",
                    background
                )
                if (newColor != null) {
                    background = newColor
                    currentWallColor = newColor
                    propertyChangeListener?.onWallColorChanged(newColor)
                    // Update selected cell if there is one
                    gridEditor?.updateSelectedCell(color = newColor)
                }
            }
        }
    }

    private fun createHeightButton(): JButton {
        return JButton("Wall Height: $currentWallHeight").apply {
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
                        propertyChangeListener?.onWallHeightChanged(newHeight)
                        gridEditor?.setWallHeight(currentWallHeight)
                        // Update selected cell if there is one
                        gridEditor?.updateSelectedCell(height = newHeight)
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
    }

    private fun createWidthButton(): JButton {
        return JButton("Wall Width: $currentWallWidth").apply {
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
                        propertyChangeListener?.onWallWidthChanged(newWidth)
                        gridEditor?.setWallWidth(currentWallWidth)
                        // Update selected cell if there is one
                        gridEditor?.updateSelectedCell(width = newWidth)
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
    }

    // Getters for current properties
    fun getCurrentWallColor(): Color = currentWallColor
    fun getCurrentWallHeight(): Double = currentWallHeight
    fun getCurrentWallWidth(): Double = currentWallWidth
}