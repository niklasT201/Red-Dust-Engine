package ui.topbar

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder

class ControlsDialog(
    private val parentComponent: Component,
    private val controlsManager: ControlsManager
) {
    private lateinit var dialog: JDialog
    private lateinit var configurableTable: JTable

    fun show() {
        dialog = JDialog(SwingUtilities.getWindowAncestor(parentComponent) as? JFrame, "Keyboard Controls", true)
        dialog.layout = BorderLayout()

        // Create main panel with gradient background
        val mainPanel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                val gradientPaint = GradientPaint(
                    0f, 0f, Color(30, 33, 40),
                    0f, height.toFloat(), Color(45, 48, 55)
                )
                g2d.paint = gradientPaint
                g2d.fillRect(0, 0, width, height)
            }
        }.apply {
            layout = BorderLayout(0, 15)
            border = EmptyBorder(20, 30, 20, 30)

            // Title Panel
            add(createTitlePanel(), BorderLayout.NORTH)

            // Card layout for switching views
            val cardPanel = JPanel(CardLayout())
            cardPanel.isOpaque = false

            // Create configurable controls panel
            val configurablePanel = ControlsPanelFactory.createConfigurableControlsPanel(dialog, controlsManager)
            cardPanel.add(configurablePanel, "configurable")

            // Save reference to the configurable table
            configurableTable = findConfigurableTable(configurablePanel)

            // Create fixed controls panel
            val fixedOnlyPanel = JPanel(BorderLayout())
            fixedOnlyPanel.isOpaque = false
            fixedOnlyPanel.add(ControlsPanelFactory.createFixedControlsPanel(), BorderLayout.CENTER)
            cardPanel.add(fixedOnlyPanel, "fixed")

            add(cardPanel, BorderLayout.CENTER)

            // Button Panel
            val (buttonPanel, resetButton, saveButton) = createButtonPanel()
            add(buttonPanel, BorderLayout.SOUTH)

            // Toggle listener
            val showFixedControls = createToggleCheckbox()
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
        }

        dialog.add(mainPanel)
        dialog.pack()
        dialog.minimumSize = Dimension(500, 500)
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(parentComponent))
        dialog.isVisible = true
    }

    private fun createTitlePanel(): JPanel {
        return JPanel().apply {
            layout = BorderLayout(0, 5)
            isOpaque = false

            // Title Label
            add(JLabel("KEYBOARD CONTROLS", SwingConstants.CENTER).apply {
                foreground = Color(220, 95, 60)
                font = Font("Impact", Font.BOLD, 22)
                border = EmptyBorder(0, 0, 5, 0)
            }, BorderLayout.NORTH)

            // Stylized separator
            add(object : JPanel() {
                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    val g2d = g as Graphics2D
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                    // Draw glowing line
                    val width = this.width
                    val y = this.height / 2

                    val gradient = LinearGradientPaint(
                        0f, y.toFloat(), width.toFloat(), y.toFloat(),
                        floatArrayOf(0.0f, 0.5f, 1.0f),
                        arrayOf(Color(45, 48, 55), Color(220, 95, 60), Color(45, 48, 55))
                    )

                    g2d.stroke = BasicStroke(2f)
                    g2d.paint = gradient
                    g2d.drawLine(0, y, width, y)
                }

                init {
                    preferredSize = Dimension(1, 10)
                }
            }, BorderLayout.CENTER)
        }
    }

    private fun createToggleCheckbox(): JCheckBox {
        return JCheckBox("Show Fixed Controls").apply {
            foreground = Color.WHITE
            font = Font("Arial", Font.PLAIN, 12)
            isOpaque = false
        }
    }

    private fun createButtonPanel(): Triple<JPanel, JButton, JButton> {
        val buttonPanel = JPanel().apply {
            isOpaque = false
            layout = FlowLayout(FlowLayout.CENTER)
        }

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
            foreground = Color.WHITE
            background = Color(60, 63, 65)
            font = Font("Arial", Font.BOLD, 12)
            preferredSize = Dimension(100, 30)
            isFocusPainted = false
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(80, 83, 85)),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
            )

            // Hover effect
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    background = Color(80, 83, 85)
                }

                override fun mouseExited(e: MouseEvent) {
                    background = Color(60, 63, 65)
                }
            })
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