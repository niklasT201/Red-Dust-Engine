import java.awt.*
import javax.swing.*
import javax.swing.border.TitledBorder
import java.awt.event.ActionListener

class EditorPanel(private val onModeSwitch: () -> Unit) : JPanel() {
    private val modeButton = JButton("Editor Mode")
    private val mainPanel = JPanel()
    private val cardLayout = CardLayout()
    private val contentPanel = JPanel(cardLayout)

    init {
        preferredSize = Dimension(250, 600)
        layout = BorderLayout()
        background = Color(40, 44, 52)

        setupModeButton()
        setupMainPanel()
        setupContentPanel()

        // Create a wrapper panel for vertical organization
        val wrapperPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)

            add(Box.createVerticalStrut(10))
            add(modeButton)
            add(Box.createVerticalStrut(10))
            add(JSeparator())
            add(Box.createVerticalStrut(-150))   // Reduced from 10 to 5
            add(mainPanel)
            // Add glue at the bottom to push everything up
            add(Box.createVerticalGlue())
        }

        add(wrapperPanel, BorderLayout.CENTER)
    }

    private fun setupModeButton() {
        modeButton.apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(250, 30)
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                text = if (text == "Editor Mode") "Game Mode" else "Editor Mode"
                onModeSwitch()
            }
        }
    }

    private fun setupMainPanel() {
        mainPanel.apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
            alignmentX = Component.LEFT_ALIGNMENT

            add(createSection("Quick Actions", listOf(
                createButton("Add Wall"),
                createButton("Add Floor")
            )))

            add(Box.createVerticalStrut(10))

            add(createSection("Wall Properties", listOf(
                createColorButton("Wall Color", Color.RED),
                createButton("Wall Height: 3.0"),
                createButton("Wall Width: 2.0")
            )))

            add(Box.createVerticalStrut(10))

            add(createTabPanel())
        }
    }

    private fun createSection(title: String, components: List<JComponent>): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            alignmentX = Component.LEFT_ALIGNMENT
            border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color(70, 73, 75)),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                Color.WHITE
            )

            components.forEach { component ->
                add(component)
                add(Box.createVerticalStrut(5))
            }

            // Add left margin to components
            border = BorderFactory.createCompoundBorder(
                border,
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
            )

            maximumSize = Dimension(250, getPreferredSize().height)
        }
    }

    private fun createButton(text: String): JButton {
        return JButton(text).apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            alignmentX = Component.LEFT_ALIGNMENT
            horizontalAlignment = SwingConstants.LEFT
            maximumSize = Dimension(230, 30)
        }
    }

    private fun createColorButton(text: String, initialColor: Color): JButton {
        return JButton(text).apply {
            background = initialColor
            foreground = Color.WHITE
            isFocusPainted = false
            alignmentX = Component.LEFT_ALIGNMENT
            horizontalAlignment = SwingConstants.LEFT
            maximumSize = Dimension(230, 30)
        }
    }

    private fun createTabPanel(): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(250, getPreferredSize().height)

            val tabPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                background = Color(40, 44, 52)
                add(createTabButton("Objects", true))
                add(createTabButton("Player", false))
                add(createTabButton("Enemies", false))
            }

            add(tabPanel)
            add(contentPanel)
        }
    }

    private fun createTabButton(text: String, isSelected: Boolean): JButton {
        return JButton(text).apply {
            background = if (isSelected) Color(60, 63, 65) else Color(50, 53, 55)
            foreground = Color.WHITE
            isFocusPainted = false
            addActionListener { cardLayout.show(contentPanel, text) }
        }
    }

    private fun createObjectsPanel() = JPanel().apply {
        background = Color(40, 44, 52)
        add(JLabel("Objects Panel").apply { foreground = Color.WHITE })
    }

    private fun createPlayerPanel() = JPanel().apply {
        background = Color(40, 44, 52)
        add(JLabel("Player Panel").apply { foreground = Color.WHITE })
    }

    private fun createEnemiesPanel() = JPanel().apply {
        background = Color(40, 44, 52)
        add(JLabel("Enemies Panel").apply { foreground = Color.WHITE })
    }

    private fun setupContentPanel() {
        contentPanel.apply {
            background = Color(40, 44, 52)
            add(createObjectsPanel(), "Objects")
            add(createPlayerPanel(), "Player")
            add(createEnemiesPanel(), "Enemies")
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(250, getPreferredSize().height)
        }
    }
}