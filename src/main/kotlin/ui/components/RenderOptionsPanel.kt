package ui.components

import Renderer
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class RenderOptionsPanel(private val renderer: Renderer) : JPanel(), ChangeListener {
    // Render distance components
    private val enableRenderDistanceCheckbox = JCheckBox("Enable Render Distance").apply {
        isSelected = renderer.enableRenderDistance
        background = Color(40, 44, 52)
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
        addActionListener {
            renderer.enableRenderDistance = isSelected
            maxRenderDistanceSlider.isEnabled = isSelected
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

    private val maxRenderDistanceSlider = JSlider(5, 100, renderer.maxRenderDistance.toInt()).apply {
        background = Color(40, 44, 52)
        foreground = Color.WHITE
        paintTicks = true
        paintLabels = false
        majorTickSpacing = 25
        minorTickSpacing = 5
        alignmentX = Component.LEFT_ALIGNMENT
        addChangeListener(this@RenderOptionsPanel)
    }

    // Shadow components
    private val enableShadowsCheckbox = JCheckBox("Enable Shadows").apply {
        isSelected = renderer.enableShadows
        background = Color(40, 44, 52)
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
        addActionListener {
            renderer.enableShadows = isSelected
            shadowDistanceSlider.isEnabled = isSelected
            shadowIntensitySlider.isEnabled = isSelected
            ambientLightSlider.isEnabled = isSelected
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

    private val shadowDistanceSlider = JSlider(5, 50, renderer.shadowDistance.toInt()).apply {
        background = Color(40, 44, 52)
        foreground = Color.WHITE
        paintTicks = true
        paintLabels = false
        majorTickSpacing = 10
        minorTickSpacing = 5
        alignmentX = Component.LEFT_ALIGNMENT
        addChangeListener(this@RenderOptionsPanel)
    }

    private val shadowIntensityLabel = JLabel("Shadow Intensity:").apply {
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
    }

    private val shadowIntensityValue = JLabel("${(renderer.shadowIntensity * 100).toInt()}%").apply {
        foreground = Color.WHITE
        alignmentX = Component.RIGHT_ALIGNMENT
    }

    private val shadowIntensitySlider = JSlider(0, 100, (renderer.shadowIntensity * 100).toInt()).apply {
        background = Color(40, 44, 52)
        foreground = Color.WHITE
        paintTicks = true
        paintLabels = false
        majorTickSpacing = 25
        minorTickSpacing = 5
        alignmentX = Component.LEFT_ALIGNMENT
        addChangeListener(this@RenderOptionsPanel)
    }

    private val ambientLightLabel = JLabel("Ambient Light:").apply {
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
    }

    private val ambientLightValue = JLabel("${(renderer.ambientLight * 100).toInt()}%").apply {
        foreground = Color.WHITE
        alignmentX = Component.RIGHT_ALIGNMENT
    }

    private val ambientLightSlider = JSlider(0, 100, (renderer.ambientLight * 100).toInt()).apply {
        background = Color(40, 44, 52)
        foreground = Color.WHITE
        paintTicks = true
        paintLabels = false
        majorTickSpacing = 25
        minorTickSpacing = 5
        alignmentX = Component.LEFT_ALIGNMENT
        addChangeListener(this@RenderOptionsPanel)
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
        add(maxRenderDistanceSlider)
        maxRenderDistanceSlider.alignmentX = Component.LEFT_ALIGNMENT
        maxRenderDistanceSlider.maximumSize = Dimension(Int.MAX_VALUE, maxRenderDistanceSlider.preferredSize.height)
        add(Box.createVerticalStrut(10))

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
        add(shadowDistanceSlider)
        shadowDistanceSlider.alignmentX = Component.LEFT_ALIGNMENT
        shadowDistanceSlider.maximumSize = Dimension(Int.MAX_VALUE, shadowDistanceSlider.preferredSize.height)
        add(Box.createVerticalStrut(5))

        val shadowIntensityPanel = JPanel().apply {
            layout = GridLayout(1, 2, 5, 0)
            background = Color(40, 44, 52)
            add(shadowIntensityLabel)
            add(shadowIntensityValue)
        }
        shadowIntensityPanel.alignmentX = Component.LEFT_ALIGNMENT
        shadowIntensityPanel.maximumSize = Dimension(Int.MAX_VALUE, shadowIntensityPanel.preferredSize.height)
        add(shadowIntensityPanel)
        add(shadowIntensitySlider)
        shadowIntensitySlider.alignmentX = Component.LEFT_ALIGNMENT
        shadowIntensitySlider.maximumSize = Dimension(Int.MAX_VALUE, shadowIntensitySlider.preferredSize.height)
        add(Box.createVerticalStrut(5))

        val ambientLightPanel = JPanel().apply {
            layout = GridLayout(1, 2, 5, 0)
            background = Color(40, 44, 52)
            add(ambientLightLabel)
            add(ambientLightValue)
        }
        ambientLightPanel.alignmentX = Component.LEFT_ALIGNMENT
        ambientLightPanel.maximumSize = Dimension(Int.MAX_VALUE, ambientLightPanel.preferredSize.height)
        add(ambientLightPanel)
        add(ambientLightSlider)
        ambientLightSlider.alignmentX = Component.LEFT_ALIGNMENT
        ambientLightSlider.maximumSize = Dimension(Int.MAX_VALUE, ambientLightSlider.preferredSize.height)

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
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        )

        // Set initial slider states
        maxRenderDistanceSlider.isEnabled = renderer.enableRenderDistance
        shadowDistanceSlider.isEnabled = renderer.enableShadows
        shadowIntensitySlider.isEnabled = renderer.enableShadows
        ambientLightSlider.isEnabled = renderer.enableShadows
    }

    override fun stateChanged(e: ChangeEvent) {
        when (e.source) {
            maxRenderDistanceSlider -> {
                val value = maxRenderDistanceSlider.value.toDouble()
                renderer.maxRenderDistance = value
                maxRenderDistanceValue.text = "${value.toInt()} units"
                renderer.repaint()
            }
            shadowDistanceSlider -> {
                val value = shadowDistanceSlider.value.toDouble()
                renderer.shadowDistance = value
                shadowDistanceValue.text = "${value.toInt()} units"
                renderer.repaint()
            }
            shadowIntensitySlider -> {
                val value = shadowIntensitySlider.value / 100.0
                renderer.shadowIntensity = value
                shadowIntensityValue.text = "${(value * 100).toInt()}%"
                renderer.repaint()
            }
            ambientLightSlider -> {
                val value = ambientLightSlider.value / 100.0
                renderer.ambientLight = value
                ambientLightValue.text = "${(value * 100).toInt()}%"
                renderer.repaint()
            }
        }
    }
}