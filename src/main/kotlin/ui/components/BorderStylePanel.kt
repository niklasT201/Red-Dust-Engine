package ui.components

import Renderer
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener


/**
 * Panel for customizing border style settings (color, thickness, visibility)
 */
class BorderStylePanel(private val renderer: Renderer) : JPanel(), ChangeListener {

    private val enableBordersCheckbox = JCheckBox("Enable Borders")
    private val borderThicknessSlider = JSlider(JSlider.HORIZONTAL, 1, 5, 2)
    private val borderColorButton = JButton("Select Color")
    private val colorPreview = JPanel()

    init {
        background = Color(40, 44, 52)
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        // Configure components
        enableBordersCheckbox.foreground = Color.WHITE
        enableBordersCheckbox.background = Color(40, 44, 52)
        enableBordersCheckbox.isSelected = renderer.drawBorders
        enableBordersCheckbox.alignmentX = Component.LEFT_ALIGNMENT

        // Create and configure thickness slider
        val thicknessPanel = JPanel()
        thicknessPanel.background = Color(40, 44, 52)
        thicknessPanel.layout = BoxLayout(thicknessPanel, BoxLayout.X_AXIS)
        thicknessPanel.alignmentX = Component.LEFT_ALIGNMENT

        val thicknessLabel = JLabel("Thickness: ")
        thicknessLabel.foreground = Color.WHITE
        thicknessPanel.add(thicknessLabel)

        borderThicknessSlider.background = Color(40, 44, 52)
        borderThicknessSlider.foreground = Color.WHITE
        borderThicknessSlider.paintTicks = true
        borderThicknessSlider.paintLabels = true
        borderThicknessSlider.majorTickSpacing = 1
        borderThicknessSlider.value = renderer.borderThickness.toInt()
        thicknessPanel.add(borderThicknessSlider)

        // Create and configure color selection
        val colorPanel = JPanel()
        colorPanel.background = Color(40, 44, 52)
        colorPanel.layout = BoxLayout(colorPanel, BoxLayout.X_AXIS)
        colorPanel.alignmentX = Component.LEFT_ALIGNMENT

        colorPanel.add(JLabel("Border Color: ").apply { foreground = Color.WHITE })

        colorPreview.background = renderer.borderColor
        colorPreview.preferredSize = Dimension(24, 24)
        colorPreview.border = BorderFactory.createLineBorder(Color.WHITE)
        colorPanel.add(colorPreview)
        colorPanel.add(Box.createHorizontalStrut(10))

        borderColorButton.background = Color(60, 63, 65)
        borderColorButton.foreground = Color.WHITE
        colorPanel.add(borderColorButton)

        // Add components to panel
        add(enableBordersCheckbox)
        add(Box.createVerticalStrut(10))
        add(thicknessPanel)
        add(Box.createVerticalStrut(10))
        add(colorPanel)

        // Add event listeners
        enableBordersCheckbox.addChangeListener(this)
        borderThicknessSlider.addChangeListener(this)
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
            borderThicknessSlider -> {
                renderer.borderThickness = borderThicknessSlider.value.toFloat()
                renderer.repaint()
            }
        }
    }

    fun refreshFromGameState() {
        // Update UI components to match the renderer state
        enableBordersCheckbox.isSelected = renderer.drawBorders
        borderThicknessSlider.value = renderer.borderThickness.toInt()
        colorPreview.background = renderer.borderColor
    }
}