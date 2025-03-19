package ui.components

import Game3D
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.border.TitledBorder

class PlayerOptionsPanel(private val game3D: Game3D) : JPanel() {
    private val crosshairVisibleCheckbox = JCheckBox("Show Crosshair")
    private val crosshairSizeLabel = JLabel("Size: 10")
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
        crosshairSizeLabel.text = "Size: ${game3D.getCrosshairSize()}"
        crosshairSizeLabel.foreground = Color.WHITE

        // Create a container for the checkbox with proper alignment
        val checkboxPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        checkboxPanel.background = Color(50, 52, 55)
        checkboxPanel.add(crosshairVisibleCheckbox)

        add(checkboxPanel)

        // Create a panel for the track and its label
        val trackPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        trackPanel.background = Color(50, 52, 55)
        trackPanel.add(crosshairSizeLabel)
        trackPanel.add(customSizeTrack)
        add(trackPanel)

        // Set up crosshair visibility toggle
        crosshairVisibleCheckbox.addActionListener {
            game3D.setCrosshairVisible(crosshairVisibleCheckbox.isSelected)
        }

        // Set up value change listener for custom track
        customSizeTrack.addChangeListener { newValue ->
            crosshairSizeLabel.text = "Size: $newValue"
            game3D.setCrosshairSize(newValue)
        }

        // Force sync the state at initialization
        SwingUtilities.invokeLater {
            crosshairVisibleCheckbox.isSelected = game3D.isCrosshairVisible()
            customSizeTrack.value = game3D.getCrosshairSize()
            crosshairSizeLabel.text = "Size: ${game3D.getCrosshairSize()}"
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

        private val trackHeight = 8
        private val knobSize = 14
        private val changeListeners = mutableListOf<(Int) -> Unit>()

        init {
            preferredSize = Dimension(150, knobSize + 4)
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
            val trackWidth = width - knobSize
            val relativeX = (x - knobSize / 2).coerceIn(0, trackWidth)
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

            val trackWidth = width - knobSize
            val trackY = height / 2 - trackHeight / 2

            // Draw track background
            g2.color = Color(30, 32, 34)
            g2.fillRoundRect(knobSize / 2, trackY, trackWidth, trackHeight, trackHeight, trackHeight)

            // Draw filled portion of track
            val fillWidth = ((value - min).toDouble() / (max - min) * trackWidth).toInt()
            g2.color = Color(100, 149, 237) // Cornflower blue
            g2.fillRoundRect(knobSize / 2, trackY, fillWidth, trackHeight, trackHeight, trackHeight)

            // Draw tick marks
            g2.color = Color(80, 82, 85)
            for (i in min..max step 5) {
                val tickX = knobSize / 2 + ((i - min).toDouble() / (max - min) * trackWidth).toInt()
                g2.drawLine(tickX, trackY - 2, tickX, trackY + trackHeight + 2)
            }

            // Draw knob
            val knobX = knobSize / 2 + ((value - min).toDouble() / (max - min) * trackWidth).toInt() - knobSize / 2
            val knobY = height / 2 - knobSize / 2

            // Knob shadow
            g2.color = Color(20, 20, 20, 100)
            g2.fillOval(knobX + 1, knobY + 1, knobSize, knobSize)

            // Knob body
            g2.color = Color(220, 220, 220)
            g2.fillOval(knobX, knobY, knobSize, knobSize)

            // Knob border
            g2.color = Color(180, 180, 180)
            g2.drawOval(knobX, knobY, knobSize, knobSize)
        }
    }
}