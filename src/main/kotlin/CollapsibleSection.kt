import java.awt.Color
import java.awt.Dimension
import javax.swing.*

class CollapsibleSection(title: String) : JPanel() {
    private val contentPanel = JPanel()
    private val headerButton = JButton(title).apply {
        background = Color(60, 63, 65)
        foreground = Color.WHITE
        isFocusPainted = false
        maximumSize = Dimension(Int.MAX_VALUE, 30)
        horizontalAlignment = SwingConstants.LEFT
        isContentAreaFilled = false
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color(70, 73, 75)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        )
    }

    private var isExpanded = false

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)

        contentPanel.apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(70, 73, 75)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            )
            isVisible = false
        }

        headerButton.addActionListener {
            isExpanded = !isExpanded
            contentPanel.isVisible = isExpanded
            headerButton.icon = if (isExpanded)
                UIManager.getIcon("Tree.expandedIcon")
            else UIManager.getIcon("Tree.collapsedIcon")
        }

        add(headerButton)
        add(contentPanel)

        // Initialize with collapsed icon
        headerButton.icon = UIManager.getIcon("Tree.collapsedIcon")

        // Set max size to prevent vertical stretching
        maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
    }

    fun addComponent(component: JComponent) {
        contentPanel.add(component)
        contentPanel.add(Box.createVerticalStrut(5))
    }
}
