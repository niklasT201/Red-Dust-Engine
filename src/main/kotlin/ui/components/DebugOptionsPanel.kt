package ui.components

import Game3D
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.border.TitledBorder

class DebugOptionsPanel(private val game3D: Game3D) : JPanel() {
    private val fpsVisibleCheckbox = JCheckBox("Show FPS Counter")
    private val directionVisibleCheckbox = JCheckBox("Show Direction")
    private val positionVisibleCheckbox = JCheckBox("Show Position")
    private val gameUIVisibleCheckbox = JCheckBox("Show Game UI")
    private val showAllCheckbox = JCheckBox("Show All Debug Info")

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(50, 52, 55)
        border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color(70, 73, 75)),
            "Debug Display",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            null,
            Color.WHITE
        )

        // Style the checkboxes
        for (checkbox in listOf(fpsVisibleCheckbox, directionVisibleCheckbox, positionVisibleCheckbox, gameUIVisibleCheckbox, showAllCheckbox)) {
            checkbox.foreground = Color.WHITE
            checkbox.background = Color(50, 52, 55)
        }

        // Create containers for each checkbox with proper alignment
        for (checkbox in listOf(fpsVisibleCheckbox, directionVisibleCheckbox, positionVisibleCheckbox, gameUIVisibleCheckbox)) {
            val panel = JPanel(FlowLayout(FlowLayout.LEFT))
            panel.background = Color(50, 52, 55)
            panel.add(checkbox)
            add(panel)
        }

        // Add a divider
        add(JSeparator().apply {
            maximumSize = Dimension(Integer.MAX_VALUE, 1)
            foreground = Color(70, 73, 75)
        })

        // Add the "Show All" checkbox
        val showAllPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        showAllPanel.background = Color(50, 52, 55)
        showAllPanel.add(showAllCheckbox)
        add(showAllPanel)

        // Set up event handlers
        fpsVisibleCheckbox.addActionListener {
            game3D.setFpsCounterVisible(fpsVisibleCheckbox.isSelected)
            updateShowAllCheckbox()
        }

        directionVisibleCheckbox.addActionListener {
            game3D.setDirectionVisible(directionVisibleCheckbox.isSelected)
            updateShowAllCheckbox()
        }

        positionVisibleCheckbox.addActionListener {
            game3D.setPositionVisible(positionVisibleCheckbox.isSelected)
            updateShowAllCheckbox()
        }

        gameUIVisibleCheckbox.addActionListener {
            game3D.setGameUIVisible(gameUIVisibleCheckbox.isSelected)
        }

        showAllCheckbox.addActionListener {
            val showAll = showAllCheckbox.isSelected
            game3D.setFpsCounterVisible(showAll)
            game3D.setDirectionVisible(showAll)
            game3D.setPositionVisible(showAll)
            // We don't include GameUI in "Show All" as it's not technically debug info

            // Update individual checkboxes without triggering their listeners
            fpsVisibleCheckbox.isSelected = showAll
            directionVisibleCheckbox.isSelected = showAll
            positionVisibleCheckbox.isSelected = showAll
        }

        // Force sync the state at initialization
        SwingUtilities.invokeLater {
            refreshFromGameState()
        }
    }

    private fun updateShowAllCheckbox() {
        // "Show All" is checked only if all individual debug items are checked
        val allVisible = game3D.isFpsCounterVisible() &&
                game3D.isDirectionVisible() &&
                game3D.isPositionVisible()

        showAllCheckbox.isSelected = allVisible
    }

    fun refreshFromGameState() {
        // Update all checkboxes to match the game state
        fpsVisibleCheckbox.isSelected = game3D.isFpsCounterVisible()
        directionVisibleCheckbox.isSelected = game3D.isDirectionVisible()
        positionVisibleCheckbox.isSelected = game3D.isPositionVisible()
        gameUIVisibleCheckbox.isSelected = game3D.isGameUIVisible()
        updateShowAllCheckbox()
    }
}