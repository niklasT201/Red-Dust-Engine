package ui.components

import Direction
import grideditor.GridEditor
import java.awt.Color
import java.awt.Dimension
import javax.swing.*

class RampPropertiesPanel : JPanel() {
    // Properties
    private var currentRampColor = Color(150, 150, 150)  // Default ramp color
    private var currentRampHeight = 3.0
    private var currentRampWidth = 2.0
    private var currentSlopeDirection = Direction.NORTH

    // UI Components
    private val colorButton: JButton
    private val heightButton: JButton
    private val widthButton: JButton
    private val directionButton: JButton

    // Reference to grid editor
    private var gridEditor: GridEditor? = null

    init {
        // Setup panel
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)

        // Create buttons
        colorButton = createColorButton("Ramp Color", currentRampColor)
        heightButton = createHeightButton()
        widthButton = createWidthButton()
        directionButton = createDirectionButton()

        // Add components
        add(colorButton)
        add(Box.createVerticalStrut(5))
        add(heightButton)
        add(Box.createVerticalStrut(5))
        add(widthButton)
        add(Box.createVerticalStrut(5))
        add(directionButton)
    }

    // Interface for property change listener
    interface RampPropertyChangeListener {
        fun onRampColorChanged(color: Color)
        fun onRampHeightChanged(height: Double)
        fun onRampWidthChanged(width: Double)
        fun onRampDirectionChanged(direction: Direction)
    }

    // Property change listener
    private var propertyChangeListener: RampPropertyChangeListener? = null

    fun setRampPropertyChangeListener(listener: RampPropertyChangeListener) {
        propertyChangeListener = listener
    }

    fun setGridEditor(editor: GridEditor) {
        gridEditor = editor
    }

    fun updateProperties(color: Color?, height: Double?, width: Double?, direction: Direction? = null) {
        color?.let {
            currentRampColor = it
            colorButton.background = it
        }

        height?.let {
            currentRampHeight = it
            heightButton.text = "Ramp Height: $it"
        }

        width?.let {
            currentRampWidth = it
            widthButton.text = "Ramp Width: $it"
        }

        direction?.let {
            currentSlopeDirection = it
            directionButton.text = "Slope Direction: ${it.name}"
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
                    "Choose Ramp Color",
                    background
                )
                if (newColor != null) {
                    background = newColor
                    currentRampColor = newColor
                    propertyChangeListener?.onRampColorChanged(newColor)
                    // Update grid editor
                    gridEditor?.setRampColor(newColor)
                }
            }
        }
    }

    private fun createHeightButton(): JButton {
        return JButton("Ramp Height: $currentRampHeight").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                val input = JOptionPane.showInputDialog(
                    this,
                    "Enter ramp height (0.5 - 10.0):",
                    currentRampHeight
                )
                try {
                    val newHeight = input?.toDoubleOrNull()
                    if (newHeight != null && newHeight in 0.5..10.0) {
                        currentRampHeight = newHeight
                        text = "Ramp Height: $currentRampHeight"
                        propertyChangeListener?.onRampHeightChanged(newHeight)
                        gridEditor?.setRampHeight(currentRampHeight)
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
        return JButton("Ramp Width: $currentRampWidth").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                val input = JOptionPane.showInputDialog(
                    this,
                    "Enter ramp width (0.5 - 5.0):",
                    currentRampWidth
                )
                try {
                    val newWidth = input?.toDoubleOrNull()
                    if (newWidth != null && newWidth in 0.5..5.0) {
                        currentRampWidth = newWidth
                        text = "Ramp Width: $currentRampWidth"
                        propertyChangeListener?.onRampWidthChanged(newWidth)
                        gridEditor?.setRampWidth(currentRampWidth)
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

    private fun createDirectionButton(): JButton {
        return JButton("Slope Direction: ${currentSlopeDirection.name}").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                // Create a simple dialog with direction options
                val directions = Direction.values()
                val selection = JOptionPane.showInputDialog(
                    this,
                    "Select slope direction:",
                    "Slope Direction",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    directions.map { it.name }.toTypedArray(),
                    currentSlopeDirection.name
                ) as String?

                selection?.let {
                    val newDirection = Direction.valueOf(it)
                    currentSlopeDirection = newDirection
                    text = "Slope Direction: ${newDirection.name}"
                    propertyChangeListener?.onRampDirectionChanged(newDirection)
                    gridEditor?.currentSlopeDirection = newDirection
                }
            }
        }
    }

    // Getters for current properties
    fun getCurrentRampColor(): Color = currentRampColor
    fun getCurrentRampHeight(): Double = currentRampHeight
    fun getCurrentRampWidth(): Double = currentRampWidth
    fun getCurrentSlopeDirection(): Direction = currentSlopeDirection
}