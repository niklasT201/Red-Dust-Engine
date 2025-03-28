package ui.components

import Game3D // Import your main game class
import java.awt.Color
import java.awt.Component
import javax.swing.*
import javax.swing.border.TitledBorder

class PlayerSettingsPanel(private val game3D: Game3D) : JPanel() {

    // Gravity toggle component
    private val gravityToggle = JCheckBox("Enable Gravity").apply {
        background = Color(40, 44, 52) // Match style
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
        isSelected = game3D.isGravityEnabled // Set initial state from Game3D

        // Add listener to update Game3D when toggled
        addActionListener {
            game3D.changeGravityEnabled(isSelected)
        }
    }

    init {
        setupPanel()
    }

    private fun setupPanel() {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52) // Match style

        // Add components to panel
        add(gravityToggle)
        add(Box.createVerticalStrut(5)) // Add some spacing if needed

        // Apply border with appropriate padding
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color(70, 73, 75)), // Match style
                "Player Settings", // Panel title
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null, // Default font
                Color.WHITE // Title color
            ),
            BorderFactory.createEmptyBorder(5, 5, 5, 5) // Padding inside the border
        )

        // Ensure panel aligns nicely if placed in another BoxLayout
        alignmentX = Component.LEFT_ALIGNMENT
    }

    /**
     * Call this method from Game3D if the gravity state is changed
     * programmatically (e.g., loading settings) to keep the checkbox synced.
     */
    fun updateGravityState(isEnabled: Boolean) {
        gravityToggle.isSelected = isEnabled
    }
}