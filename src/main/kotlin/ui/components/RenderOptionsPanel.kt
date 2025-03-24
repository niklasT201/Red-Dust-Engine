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
    // New default values for visibility zone
    private val defaultEnableVisibilityRadius = renderer.enableVisibilityRadius
    private val defaultVisibilityRadius = renderer.visibilityRadius.toInt()
    private val defaultVisibilityFalloff = renderer.visibilityFalloff.toInt()
    private val defaultOutsideColor = renderer.outsideColor

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

    // New visibility zone components
    private val enableVisibilityRadiusCheckbox = JCheckBox("Enable Visibility Zone").apply {
        isSelected = renderer.enableVisibilityRadius
        background = Color(40, 44, 52)
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
        addActionListener {
            renderer.enableVisibilityRadius = isSelected
            visibilityRadiusTrack.isEnabled = isSelected
            visibilityFalloffTrack.isEnabled = isSelected
            outsideColorButton.isEnabled = isSelected
            renderer.repaint()
        }
    }

    private val visibilityRadiusLabel = JLabel("Visibility Radius:").apply {
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
    }

    private val visibilityRadiusValue = JLabel("${renderer.visibilityRadius.toInt()} units").apply {
        foreground = Color.WHITE
        alignmentX = Component.RIGHT_ALIGNMENT
    }

    private val visibilityRadiusTrack = CustomTrack(10, 100, renderer.visibilityRadius.toInt()).apply {
        background = Color(40, 44, 52)
        alignmentX = Component.LEFT_ALIGNMENT
        addChangeListener { newValue ->
            renderer.visibilityRadius = newValue.toDouble()
            visibilityRadiusValue.text = "$newValue units"
            renderer.repaint()
        }
    }

    private val visibilityFalloffLabel = JLabel("Edge Falloff:").apply {
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
    }

    private val visibilityFalloffValue = JLabel("${renderer.visibilityFalloff.toInt()} units").apply {
        foreground = Color.WHITE
        alignmentX = Component.RIGHT_ALIGNMENT
    }

    private val visibilityFalloffTrack = CustomTrack(0, 10, renderer.visibilityFalloff.toInt()).apply {
        background = Color(40, 44, 52)
        alignmentX = Component.LEFT_ALIGNMENT
        addChangeListener { newValue ->
            renderer.visibilityFalloff = newValue.toDouble()
            visibilityFalloffValue.text = "$newValue units"
            renderer.repaint()
        }
    }

    private val outsideColorLabel = JLabel("Outside Color:").apply {
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
    }

    private val outsideColorButton = JButton().apply {
        background = renderer.outsideColor
        preferredSize = Dimension(30, 20)
        addActionListener {
            val newColor = JColorChooser.showDialog(
                this@RenderOptionsPanel,
                "Choose Outside Color",
                renderer.outsideColor
            )
            if (newColor != null) {
                background = newColor
                renderer.outsideColor = newColor
                renderer.repaint()
            }
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
        val renderDistCheckboxPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        renderDistCheckboxPanel.background = Color(40, 44, 52)
        renderDistCheckboxPanel.add(enableRenderDistanceCheckbox)
        add(renderDistCheckboxPanel)
        add(Box.createVerticalStrut(5))

        val renderDistancePanel = JPanel().apply {
            layout = BorderLayout(5, 0)  // 5px horizontal gap
            background = Color(40, 44, 52)
            add(maxRenderDistanceLabel, BorderLayout.WEST)
            add(maxRenderDistanceValue, BorderLayout.EAST)
        }
        add(renderDistancePanel)

        // Create a panel for the track with BorderLayout to allow expansion
        val maxRenderDistancePanel = JPanel(BorderLayout(0, 0))
        maxRenderDistancePanel.background = Color(40, 44, 52)
        maxRenderDistancePanel.add(maxRenderDistanceTrack, BorderLayout.CENTER)
        add(maxRenderDistancePanel)

        // Add more space between sections
        add(Box.createVerticalStrut(20))

        // Shadow section
        val shadowCheckboxPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        shadowCheckboxPanel.background = Color(40, 44, 52)
        shadowCheckboxPanel.add(enableShadowsCheckbox)
        add(shadowCheckboxPanel)
        add(Box.createVerticalStrut(5))

        val shadowDistancePanel = JPanel().apply {
            layout = BorderLayout(5, 0)
            background = Color(40, 44, 52)
            add(shadowDistanceLabel, BorderLayout.WEST)
            add(shadowDistanceValue, BorderLayout.EAST)
        }
        add(shadowDistancePanel)

        // Create a panel for the shadow distance track
        val shadowDistanceTrackPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        shadowDistanceTrackPanel.background = Color(40, 44, 52)
        shadowDistanceTrackPanel.add(shadowDistanceTrack)
        add(shadowDistanceTrackPanel)
        add(Box.createVerticalStrut(10))

        val shadowIntensityPanel = JPanel().apply {
            layout = BorderLayout(5, 0)
            background = Color(40, 44, 52)
            add(shadowIntensityLabel, BorderLayout.WEST)
            add(shadowIntensityValue, BorderLayout.EAST)
        }
        add(shadowIntensityPanel)

        // Create a panel for the shadow intensity track
        val shadowIntensityTrackPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        shadowIntensityTrackPanel.background = Color(40, 44, 52)
        shadowIntensityTrackPanel.add(shadowIntensityTrack)
        add(shadowIntensityTrackPanel)
        add(Box.createVerticalStrut(10))

        val ambientLightPanel = JPanel().apply {
            layout = BorderLayout(5, 0)
            background = Color(40, 44, 52)
            add(ambientLightLabel, BorderLayout.WEST)
            add(ambientLightValue, BorderLayout.EAST)
        }
        add(ambientLightPanel)

        // Create a panel for the ambient light track
        val ambientLightTrackPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        ambientLightTrackPanel.background = Color(40, 44, 52)
        ambientLightTrackPanel.add(ambientLightTrack)
        add(ambientLightTrackPanel)

        // Add more space between sections
        add(Box.createVerticalStrut(20))

        // New Visibility Zone section
        val visibilityCheckboxPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        visibilityCheckboxPanel.background = Color(40, 44, 52)
        visibilityCheckboxPanel.add(enableVisibilityRadiusCheckbox)
        add(visibilityCheckboxPanel)
        add(Box.createVerticalStrut(5))

        val visibilityRadiusPanel = JPanel().apply {
            layout = BorderLayout(5, 0)
            background = Color(40, 44, 52)
            add(visibilityRadiusLabel, BorderLayout.WEST)
            add(visibilityRadiusValue, BorderLayout.EAST)
        }
        add(visibilityRadiusPanel)

        // Create a panel for the visibility radius track
        val visibilityRadiusTrackPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        visibilityRadiusTrackPanel.background = Color(40, 44, 52)
        visibilityRadiusTrackPanel.add(visibilityRadiusTrack)
        add(visibilityRadiusTrackPanel)
        add(Box.createVerticalStrut(10))

        val visibilityFalloffPanel = JPanel().apply {
            layout = BorderLayout(5, 0)
            background = Color(40, 44, 52)
            add(visibilityFalloffLabel, BorderLayout.WEST)
            add(visibilityFalloffValue, BorderLayout.EAST)
        }
        add(visibilityFalloffPanel)

        // Create a panel for the visibility falloff track
        val visibilityFalloffTrackPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        visibilityFalloffTrackPanel.background = Color(40, 44, 52)
        visibilityFalloffTrackPanel.add(visibilityFalloffTrack)
        add(visibilityFalloffTrackPanel)
        add(Box.createVerticalStrut(10))

        val outsideColorPanel = JPanel().apply {
            layout = BorderLayout(5, 0)
            background = Color(40, 44, 52)
            add(outsideColorLabel, BorderLayout.WEST)

            // Create a container for the color button to prevent it from expanding
            val colorButtonContainer = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
            colorButtonContainer.background = Color(40, 44, 52)
            colorButtonContainer.add(outsideColorButton)

            add(colorButtonContainer, BorderLayout.EAST)
        }
        add(outsideColorPanel)
        add(Box.createVerticalStrut(10))

        // Add reset button with some space above
        add(Box.createVerticalStrut(20))

        // Reset button panel
        val resetPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        resetPanel.background = Color(40, 44, 52)
        resetPanel.add(resetButton)
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

        // Set initial states for visibility controls
        visibilityRadiusTrack.isEnabled = renderer.enableVisibilityRadius
        visibilityFalloffTrack.isEnabled = renderer.enableVisibilityRadius
        outsideColorButton.isEnabled = renderer.enableVisibilityRadius
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

        // Reset visibility zone section
        enableVisibilityRadiusCheckbox.isSelected = defaultEnableVisibilityRadius
        renderer.enableVisibilityRadius = defaultEnableVisibilityRadius
        visibilityRadiusTrack.value = defaultVisibilityRadius
        visibilityRadiusTrack.isEnabled = defaultEnableVisibilityRadius
        renderer.visibilityRadius = defaultVisibilityRadius.toDouble()
        visibilityRadiusValue.text = "$defaultVisibilityRadius units"

        visibilityFalloffTrack.value = defaultVisibilityFalloff
        visibilityFalloffTrack.isEnabled = defaultEnableVisibilityRadius
        renderer.visibilityFalloff = defaultVisibilityFalloff.toDouble()
        visibilityFalloffValue.text = "$defaultVisibilityFalloff units"

        outsideColorButton.background = defaultOutsideColor
        outsideColorButton.isEnabled = defaultEnableVisibilityRadius
        renderer.outsideColor = defaultOutsideColor

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
            minimumSize = Dimension(50, trackHeight)
            preferredSize = Dimension(130, trackHeight)
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

    fun refreshFromGameState() {
        // Update render distance controls
        enableRenderDistanceCheckbox.isSelected = renderer.enableRenderDistance
        maxRenderDistanceTrack.value = renderer.maxRenderDistance.toInt()
        maxRenderDistanceTrack.isEnabled = renderer.enableRenderDistance
        maxRenderDistanceValue.text = "${renderer.maxRenderDistance.toInt()} units"

        // Update shadow controls
        enableShadowsCheckbox.isSelected = renderer.enableShadows
        shadowDistanceTrack.value = renderer.shadowDistance.toInt()
        shadowDistanceTrack.isEnabled = renderer.enableShadows
        shadowDistanceValue.text = "${renderer.shadowDistance.toInt()} units"

        shadowIntensityTrack.value = (renderer.shadowIntensity * 100).toInt()
        shadowIntensityTrack.isEnabled = renderer.enableShadows
        shadowIntensityValue.text = "${(renderer.shadowIntensity * 100).toInt()}%"

        ambientLightTrack.value = (renderer.ambientLight * 100).toInt()
        ambientLightTrack.isEnabled = renderer.enableShadows
        ambientLightValue.text = "${(renderer.ambientLight * 100).toInt()}%"

        // Update visibility zone controls
        enableVisibilityRadiusCheckbox.isSelected = renderer.enableVisibilityRadius
        visibilityRadiusTrack.value = renderer.visibilityRadius.toInt()
        visibilityRadiusTrack.isEnabled = renderer.enableVisibilityRadius
        visibilityRadiusValue.text = "${renderer.visibilityRadius.toInt()} units"

        visibilityFalloffTrack.value = renderer.visibilityFalloff.toInt()
        visibilityFalloffTrack.isEnabled = renderer.enableVisibilityRadius
        visibilityFalloffValue.text = "${renderer.visibilityFalloff.toInt()} units"

        outsideColorButton.background = renderer.outsideColor
        outsideColorButton.isEnabled = renderer.enableVisibilityRadius
    }
}