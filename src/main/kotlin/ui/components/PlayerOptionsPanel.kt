package ui.components

import Game3D
import java.awt.Color
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.border.TitledBorder

class PlayerOptionsPanel(private val game3D: Game3D) : JPanel() {
    private val crosshairVisibleCheckbox = JCheckBox("Show Crosshair")

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

        // Create a container for the checkbox with proper alignment
        val checkboxPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        checkboxPanel.background = Color(50, 52, 55)
        checkboxPanel.add(crosshairVisibleCheckbox)

        add(checkboxPanel)

        // Set up crosshair visibility toggle - MOVED THIS AFTER ADDING TO PANEL
        crosshairVisibleCheckbox.addActionListener {
            game3D.setCrosshairVisible(crosshairVisibleCheckbox.isSelected)
        }

        // FORCE SYNC THE STATE AT THE END OF INITIALIZATION
        SwingUtilities.invokeLater {
            crosshairVisibleCheckbox.isSelected = game3D.isCrosshairVisible()
        }
    }
}