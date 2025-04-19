package ui.builder

import player.uis.CustomizableGameUI
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class UIControlPanel(
    private val customUI: CustomizableGameUI,
    private val previewPanel: UIPreviewPanel
) : JPanel() {

    init {
        layout = BorderLayout()
        border = EmptyBorder(10, 10, 10, 10)
        background = UIBuilder.BACKGROUND_COLOR_DARK

        // Create a panel with dark gradient background
        val contentPanel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                val gradientPaint = GradientPaint(
                    0f, 0f, UIBuilder.BACKGROUND_COLOR_DARK,
                    0f, height.toFloat(), UIBuilder.BACKGROUND_COLOR_LIGHT
                )
                g2d.paint = gradientPaint
                g2d.fillRect(0, 0, width, height)
            }
        }.apply {
            layout = BorderLayout()
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIBuilder.BORDER_COLOR),
                EmptyBorder(12, 15, 12, 15)
            )
        }

        // Create styled button panel
        val buttonPanel = JPanel().apply {
            layout = FlowLayout(FlowLayout.CENTER, 15, 0)
            isOpaque = false  // To show gradient background
        }

        // Helper function to create styled buttons
        fun createStyledButton(text: String): JButton {
            return JButton(text).apply {
                foreground = UIBuilder.TEXT_COLOR
                background = UIBuilder.BUTTON_BG
                font = Font("Arial", Font.BOLD, 12)
                preferredSize = Dimension(130, 30)
                isFocusPainted = false
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIBuilder.BUTTON_BORDER),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
                )

                // Add hover effect
                addMouseListener(object : java.awt.event.MouseAdapter() {
                    override fun mouseEntered(e: java.awt.event.MouseEvent) {
                        background = UIBuilder.BUTTON_BORDER
                    }

                    override fun mouseExited(e: java.awt.event.MouseEvent) {
                        background = UIBuilder.BUTTON_BG
                    }
                })
            }
        }

        // Reset to default button
        val resetButton = createStyledButton("Reset to Default")
        resetButton.addActionListener {
            if (JOptionPane.showConfirmDialog(
                    this,
                    "Reset to default layout? Current layout will be lost.",
                    "Reset Layout",
                    JOptionPane.YES_NO_OPTION
                ) == JOptionPane.YES_OPTION
            ) {
                customUI.createDefaultLayout(previewPanel.width, previewPanel.height)
                previewPanel.repaint()
            }
        }
        buttonPanel.add(resetButton)

        // Clear all button
        val clearButton = createStyledButton("Clear All")
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

        // Refresh preview button
        val refreshButton = createStyledButton("Refresh Preview")
        refreshButton.addActionListener {
            previewPanel.updateBackgroundImage()
            previewPanel.repaint()
        }
        buttonPanel.add(refreshButton)

        contentPanel.add(buttonPanel, BorderLayout.CENTER)

        // Create stylized status label with accent border
        val statusPanel = JPanel().apply {
            layout = BorderLayout()
            isOpaque = false  // To show gradient background
            border = BorderFactory.createEmptyBorder(10, 0, 0, 0)
        }

        // Add a separator with gradient styling
        statusPanel.add(object : JPanel() {
            override fun preferredSize(): Dimension {
                return Dimension(super.getWidth(), 1)
            }

            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                // Create gradient line similar to About dialog
                val gradient = LinearGradientPaint(
                    0f, 0f, width.toFloat(), 0f,
                    floatArrayOf(0.0f, 0.5f, 1.0f),
                    arrayOf(UIBuilder.BACKGROUND_COLOR_LIGHT, UIBuilder.ACCENT_COLOR, UIBuilder.BACKGROUND_COLOR_LIGHT)
                )

                g2d.stroke = BasicStroke(1f)
                g2d.paint = gradient
                g2d.drawLine(0, 0, width, 0)
            }
        }, BorderLayout.NORTH)

        // Status label with styled text
        val statusLabel = JLabel("UI Builder - Drag components to position them").apply {
            horizontalAlignment = SwingConstants.CENTER
            foreground = UIBuilder.TEXT_COLOR
            font = Font("Arial", Font.ITALIC, 12)
            border = BorderFactory.createEmptyBorder(5, 0, 0, 0)
        }
        statusPanel.add(statusLabel, BorderLayout.CENTER)

        contentPanel.add(statusPanel, BorderLayout.SOUTH)
        add(contentPanel, BorderLayout.CENTER)
    }
}