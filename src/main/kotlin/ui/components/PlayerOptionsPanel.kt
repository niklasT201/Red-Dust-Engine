package ui.components

import Game3D
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.border.TitledBorder

class PlayerOptionsPanel(private val game3D: Game3D) : JPanel() {
    private val crosshairVisibleCheckbox = JCheckBox("Show Crosshair")
    private val customSizeTrack = CrosshairSizeTrack(5, 30, 10)

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(50, 52, 55)
        border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color(70, 73, 75)),
            "Player Display",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            null,
            Color.WHITE
        )

        // Initialize checkbox with current state
        crosshairVisibleCheckbox.isSelected = game3D.isCrosshairVisible()
        crosshairVisibleCheckbox.foreground = Color.WHITE
        crosshairVisibleCheckbox.background = Color(50, 52, 55)

        // Initialize custom size track with current size
        customSizeTrack.value = game3D.getCrosshairSize()

        // Create a container for the checkbox with proper alignment
        val checkboxPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        checkboxPanel.background = Color(50, 52, 55)
        checkboxPanel.add(crosshairVisibleCheckbox)

        add(checkboxPanel)

        // Create a panel for the track with a label
        val trackPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        trackPanel.background = Color(50, 52, 55)
        trackPanel.add(JLabel("Size:").apply { foreground = Color.WHITE })
        trackPanel.add(customSizeTrack)
        add(trackPanel)

        // Set up crosshair visibility toggle
        crosshairVisibleCheckbox.addActionListener {
            game3D.setCrosshairVisible(crosshairVisibleCheckbox.isSelected)
        }

        // Set up value change listener for custom track
        customSizeTrack.addChangeListener { newValue ->
            game3D.setCrosshairSize(newValue)
        }

        // Force sync the state at initialization
        SwingUtilities.invokeLater {
            crosshairVisibleCheckbox.isSelected = game3D.isCrosshairVisible()
            customSizeTrack.value = game3D.getCrosshairSize()
        }
    }

    // Custom component for size selection that allows direct clicking
    private inner class CrosshairSizeTrack(
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

        private val trackHeight = 20  // Taller track to accommodate the text
        private val markerWidth = 3    // Slim vertical marker instead of knob
        private val changeListeners = mutableListOf<(Int) -> Unit>()

        init {
            preferredSize = Dimension(150, trackHeight)
            background = Color(50, 52, 55)

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
            for (i in min..max step 5) {
                val tickX = ((i - min).toDouble() / (max - min) * width).toInt()
                g2.drawLine(tickX, 2, tickX, trackHeight - 2)
            }

            // Draw marker at current position (slim vertical line)
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