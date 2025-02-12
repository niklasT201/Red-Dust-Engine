import java.awt.*
import javax.swing.*
import javax.swing.border.TitledBorder
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent

class EditorPanel(private val onModeSwitch: () -> Unit) : JPanel() {
    private val modeButton = JButton("Editor Mode")
    private val mainPanel = JPanel()
    private val cardLayout = CardLayout()
    private val contentPanel = JPanel(cardLayout)

    init {
        layout = BorderLayout()
        background = Color(40, 44, 52)

        // Add component listener to handle resize events
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                updateSizes()
            }
        })

        setupModeButton()
        setupMainPanel()
        setupContentPanel()

        val scrollPane = JScrollPane(mainPanel).apply {
            border = null
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }

        // Create wrapper panel with better layout
        val wrapperPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            add(createTopPanel())
            add(scrollPane)
        }

        add(wrapperPanel, BorderLayout.CENTER)
    }

    private fun createTopPanel(): JPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

        add(modeButton)
        add(Box.createVerticalStrut(5))
        add(JSeparator())

        maximumSize = Dimension(Integer.MAX_VALUE, 50)
    }

    private fun setupModeButton() {
        modeButton.apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
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

            border = BorderFactory.createCompoundBorder(
                border,
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            )
        }
    }

    private fun createButton(text: String): JButton {
        return JButton(text).apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            alignmentX = Component.LEFT_ALIGNMENT
            horizontalAlignment = SwingConstants.LEFT
        }
    }

    private fun createColorButton(text: String, initialColor: Color): JButton {
        return JButton(text).apply {
            background = initialColor
            foreground = Color.WHITE
            isFocusPainted = false
            alignmentX = Component.LEFT_ALIGNMENT
            horizontalAlignment = SwingConstants.LEFT
        }
    }

    private fun createTabPanel(): JPanel {
        val tabsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            background = Color(40, 44, 52)
            add(createTabButton("Objects", true))
            add(createTabButton("Player", false))
            add(createTabButton("Enemies", false))
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            add(tabsPanel)
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

    private fun setupContentPanel() {
        contentPanel.apply {
            background = Color(40, 44, 52)
            add(createContentSubPanel("Objects Panel"), "Objects")
            add(createContentSubPanel("Player Panel"), "Player")
            add(createContentSubPanel("Enemies Panel"), "Enemies")
        }
    }

    private fun createContentSubPanel(text: String): JPanel = JPanel().apply {
        background = Color(40, 44, 52)
        add(JLabel(text).apply { foreground = Color.WHITE })
    }

    private fun updateSizes() {
        val parentWidth = width
        val buttonWidth = (parentWidth - 20).coerceAtLeast(100)

        // Update sizes for all buttons
        fun updateComponentSizes(container: Container) {
            container.components.forEach { component ->
                when (component) {
                    is JButton -> {
                        component.maximumSize = Dimension(buttonWidth, 30)
                        component.minimumSize = Dimension(100, 30)
                        component.preferredSize = Dimension(buttonWidth, 30)
                    }
                    is Container -> updateComponentSizes(component)
                }
            }
        }

        updateComponentSizes(this)
        revalidate()
    }
}