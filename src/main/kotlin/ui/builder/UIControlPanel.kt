package ui.builder

import player.uis.CustomizableGameUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.border.EmptyBorder

class UIControlPanel(
    private val customUI: CustomizableGameUI,
    private val previewPanel: UIPreviewPanel
) : JPanel() {

    init {
        layout = BorderLayout()
        border = EmptyBorder(10, 10, 10, 10)

        // Create layout buttons
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 5, 0))

        // Reset to default
        val resetButton = JButton("Reset to Default")
        resetButton.addActionListener {
            if (JOptionPane.showConfirmDialog(
                    this,
                    "Reset to default layout? Current layout will be lost.",
                    "Reset Layout",
                    JOptionPane.YES_NO_OPTION
                ) == JOptionPane.YES_OPTION
            ) {
                customUI.createDefaultLayout(previewPanel.getWidth(), previewPanel.getHeight())
                previewPanel.repaint()
            }
        }
        buttonPanel.add(resetButton)

        // Clear all
        val clearButton = JButton("Clear All")
        clearButton.addActionListener {
            if (JOptionPane.showConfirmDialog(
                    this,
                    "Remove all components? This cannot be undone.",
                    "Clear Layout",
                    JOptionPane.YES_NO_OPTION
                ) == JOptionPane.YES_OPTION
            ) {
                val componentsList = customUI.getComponents().toList()
                for (component in componentsList) {
                    customUI.removeComponent(component)
                }
                previewPanel.repaint()
            }
        }
        buttonPanel.add(clearButton)

        // Refresh preview
        val refreshButton = JButton("Refresh Preview")
        refreshButton.addActionListener {
            previewPanel.updateBackgroundImage()
            previewPanel.repaint()
        }
        buttonPanel.add(refreshButton)

        add(buttonPanel, BorderLayout.CENTER)

        // Status label
        val statusLabel = JLabel("UI Builder - Drag components to position them")
        statusLabel.horizontalAlignment = SwingConstants.CENTER
        add(statusLabel, BorderLayout.SOUTH)
    }
}