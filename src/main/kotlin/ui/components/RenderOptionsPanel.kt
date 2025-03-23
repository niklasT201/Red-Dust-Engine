package ui.components

import Renderer
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.border.TitledBorder

class RenderOptionsPanel(private val renderer: Renderer) : JPanel() {
    // Default values for reset functionality
    private val defaultRenderDistance = renderer.maxRenderDistance.toInt()
    private val defaultShadowDistance = renderer.shadowDistance.toInt()
    private val defaultShadowIntensity = (renderer.shadowIntensity * 100).toInt()
    private val defaultAmbientLight = (renderer.ambientLight * 100).toInt()
    private val defaultEnableRenderDistance = renderer.enableRenderDistance
    private val defaultEnableShadows = renderer.enableShadows

    // Render distance components
    private val enableRenderDistanceCheckbox = JCheckBox("Enable Render Distance").apply {
        isSelected = renderer.enableRenderDistance
        background = Color(40, 44, 52)
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
        addActionListener {
            renderer.enableRenderDistance = isSelected
            maxRenderDistanceTrack.isEnabled = isSelected
            renderer.repaint()
        }
    }

    private val maxRenderDistanceLabel = JLabel("Max Render Distance:").apply {
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
    }

    private val maxRenderDistanceValue = JLabel("${renderer.maxRenderDistance.toInt()} units").apply {
        foreground = Color.WHITE
        alignmentX = Component.RIGHT_ALIGNMENT
    }

    private val maxRenderDistanceTrack = CustomTrack(5, 100, renderer.maxRenderDistance.toInt()).apply {
        background = Color(40, 44, 52)
        alignmentX = Component.LEFT_ALIGNMENT
        addChangeListener { newValue ->
            renderer.maxRenderDistance = newValue.toDouble()
            maxRenderDistanceValue.text = "$newValue units"
            renderer.repaint()
        }
    }

    // Shadow components
    private val enableShadowsCheckbox = JCheckBox("Enable Shadows").apply {
        isSelected = renderer.enableShadows
        background = Color(40, 44, 52)
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
        addActionListener {
            renderer.enableShadows = isSelected
            shadowDistanceTrack.isEnabled = isSelected
            shadowIntensityTrack.isEnabled = isSelected
            ambientLightTrack.isEnabled = isSelected
            renderer.repaint()
        }
    }

    private val shadowDistanceLabel = JLabel("Shadow Distance:").apply {
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
    }

    private val shadowDistanceValue = JLabel("${renderer.shadowDistance.toInt()} units").apply {
        foreground = Color.WHITE
        alignmentX = Component.RIGHT_ALIGNMENT
    }

    private val shadowDistanceTrack = CustomTrack(5, 50, renderer.shadowDistance.toInt()).apply {
        background = Color(40, 44, 52)
        alignmentX = Component.LEFT_ALIGNMENT
        addChangeListener { newValue ->
            renderer.shadowDistance = newValue.toDouble()
            shadowDistanceValue.text = "$newValue units"
            renderer.repaint()
        }
    }

    private val shadowIntensityLabel = JLabel("Shadow Intensity:").apply {
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
    }

    private val shadowIntensityValue = JLabel("${(renderer.shadowIntensity * 100).toInt()}%").apply {
        foreground = Color.WHITE
        alignmentX = Component.RIGHT_ALIGNMENT
    }

    private val shadowIntensityTrack = CustomTrack(0, 100, (renderer.shadowIntensity * 100).toInt()).apply {
        background = Color(40, 44, 52)
        alignmentX = Component.LEFT_ALIGNMENT
        addChangeListener { newValue ->
            renderer.shadowIntensity = newValue / 100.0
            shadowIntensityValue.text = "$newValue%"
            renderer.repaint()
        }
    }

    private val ambientLightLabel = JLabel("Ambient Light:").apply {
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
    }

    private val ambientLightValue = JLabel("${(renderer.ambientLight * 100).toInt()}%").apply {
        foreground = Color.WHITE
        alignmentX = Component.RIGHT_ALIGNMENT
    }

    private val ambientLightTrack = CustomTrack(0, 100, (renderer.ambientLight * 100).toInt()).apply {
        background = Color(40, 44, 52)
        alignmentX = Component.LEFT_ALIGNMENT
        addChangeListener { newValue ->
            renderer.ambientLight = newValue / 100.0
            ambientLightValue.text = "$newValue%"
            renderer.repaint()
        }
    }

    private val resetButton = JButton("Reset to Defaults").apply {
        background = Color(60, 63, 65)
        foreground = Color.WHITE
        addActionListener {
            resetToDefaults()
        }
    }

    init {
        setupPanel()
    }

    private fun setupPanel() {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)

        // Render distance section
        add(enableRenderDistanceCheckbox)
        add(Box.createVerticalStrut(5))

        val renderDistancePanel = JPanel().apply {
            layout = GridLayout(1, 2, 5, 0)
            background = Color(40, 44, 52)
            add(maxRenderDistanceLabel)
            add(maxRenderDistanceValue)
        }
        renderDistancePanel.alignmentX = Component.LEFT_ALIGNMENT
        renderDistancePanel.maximumSize = Dimension(Int.MAX_VALUE, renderDistancePanel.preferredSize.height)
        add(renderDistancePanel)
        add(maxRenderDistanceTrack)
        maxRenderDistanceTrack.alignmentX = Component.LEFT_ALIGNMENT
        maxRenderDistanceTrack.maximumSize = Dimension(Int.MAX_VALUE, maxRenderDistanceTrack.preferredSize.height)

        // Add more space between sections
        add(Box.createVerticalStrut(20))

        // Shadow section
        add(enableShadowsCheckbox)
        add(Box.createVerticalStrut(5))

        val shadowDistancePanel = JPanel().apply {
            layout = GridLayout(1, 2, 5, 0)
            background = Color(40, 44, 52)
            add(shadowDistanceLabel)
            add(shadowDistanceValue)
        }
        shadowDistancePanel.alignmentX = Component.LEFT_ALIGNMENT
        shadowDistancePanel.maximumSize = Dimension(Int.MAX_VALUE, shadowDistancePanel.preferredSize.height)
        add(shadowDistancePanel)
        add(shadowDistanceTrack)
        shadowDistanceTrack.alignmentX = Component.LEFT_ALIGNMENT
        shadowDistanceTrack.maximumSize = Dimension(Int.MAX_VALUE, shadowDistanceTrack.preferredSize.height)
        add(Box.createVerticalStrut(10))

        val shadowIntensityPanel = JPanel().apply {
            layout = GridLayout(1, 2, 5, 0)
            background = Color(40, 44, 52)
            add(shadowIntensityLabel)
            add(shadowIntensityValue)
        }
        shadowIntensityPanel.alignmentX = Component.LEFT_ALIGNMENT
        shadowIntensityPanel.maximumSize = Dimension(Int.MAX_VALUE, shadowIntensityPanel.preferredSize.height)
        add(shadowIntensityPanel)
        add(shadowIntensityTrack)
        shadowIntensityTrack.alignmentX = Component.LEFT_ALIGNMENT
        shadowIntensityTrack.maximumSize = Dimension(Int.MAX_VALUE, shadowIntensityTrack.preferredSize.height)
        add(Box.createVerticalStrut(10))

        val ambientLightPanel = JPanel().apply {
            layout = GridLayout(1, 2, 5, 0)
            background = Color(40, 44, 52)
            add(ambientLightLabel)
            add(ambientLightValue)
        }
        ambientLightPanel.alignmentX = Component.LEFT_ALIGNMENT
        ambientLightPanel.maximumSize = Dimension(Int.MAX_VALUE, ambientLightPanel.preferredSize.height)
        add(ambientLightPanel)
        add(ambientLightTrack)
        ambientLightTrack.alignmentX = Component.LEFT_ALIGNMENT
        ambientLightTrack.maximumSize = Dimension(Int.MAX_VALUE, ambientLightTrack.preferredSize.height)

        // Add reset button with some space above
        add(Box.createVerticalStrut(20))

        // Change to LEFT alignment for the reset button panel
        val resetPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        resetPanel.background = Color(40, 44, 52)
        resetPanel.add(resetButton)
        resetPanel.alignmentX = Component.LEFT_ALIGNMENT
        resetPanel.maximumSize = Dimension(Int.MAX_VALUE, resetPanel.preferredSize.height)
        add(resetPanel)

        // Apply border with appropriate padding
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color(70, 73, 75)),
                "Render Options",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                Color.WHITE
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        )

        // Set initial track states
        maxRenderDistanceTrack.isEnabled = renderer.enableRenderDistance
        shadowDistanceTrack.isEnabled = renderer.enableShadows
        shadowIntensityTrack.isEnabled = renderer.enableShadows
        ambientLightTrack.isEnabled = renderer.enableShadows
    }

    private fun resetToDefaults() {
        // Reset render distance section
        enableRenderDistanceCheckbox.isSelected = defaultEnableRenderDistance
        renderer.enableRenderDistance = defaultEnableRenderDistance
        maxRenderDistanceTrack.value = defaultRenderDistance
        maxRenderDistanceTrack.isEnabled = defaultEnableRenderDistance
        renderer.maxRenderDistance = defaultRenderDistance.toDouble()
        maxRenderDistanceValue.text = "$defaultRenderDistance units"

        // Reset shadow section
        enableShadowsCheckbox.isSelected = defaultEnableShadows
        renderer.enableShadows = defaultEnableShadows
        shadowDistanceTrack.value = defaultShadowDistance
        shadowDistanceTrack.isEnabled = defaultEnableShadows
        renderer.shadowDistance = defaultShadowDistance.toDouble()
        shadowDistanceValue.text = "$defaultShadowDistance units"

        shadowIntensityTrack.value = defaultShadowIntensity
        shadowIntensityTrack.isEnabled = defaultEnableShadows
        renderer.shadowIntensity = defaultShadowIntensity / 100.0
        shadowIntensityValue.text = "$defaultShadowIntensity%"

        ambientLightTrack.value = defaultAmbientLight
        ambientLightTrack.isEnabled = defaultEnableShadows
        renderer.ambientLight = defaultAmbientLight / 100.0
        ambientLightValue.text = "$defaultAmbientLight%"

        // Refresh the renderer
        renderer.repaint()
    }

    // Custom slider component based on your PlayerOptionsPanel design
    private inner class CustomTrack(
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
            background = Color(40, 44, 52)

            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    if (isEnabled) updateValueFromMouse(e.x)
                }
            })

            addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseDragged(e: MouseEvent) {
                    if (isEnabled) updateValueFromMouse(e.x)
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

            val alpha = if (isEnabled) 255 else 150

            // Draw track background
            g2.color = Color(30, 32, 34, alpha)
            g2.fillRoundRect(0, 0, width, trackHeight, 8, 8)

            // Draw filled portion of track
            val fillWidth = ((value - min).toDouble() / (max - min) * width).toInt()
            g2.color = Color(100, 149, 237, alpha) // Cornflower blue
            g2.fillRoundRect(0, 0, fillWidth, trackHeight, 8, 8)

            // Draw marker at current position (slim vertical line)
            val markerX = ((value - min).toDouble() / (max - min) * width).toInt() - markerWidth / 2
            g2.color = Color(255, 255, 255, alpha)
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
            g2.color = Color(0, 0, 0, alpha - 105)
            g2.drawString(valueText, textX + 1, textY + 1)

            // Draw text in white
            g2.color = Color(255, 255, 255, alpha)
            g2.drawString(valueText, textX, textY)
        }
    }
}