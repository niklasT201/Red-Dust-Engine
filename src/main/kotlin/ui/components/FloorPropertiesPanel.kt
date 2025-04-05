package ui.components

import grideditor.GridEditor
import java.awt.Color
import java.awt.Dimension
import javax.swing.*

class FloorPropertiesPanel : JPanel() {
    // Properties
    private var currentFloorColor = Color(100, 100, 100)  // Default floor color
    private var currentFloorHeight = 0.0

    // UI Components
    private val colorButton: JButton
    private val heightButton: JButton

    // Reference to grid editor
    private var gridEditor: GridEditor? = null

    init {
        // Setup panel
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)

        // Create buttons
        colorButton = createColorButton("Floor Color", currentFloorColor)
        heightButton = createHeightButton()

        // Add components
        add(colorButton)
        add(Box.createVerticalStrut(5))
        add(heightButton)
    }

    // Interface for property change listener
    interface FloorPropertyChangeListener {
        fun onFloorColorChanged(color: Color)
        fun onFloorHeightChanged(height: Double)
    }

    // Property change listener
    private var propertyChangeListener: FloorPropertyChangeListener? = null

    fun setFloorPropertyChangeListener(listener: FloorPropertyChangeListener) {
        propertyChangeListener = listener
    }

    fun setGridEditor(editor: GridEditor) {
        gridEditor = editor
    }

    fun updateProperties(color: Color?, height: Double?) {
        color?.let {
            currentFloorColor = it
            colorButton.background = it
        }

        height?.let {
            currentFloorHeight = it
            heightButton.text = "Floor Height: $it"
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
                    "Choose Floor Color",
                    background
                )
                if (newColor != null) {
                    background = newColor
                    currentFloorColor = newColor
                    propertyChangeListener?.onFloorColorChanged(newColor)
                    // Update grid editor
                    gridEditor?.setFloorColor(newColor)
                }
            }
        }
    }

    private fun createHeightButton(): JButton {
        return JButton("Floor Y-Offset: $currentFloorHeight").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                val input = JOptionPane.showInputDialog(
                    this,
                    "Enter floor Y-offset (-5.0 to 5.0):",
                    currentFloorHeight
                )
                try {
                    val newHeight = input?.toDoubleOrNull()
                    if (newHeight != null && newHeight in -5.0..5.0) {
                        currentFloorHeight = newHeight
                        text = "Floor Y-Offset: $currentFloorHeight"
                        propertyChangeListener?.onFloorHeightChanged(newHeight)
                        gridEditor?.updateCurrentFloorHeight(currentFloorHeight)
                    }
                } catch (e: NumberFormatException) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Please enter a valid number between -5.0 and 5.0",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }

    // Getters for current properties
    fun getCurrentFloorColor(): Color = currentFloorColor
    fun getCurrentFloorHeight(): Double = currentFloorHeight
}