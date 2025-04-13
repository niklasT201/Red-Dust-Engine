package ui.components

import grideditor.GridEditor
import java.awt.Color
import java.awt.Dimension
import javax.swing.*

class WaterPropertiesPanel : JPanel() {
    // Properties
    private var currentWaterColor = Color(0, 105, 148, 200)  // Default semi-transparent blue
    private var currentFloorHeight = 0.0
    private var currentDepth = 2.0
    private var currentWaveHeight = 0.1
    private var currentWaveSpeed = 1.0
    private var currentDamagePerSecond = 0.0

    // UI Components
    private val colorButton: JButton
    private val depthButton: JButton
    private val waveHeightButton: JButton
    private val waveSpeedButton: JButton
    private val damageButton: JButton

    // Reference to grid editor
    private var gridEditor: GridEditor? = null

    init {
        // Setup panel
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)

        // Create buttons
        colorButton = createColorButton("Water Color", currentWaterColor)
        depthButton = createDepthButton()
        waveHeightButton = createWaveHeightButton()
        waveSpeedButton = createWaveSpeedButton()
        damageButton = createDamageButton()

        // Add components
        add(colorButton)
        add(Box.createVerticalStrut(5))
        add(depthButton)
        add(Box.createVerticalStrut(5))
        add(waveHeightButton)
        add(Box.createVerticalStrut(5))
        add(waveSpeedButton)
        add(Box.createVerticalStrut(5))
        add(damageButton)
    }

    // Interface for property change listener
    interface WaterPropertyChangeListener {
        fun onWaterColorChanged(color: Color)
        fun onFloorHeightChanged(height: Double)
        fun onDepthChanged(depth: Double)
        fun onWaveHeightChanged(height: Double)
        fun onWaveSpeedChanged(speed: Double)
        fun onDamagePerSecondChanged(damage: Double)
    }

    // Property change listener
    private var propertyChangeListener: WaterPropertyChangeListener? = null

    fun setWaterPropertyChangeListener(listener: WaterPropertyChangeListener) {
        propertyChangeListener = listener
    }

    fun setGridEditor(editor: GridEditor) {
        gridEditor = editor
    }

    fun updateProperties(
        color: Color? = null,
        depth: Double? = null,
        waveHeight: Double? = null,
        waveSpeed: Double? = null,
        damagePerSecond: Double? = null
    ) {
        color?.let {
            currentWaterColor = it
            colorButton.background = it
        }

        depth?.let {
            currentDepth = it
            depthButton.text = "Water Depth: $it"
        }

        waveHeight?.let {
            currentWaveHeight = it
            waveHeightButton.text = "Wave Height: $it"
        }

        waveSpeed?.let {
            currentWaveSpeed = it
            waveSpeedButton.text = "Wave Speed: $it"
        }

        damagePerSecond?.let {
            currentDamagePerSecond = it
            damageButton.text = "Damage/Second: $it"
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
                    "Choose Water Color",
                    background
                )
                if (newColor != null) {
                    // Create a new color with transparency if it doesn't have it
                    val finalColor = if (newColor.alpha == 255) {
                        Color(newColor.red, newColor.green, newColor.blue, 200) // Default transparency
                    } else {
                        newColor
                    }

                    background = finalColor
                    currentWaterColor = finalColor
                    propertyChangeListener?.onWaterColorChanged(finalColor)
                    // Update selected cell if there is one
                    gridEditor?.updateSelectedWater(color = finalColor)
                }
            }
        }
    }

    private fun createFloorHeightButton(): JButton {
        return JButton("Floor Height: $currentFloorHeight").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                val input = JOptionPane.showInputDialog(
                    this,
                    "Enter floor height (0.0 - 10.0):",
                    currentFloorHeight
                )
                try {
                    val newHeight = input?.toDoubleOrNull()
                    if (newHeight != null && newHeight in 0.0..10.0) {
                        currentFloorHeight = newHeight
                        text = "Floor Height: $currentFloorHeight"
                        propertyChangeListener?.onFloorHeightChanged(newHeight)
                        gridEditor?.updateSelectedWater(floorHeight = newHeight)
                    }
                } catch (e: NumberFormatException) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Please enter a valid number between 0.0 and 10.0",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }

    private fun createDepthButton(): JButton {
        return JButton("Water Depth: $currentDepth").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                val input = JOptionPane.showInputDialog(
                    this,
                    "Enter water depth (0.5 - 10.0):",
                    currentDepth
                )
                try {
                    val newDepth = input?.toDoubleOrNull()
                    if (newDepth != null && newDepth in 0.5..10.0) {
                        currentDepth = newDepth
                        text = "Water Depth: $currentDepth"
                        propertyChangeListener?.onDepthChanged(newDepth)
                        gridEditor?.updateSelectedWater(depth = newDepth)
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

    private fun createWaveHeightButton(): JButton {
        return JButton("Wave Height: $currentWaveHeight").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                val input = JOptionPane.showInputDialog(
                    this,
                    "Enter wave height (0.0 - 1.0):",
                    currentWaveHeight
                )
                try {
                    val newHeight = input?.toDoubleOrNull()
                    if (newHeight != null && newHeight in 0.0..1.0) {
                        currentWaveHeight = newHeight
                        text = "Wave Height: $currentWaveHeight"
                        propertyChangeListener?.onWaveHeightChanged(newHeight)
                        gridEditor?.updateSelectedWater(waveHeight = newHeight)
                    }
                } catch (e: NumberFormatException) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Please enter a valid number between 0.0 and 1.0",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }

    private fun createWaveSpeedButton(): JButton {
        return JButton("Wave Speed: $currentWaveSpeed").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                val input = JOptionPane.showInputDialog(
                    this,
                    "Enter wave speed (0.0 - 5.0):",
                    currentWaveSpeed
                )
                try {
                    val newSpeed = input?.toDoubleOrNull()
                    if (newSpeed != null && newSpeed in 0.0..5.0) {
                        currentWaveSpeed = newSpeed
                        text = "Wave Speed: $currentWaveSpeed"
                        propertyChangeListener?.onWaveSpeedChanged(newSpeed)
                        gridEditor?.updateSelectedWater(waveSpeed = newSpeed)
                    }
                } catch (e: NumberFormatException) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Please enter a valid number between 0.0 and 5.0",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }

    private fun createDamageButton(): JButton {
        return JButton("Damage/Second: $currentDamagePerSecond").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener {
                val input = JOptionPane.showInputDialog(
                    this,
                    "Enter damage per second (0.0 - 100.0):",
                    currentDamagePerSecond
                )
                try {
                    val newDamage = input?.toDoubleOrNull()
                    if (newDamage != null && newDamage in 0.0..100.0) {
                        currentDamagePerSecond = newDamage
                        text = "Damage/Second: $currentDamagePerSecond"
                        propertyChangeListener?.onDamagePerSecondChanged(newDamage)
                        gridEditor?.updateSelectedWater(damagePerSecond = newDamage)
                    }
                } catch (e: NumberFormatException) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Please enter a valid number between 0.0 and 100.0",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }

    // Getters for current properties
    fun getCurrentWaterColor(): Color = currentWaterColor
    fun getCurrentFloorHeight(): Double = currentFloorHeight
    fun getCurrentDepth(): Double = currentDepth
    fun getCurrentWaveHeight(): Double = currentWaveHeight
    fun getCurrentWaveSpeed(): Double = currentWaveSpeed
    fun getCurrentDamagePerSecond(): Double = currentDamagePerSecond
}