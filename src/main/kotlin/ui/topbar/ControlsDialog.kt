package ui.topbar

import java.awt.*
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder

class ControlsDialog(
    private val parentComponent: Component,
    private val controlsManager: ControlsManager
) {
    // UI Colors
    private val backgroundColor = Color(32, 34, 37)
    private val foregroundColor = Color(220, 221, 222)
    private val accentColor = Color(114, 137, 218)
    private val hoverColor = Color(66, 70, 77)
    private val borderColor = Color(47, 49, 54)

    private lateinit var dialog: JDialog
    private lateinit var configurableTable: JTable

    fun show() {
        dialog = JDialog(SwingUtilities.getWindowAncestor(parentComponent) as? JFrame, "Controls", true)
        dialog.layout = BorderLayout(0, 0)
        dialog.background = backgroundColor

        // Create main panel with padding
        val mainPanel = JPanel(BorderLayout(0, 10))
        mainPanel.background = backgroundColor
        mainPanel.border = EmptyBorder(15, 15, 15, 15)

        // Create title panel with toggle
        val (titlePanel, showFixedControls) = createTitlePanel()
        mainPanel.add(titlePanel, BorderLayout.NORTH)

        // Create card layout for switching between views
        val cardPanel = JPanel(CardLayout())
        cardPanel.background = backgroundColor

        // Create configurable controls panel
        val configurablePanel = ControlsPanelFactory.createConfigurableControlsPanel(dialog, controlsManager)
        cardPanel.add(configurablePanel, "configurable")

        // Save reference to the configurable table
        configurableTable = findConfigurableTable(configurablePanel)

        // Create fixed controls panel (only fixed controls)
        val fixedOnlyPanel = JPanel(BorderLayout())
        fixedOnlyPanel.background = backgroundColor
        fixedOnlyPanel.add(ControlsPanelFactory.createFixedControlsPanel(), BorderLayout.CENTER)
        cardPanel.add(fixedOnlyPanel, "fixed")

        mainPanel.add(cardPanel, BorderLayout.CENTER)

        // Add button panel
        val (buttonPanel, resetButton, saveButton) = createButtonPanel()
        mainPanel.add(buttonPanel, BorderLayout.SOUTH)

        // Add toggle listener
        showFixedControls.addItemListener { e ->
            val cardLayout = cardPanel.layout as CardLayout
            if (e.stateChange == ItemEvent.SELECTED) {
                cardLayout.show(cardPanel, "fixed")
                // Hide reset and save buttons when showing fixed controls
                resetButton.isVisible = false
                saveButton.isVisible = false
            } else {
                cardLayout.show(cardPanel, "configurable")
                // Show reset and save buttons when showing configurable controls
                resetButton.isVisible = true
                saveButton.isVisible = true
            }
        }

        dialog.add(mainPanel)
        dialog.setSize(500, 500)
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(parentComponent)) // Center the dialog
        dialog.isVisible = true
    }

    private fun createTitlePanel(): Pair<JPanel, JCheckBox> {
        val titlePanel = JPanel(BorderLayout())
        titlePanel.background = backgroundColor
        titlePanel.border = EmptyBorder(0, 0, 10, 0)

        val titleLabel = JLabel("KEYBOARD CONTROLS")
        titleLabel.font = Font("SansSerif", Font.BOLD, 18)
        titleLabel.foreground = foregroundColor
        titlePanel.add(titleLabel, BorderLayout.WEST)

        // Create toggle for showing fixed controls
        val showFixedControls = JCheckBox("Show Fixed Controls")
        showFixedControls.background = backgroundColor
        showFixedControls.foreground = foregroundColor
        showFixedControls.isSelected = false
        titlePanel.add(showFixedControls, BorderLayout.EAST)

        return Pair(titlePanel, showFixedControls)
    }

    private fun createButtonPanel(): Triple<JPanel, JButton, JButton> {
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonPanel.background = backgroundColor

        val resetButton = createStyledButton("Reset All")
        resetButton.addActionListener {
            val success = controlsManager.resetKeyBindings(dialog)
            if (success) {
                controlsManager.refreshTable(configurableTable)
            }
        }

        val saveButton = createStyledButton("Save")
        saveButton.addActionListener {
            controlsManager.saveKeyBindings(dialog)
        }

        val closeButton = createStyledButton("Close")
        closeButton.addActionListener { dialog.dispose() }

        buttonPanel.add(resetButton)
        buttonPanel.add(saveButton)
        buttonPanel.add(closeButton)

        return Triple(buttonPanel, resetButton, saveButton)
    }

    private fun createStyledButton(text: String): JButton {
        return JButton(text).apply {
            background = hoverColor
            foreground = foregroundColor
            //focusPainted = false
            border = CompoundBorder(
                EmptyBorder(8, 12, 8, 12),
                border
            )
            font = Font("SansSerif", Font.BOLD, 12)
        }
    }

    private fun findConfigurableTable(panel: JPanel): JTable {
        for (component in panel.components) {
            if (component is JScrollPane) {
                val viewport = component.viewport
                if (viewport.view is JTable) {
                    return viewport.view as JTable
                }
            }
        }
        throw IllegalStateException("Could not find configurable table in panel")
    }
}