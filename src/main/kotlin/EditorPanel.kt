import java.awt.*
import javax.swing.*
import javax.swing.border.TitledBorder
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent

class EditorPanel(private val onModeSwitch: () -> Unit) : JPanel() {
    private val gridEditor = GridEditor()
    private val modeButton = JButton("Editor Mode")
    private val mainPanel = JPanel()

    init {
        layout = BorderLayout()
        background = Color(40, 44, 52)

        setupModeButton()
        setupMainPanel()

        val scrollPane = JScrollPane(mainPanel).apply {
            border = null
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }

        // Create wrapper panel
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

            add(createSection("Tools", listOf(
                createButton("Select"),
                createButton("Move"),
                createButton("Rotate"),
                createButton("Scale")
            )))
        }
    }

    private fun createSection(title: String, components: List<JComponent>): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
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
            maximumSize = Dimension(Int.MAX_VALUE, 30)
        }
    }

    private fun createColorButton(text: String, initialColor: Color): JButton {
        return JButton(text).apply {
            background = initialColor
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
        }
    }
}