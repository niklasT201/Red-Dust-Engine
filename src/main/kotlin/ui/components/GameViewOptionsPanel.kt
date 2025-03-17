package ui.components

import Game3D
import java.awt.Color
import java.awt.Component
import javax.swing.*

/**
 * Panel that manages display options for the Game View
 * Controls visibility of FPS counter, direction indicator, and position display
 */
class GameViewOptionsPanel(private val game3D: Game3D) : JPanel() {

    init {
        // Setup panel properties
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

        // FPS Counter checkbox
        val fpsCheckbox = JCheckBox("Show FPS Counter").apply {
            // Force selection to match actual state
            isSelected = true
            background = Color(40, 44, 52)
            foreground = Color.WHITE
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                game3D.setFpsCounterVisible(isSelected)
            }
        }

        // Debug info checkbox (contains direction and position)
        val debugInfoCheckbox = JCheckBox("Show Direction & Position").apply {
            // Force selection to match actual state
            isSelected = true
            background = Color(40, 44, 52)
            foreground = Color.WHITE
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                game3D.setDebugInfoVisible(isSelected)
            }
        }

        // Crosshair checkbox
        val crosshairCheckbox = JCheckBox("Show Crosshair").apply {
            // Force selection to match actual state
            isSelected = true
            background = Color(40, 44, 52)
            foreground = Color.WHITE
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                game3D.setCrosshairVisible(isSelected)
            }
        }

        // Add components to panel
        add(fpsCheckbox)
        add(Box.createVerticalStrut(2))
        add(debugInfoCheckbox)
        add(Box.createVerticalStrut(2))
        add(crosshairCheckbox)
    }
}