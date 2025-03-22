package ui.components

import Renderer
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

/**
 * Panel for customizing border style settings (color, thickness, visibility)
 * with responsive design and custom slider matching the game's style
 */
class BorderStylePanel(private val renderer: Renderer) : JPanel(), ChangeListener {

    private val enableBordersCheckbox = JCheckBox("Enable Borders")
    private val borderThicknessTrack = CustomSizeTrack(1, 5, renderer.borderThickness.toInt())
    private val borderColorButton = JButton("Select")
    private val colorPreview = JPanel()

    // Colors - matching the PlayerOptionsPanel
    private val backgroundColor = Color(50, 52, 55)
    private val darkBackgroundColor = Color(40, 42, 45)
    private val borderColor = Color(70, 73, 75)
    private val textColor = Color.WHITE

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = backgroundColor
        border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(borderColor),
            "Border Style",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            null,
            textColor
        )

        // Checkbox Panel
        val checkboxPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        checkboxPanel.background = backgroundColor
        enableBordersCheckbox.foreground = textColor
        enableBordersCheckbox.background = backgroundColor
        enableBordersCheckbox.isSelected = renderer.drawBorders
        checkboxPanel.add(enableBordersCheckbox)
        add(checkboxPanel)

        // Thickness Panel
        val thicknessPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        thicknessPanel.background = backgroundColor
        thicknessPanel.add(JLabel("Thickness:").apply { foreground = textColor })
        thicknessPanel.add(borderThicknessTrack)
        add(thicknessPanel)

        // Color Panel
        val colorPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        colorPanel.background = backgroundColor
        colorPanel.add(JLabel("Color:").apply { foreground = textColor })

        // Create a container for color preview and button
        val colorControlPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        colorControlPanel.background = backgroundColor

        // Configure color preview
        colorPreview.background = renderer.borderColor
        colorPreview.preferredSize = Dimension(24, 24)
        colorPreview.border = BorderFactory.createLineBorder(Color.WHITE)
        colorControlPanel.add(colorPreview)

        // Configure color button
        borderColorButton.background = darkBackgroundColor
        borderColorButton.foreground = textColor
        borderColorButton.preferredSize = Dimension(110, 28)
        borderColorButton.isFocusPainted = false
        colorControlPanel.add(borderColorButton)

        colorPanel.add(colorControlPanel)
        add(colorPanel)

        // Add some padding at the bottom
        add(Box.createVerticalStrut(5))

        // Set up event handlers
        enableBordersCheckbox.addChangeListener(this)
        borderThicknessTrack.addChangeListener { newValue ->
            renderer.borderThickness = newValue.toFloat()
            renderer.repaint()
        }
        borderColorButton.addActionListener {
            val selectedColor = JColorChooser.showDialog(
                this,
                "Choose Border Color",
                renderer.borderColor
            )
            if (selectedColor != null) {
                renderer.borderColor = selectedColor
                colorPreview.background = selectedColor
                renderer.repaint()
            }
        }
    }

    override fun stateChanged(e: ChangeEvent) {
        when (e.source) {
            enableBordersCheckbox -> {
                renderer.drawBorders = enableBordersCheckbox.isSelected
                renderer.repaint()
            }
        }
    }

    fun refreshFromGameState() {
        // Update UI components to match the renderer state
        enableBordersCheckbox.isSelected = renderer.drawBorders
        borderThicknessTrack.value = renderer.borderThickness.toInt()
        colorPreview.background = renderer.borderColor
    }

    /**
     * Custom slider component matching the CrosshairSizeTrack from PlayerOptionsPanel
     */
    private inner class CustomSizeTrack(
        private val min: Int,
        private val max: Int,
        initialValue: Int
    ) : JPanel() {
        var value: Int = initialValue
            set(newValue) {
                val clampedValue = newValue.coerceIn(min, max)
                if (field != clampedValue) {
                    field = clampedValue
                    repaint()
                    changeListeners.forEach { it(field) }
                }
            }

        private val trackHeight = 20
        private val markerWidth = 3
        private val changeListeners = mutableListOf<(Int) -> Unit>()

        init {
            preferredSize = Dimension(130, trackHeight)
            background = backgroundColor

            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    updateValueFromMouse(e.x)
                }
            })

            addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseDragged(e: MouseEvent) {
                    updateValueFromMouse(e.x)
                }
            })
        }

        private fun updateValueFromMouse(x: Int) {
            val trackWidth = width
            val relativeX = x.coerceIn(0, trackWidth)
            val newValue = min + ((max - min) * relativeX.toDouble() / trackWidth).toInt()
            value = newValue
        }

        fun addChangeListener(listener: (Int) -> Unit) {
            changeListeners.add(listener)
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // Draw track background
            g2.color = Color(30, 32, 34)
            g2.fillRoundRect(0, 0, width, trackHeight, 8, 8)

            // Draw filled portion of track
            val fillWidth = ((value - min).toDouble() / (max - min) * width).toInt()
            g2.color = Color(100, 149, 237) // Cornflower blue
            g2.fillRoundRect(0, 0, fillWidth, trackHeight, 8, 8)

            // Draw tick marks
            g2.color = Color(200, 200, 200, 120)
            for (i in min..max) {
                val tickX = ((i - min).toDouble() / (max - min) * width).toInt()
                g2.drawLine(tickX, 2, tickX, trackHeight - 2)
            }

            // Draw marker at current position
            val markerX = ((value - min).toDouble() / (max - min) * width).toInt() - markerWidth / 2
            g2.color = Color.WHITE
            g2.fillRect(markerX, 0, markerWidth, trackHeight)

            // Draw value text centered in the track
            g2.font = Font("SansSerif", Font.BOLD, 12)
            val valueText = value.toString()
            val textWidth = g2.fontMetrics.stringWidth(valueText)
            val textHeight = g2.fontMetrics.height

            // Center text in the track
            val textX = (width - textWidth) / 2
            val textY = (trackHeight + textHeight) / 2 - 2

            // Draw text shadow for better readability
            g2.color = Color(0, 0, 0, 150)
            g2.drawString(valueText, textX + 1, textY + 1)

            // Draw text in white
            g2.color = Color.WHITE
            g2.drawString(valueText, textX, textY)
        }
    }
}