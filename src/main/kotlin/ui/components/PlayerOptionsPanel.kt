package ui.components

import Game3D
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.border.TitledBorder

class PlayerOptionsPanel(private val game3D: Game3D) : JPanel() {
    private val crosshairVisibleCheckbox = JCheckBox("Show Crosshair")
    private val crosshairSizeSlider = JSlider(JSlider.HORIZONTAL, 5, 30, 10)
    private val crosshairSizeLabel = JLabel("Size: 10")

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

        // Initialize checkbox with current state - MAKE SURE TO SET THIS FIRST
        crosshairVisibleCheckbox.isSelected = game3D.isCrosshairVisible()
        crosshairVisibleCheckbox.foreground = Color.WHITE
        crosshairVisibleCheckbox.background = Color(50, 52, 55)

        // Set up the slider with current size
        crosshairSizeSlider.value = game3D.getCrosshairSize()
        crosshairSizeSlider.background = Color(50, 52, 55)
        crosshairSizeSlider.foreground = Color.WHITE
        crosshairSizeSlider.preferredSize = Dimension(150, crosshairSizeSlider.preferredSize.height)
        crosshairSizeSlider.majorTickSpacing = 5
        crosshairSizeSlider.minorTickSpacing = 1
        crosshairSizeSlider.paintTicks = true
        crosshairSizeLabel.text = "Size: ${game3D.getCrosshairSize()}"
        crosshairSizeLabel.foreground = Color.WHITE

        // Create a container for the checkbox with proper alignment
        val checkboxPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        checkboxPanel.background = Color(50, 52, 55)
        checkboxPanel.add(crosshairVisibleCheckbox)

        add(checkboxPanel)

        // Create a panel for the slider and its label
        val sliderPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        sliderPanel.background = Color(50, 52, 55)
        sliderPanel.add(crosshairSizeLabel)
        sliderPanel.add(crosshairSizeSlider)
        add(sliderPanel)

        // Set up crosshair visibility toggle - MOVED THIS AFTER ADDING TO PANEL
        crosshairVisibleCheckbox.addActionListener {
            game3D.setCrosshairVisible(crosshairVisibleCheckbox.isSelected)
        }

        // Set up crosshair size change listener
        crosshairSizeSlider.addChangeListener { e ->
            val size = crosshairSizeSlider.value
            crosshairSizeLabel.text = "Size: $size"
            game3D.setCrosshairSize(size)
        }

        // FORCE SYNC THE STATE AT THE END OF INITIALIZATION
        SwingUtilities.invokeLater {
            crosshairVisibleCheckbox.isSelected = game3D.isCrosshairVisible()
            crosshairSizeSlider.value = game3D.getCrosshairSize()
            crosshairSizeLabel.text = "Size: ${game3D.getCrosshairSize()}"
        }
    }
}