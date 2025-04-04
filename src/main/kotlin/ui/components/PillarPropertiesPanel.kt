package ui.components

import grideditor.GridEditor
import java.awt.Color
import java.awt.Dimension
import javax.swing.*

class PillarPropertiesPanel : JPanel() {
    // Properties
    private var currentPillarColor = Color(180, 170, 150)  // Default pillar color
    private var currentPillarHeight = 4.0
    private var currentPillarWidth = 1.0

    // UI Components
    private val colorButton: JButton
    private val heightButton: JButton
    private val widthButton: JButton

    // Reference to grid editor
    private var gridEditor: GridEditor? = null

    init {
        // Setup panel
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)

        // Create buttons
        colorButton = createColorButton("Pillar Color", currentPillarColor)
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
    interface PillarPropertyChangeListener {
        fun onPillarColorChanged(color: Color)
        fun onPillarHeightChanged(height: Double)
        fun onPillarWidthChanged(width: Double)
    }

    // Property change listener
    private var propertyChangeListener: PillarPropertyChangeListener? = null

    fun setPillarPropertyChangeListener(listener: PillarPropertyChangeListener) {
        propertyChangeListener = listener
    }

    fun setGridEditor(editor: GridEditor) {
        gridEditor = editor
    }

    fun updateProperties(color: Color?, height: Double?, width: Double?) {
        color?.let {
            currentPillarColor = it
            colorButton.background = it
        }

        height?.let {
            currentPillarHeight = it
            heightButton.text = "Pillar Height: $it"
        }

        width?.let {
            currentPillarWidth = it
            widthButton.text = "Pillar Width: $it"
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
                    "Choose Pillar Color",
                    background
                )
                if (newColor != null) {
                    background = newColor
                    currentPillarColor = newColor
                    propertyChangeListener?.onPillarColorChanged(newColor)
                    // Update grid editor
                    gridEditor?.setPillarColor(newColor)
                }
            }
        }
    }

    private fun createHeightButton(): JButton {
        return JButton("Pillar Height: $currentPillarHeight").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                val input = JOptionPane.showInputDialog(
                    this,
                    "Enter pillar height (0.5 - 10.0):",
                    currentPillarHeight
                )
                try {
                    val newHeight = input?.toDoubleOrNull()
                    if (newHeight != null && newHeight in 0.5..10.0) {
                        currentPillarHeight = newHeight
                        text = "Pillar Height: $currentPillarHeight"
                        propertyChangeListener?.onPillarHeightChanged(newHeight)
                        gridEditor?.setPillarHeight(currentPillarHeight)
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
        return JButton("Pillar Width: $currentPillarWidth").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                val input = JOptionPane.showInputDialog(
                    this,
                    "Enter pillar width (0.5 - 5.0):",
                    currentPillarWidth
                )
                try {
                    val newWidth = input?.toDoubleOrNull()
                    if (newWidth != null && newWidth in 0.5..5.0) {
                        currentPillarWidth = newWidth
                        text = "Pillar Width: $currentPillarWidth"
                        propertyChangeListener?.onPillarWidthChanged(newWidth)
                        gridEditor?.setPillarWidth(currentPillarWidth)
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
    fun getCurrentPillarColor(): Color = currentPillarColor
    fun getCurrentPillarHeight(): Double = currentPillarHeight
    fun getCurrentPillarWidth(): Double = currentPillarWidth
}