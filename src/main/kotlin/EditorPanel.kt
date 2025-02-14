import java.awt.*
import javax.swing.*
import javax.swing.border.TitledBorder

class EditorPanel(private val onModeSwitch: () -> Unit) : JPanel() {
    var gridEditor = GridEditor()
    private val modeButton = JButton("Editor Mode")
    private val mainPanel = JPanel()
    private val wallStyleGroup = ButtonGroup()
    private var onWallStyleChange: ((Boolean) -> Unit)? = null
    private var currentWallColor = Color(150, 0, 0)  // Default wall color
    private var onColorChange: ((Color) -> Unit)? = null

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
        border = BorderFactory.createEmptyBorder(15, 5, 2, 5)

        add(modeButton)
        add(Box.createVerticalStrut(15))

        // Create a panel for the separator with margins
        val separatorPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)

            // Add left margin
            add(Box.createHorizontalStrut(5))

            // Add separator with fixed width
            add(JSeparator().apply {
                maximumSize = Dimension(Short.MAX_VALUE.toInt(), 1)
                alignmentX = Component.LEFT_ALIGNMENT
            })

            // Add right margin
            add(Box.createHorizontalStrut(5))
        }

        add(separatorPanel)
        add(Box.createVerticalStrut(2))
    }

    fun setModeButtonText(text: String) {
        modeButton.text = text
    }

    fun setWallStyleChangeListener(listener: (Boolean) -> Unit) {
        onWallStyleChange = listener
    }

    fun setColorChangeListener(listener: (Color) -> Unit) {
        onColorChange = listener
    }

    private fun setupModeButton() {
        modeButton.apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                onModeSwitch()
            }
        }
    }

    private fun createWallStylePanel(): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color(70, 73, 75)),
                "Wall Style",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                Color.WHITE
            )

            val flatWallRadio = JRadioButton("Flat Walls").apply {
                isSelected = true
                background = Color(40, 44, 52)
                foreground = Color.WHITE
                alignmentX = Component.LEFT_ALIGNMENT
                addActionListener { onWallStyleChange?.invoke(false) }
            }

            val blockWallRadio = JRadioButton("Block Walls").apply {
                background = Color(40, 44, 52)
                foreground = Color.WHITE
                alignmentX = Component.LEFT_ALIGNMENT
                addActionListener { onWallStyleChange?.invoke(true) }
            }

            wallStyleGroup.add(flatWallRadio)
            wallStyleGroup.add(blockWallRadio)

            add(flatWallRadio)
            add(Box.createVerticalStrut(2))
            add(blockWallRadio)

            border = BorderFactory.createCompoundBorder(
                border,
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            )
        }
    }

    private fun setupMainPanel() {
        mainPanel.apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createEmptyBorder(5, 10, 5, 10)

            add(createSection("Quick Actions", listOf(
                createButton("Add Wall"),
                createButton("Add Floor"),
                createButton("Clear All").apply {
                    addActionListener {
                        gridEditor?.clearGrid()  // We'll need to add this reference
                    }
                }
            )))

            add(Box.createVerticalStrut(10))

            add(createWallStylePanel())

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
            addActionListener {
                val newColor = JColorChooser.showDialog(
                    this,
                    "Choose Wall Color",
                    background
                )
                if (newColor != null) {
                    background = newColor
                    currentWallColor = newColor
                    onColorChange?.invoke(newColor)
                }
            }
        }
    }
}