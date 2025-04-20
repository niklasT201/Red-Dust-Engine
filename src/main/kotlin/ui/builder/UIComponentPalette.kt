package ui.builder

import player.uis.*
import player.uis.components.*
import player.uis.components.TextComponent
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class UIComponentPalette(
    private val customUI: CustomizableGameUI,
    private val previewPanel: UIPreviewPanel
) : JPanel() {
    private var selectionListener: ((UIComponent?) -> Unit)? = null

    companion object {
        // Color scheme matching the UI Builder and About dialog
        val BACKGROUND_COLOR_DARK = Color(30, 33, 40)
        val BACKGROUND_COLOR_LIGHT = Color(45, 48, 55)
        val ACCENT_COLOR = Color(220, 95, 60) // Warm orange/red
        val TEXT_COLOR = Color(200, 200, 200)
        val BORDER_COLOR = Color(25, 28, 35)
        val BUTTON_BG = Color(60, 63, 65)
        val BUTTON_BORDER = Color(80, 83, 85)
    }

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = EmptyBorder(15, 15, 15, 15)

        // Important: Set to false for custom painting
        isOpaque = false

        add(createHeader("Basic Elements"))
        add(createComponentButton("Background Panel", BackgroundComponent(20, 20, 200, 100)))
        add(createComponentButton("Text Label", TextComponent(20, 20, 100, 20)))
        add(createComponentButton("Face Image", ImageComponent(20, 20, 64, 64)))
        add(createComponentButton("Progress Bar", ProgressBarComponent(20, 20, 180, 16)))
        add(createComponentButton("Stat Display", StatComponent(20, 20, 100, 20)))

        // Add a stylized separator
        add(createStylizedSeparator())

        add(createHeader("Game UI Elements"))
        add(createComponentButton("Health Bar", HealthBarComponent(20, 20, 210, 100)))
        add(createComponentButton("Ammo Bar", AmmoBarComponent(20, 20, 210, 100)))
        add(createComponentButton("Face Panel", FaceComponent(20, 20, 170, 100)))
        add(createComponentButton("Weapon Selector", WeaponSelectorComponent(20, 20)))

        add(Box.createVerticalGlue())

        // Add a footer with component count
        val footerPanel = JPanel()
        footerPanel.isOpaque = false
        footerPanel.layout = FlowLayout(FlowLayout.RIGHT)
        footerPanel.add(JLabel("9 components available").apply {
            foreground = Color(150, 150, 150)
            font = Font("Arial", Font.ITALIC, 11)
        })
        footerPanel.alignmentX = Component.LEFT_ALIGNMENT
        footerPanel.maximumSize = Dimension(Integer.MAX_VALUE, footerPanel.preferredSize.height)
        add(footerPanel)
    }

    // Override paintComponent to draw the gradient background properly
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val gradientPaint = GradientPaint(
            0f, 0f, BACKGROUND_COLOR_DARK,
            0f, height.toFloat(), BACKGROUND_COLOR_LIGHT
        )
        g2d.paint = gradientPaint
        g2d.fillRect(0, 0, width, height)
    }

    private fun createHeader(text: String): JLabel {
        val label = JLabel(text)
        label.font = Font("Arial", Font.BOLD, 14)
        label.foreground = ACCENT_COLOR
        label.alignmentX = Component.LEFT_ALIGNMENT
        label.border = EmptyBorder(5, 0, 10, 0)
        return label
    }

    private fun createStylizedSeparator(): JComponent {
        val separator = object : JPanel() {
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
                    arrayOf(BACKGROUND_COLOR_LIGHT, ACCENT_COLOR, BACKGROUND_COLOR_LIGHT)
                )

                g2d.stroke = BasicStroke(1.5f)
                g2d.paint = gradient
                g2d.drawLine(0, y, width, y)
            }

            init {
                isOpaque = false
            }
        }
        separator.preferredSize = Dimension(1, 15)
        separator.alignmentX = Component.LEFT_ALIGNMENT
        separator.maximumSize = Dimension(Integer.MAX_VALUE, 15)
        return separator
    }

    private fun createComponentButton(text: String, templateComponent: UIComponent): JButton {
        val button = JButton(text)
        button.alignmentX = Component.LEFT_ALIGNMENT
        button.maximumSize = Dimension(Integer.MAX_VALUE, button.preferredSize.height + 10)

        // Style the button
        button.background = BUTTON_BG
        button.foreground = TEXT_COLOR
        button.font = Font("Arial", Font.PLAIN, 12)
        button.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BUTTON_BORDER, 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        )
        button.isFocusPainted = false

        // Add hover effect
        button.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseEntered(e: java.awt.event.MouseEvent) {
                button.background = BUTTON_BORDER
            }

            override fun mouseExited(e: java.awt.event.MouseEvent) {
                button.background = BUTTON_BG
            }
        })

        // Add margin between buttons
        button.border = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 0, 8, 0),
            button.border
        )

        button.addActionListener {
            // Create a new component from the template
            val newComponent = templateComponent.clone()
            newComponent.id = "${text.toLowerCase().replace(" ", "_")}_${System.currentTimeMillis()}"

            // Add it to the UI
            customUI.addComponent(newComponent)

            // Show a small visual feedback for component addition
            showComponentAddedFeedback(button)

            // Notify selection listener
            selectionListener?.invoke(newComponent)

            // Repaint preview
            previewPanel.repaint()
        }

        return button
    }

    private fun showComponentAddedFeedback(button: JButton) {
        // Store original colors
        val originalBackground = button.background
        val originalForeground = button.foreground

        // Flash with accent color
        button.background = ACCENT_COLOR
        button.foreground = Color.WHITE

        // Restore original colors after delay
        Timer(150) {
            button.background = originalBackground
            button.foreground = originalForeground
        }.apply {
            isRepeats = false
            start()
        }
    }

    fun setSelectionListener(listener: (UIComponent?) -> Unit) {
        this.selectionListener = listener
    }
}