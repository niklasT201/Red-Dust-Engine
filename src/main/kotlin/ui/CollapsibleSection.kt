package ui

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
        alignmentX = LEFT_ALIGNMENT  // Add this
    }

    private var isExpanded = false

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)
        alignmentX = LEFT_ALIGNMENT  // Add this

        contentPanel.apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(70, 73, 75)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            )
            isVisible = false
            alignmentX = LEFT_ALIGNMENT  // Add this

            // Add this to control the width
            maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        }

        headerButton.addActionListener {
            isExpanded = !isExpanded
            contentPanel.isVisible = isExpanded
            headerButton.icon = if (isExpanded)
                UIManager.getIcon("Tree.expandedIcon")
            else UIManager.getIcon("Tree.collapsedIcon")

            // Add this to trigger proper layout update
            revalidate()
            repaint()
        }

        add(headerButton)
        add(contentPanel)

        // Initialize with collapsed icon
        headerButton.icon = UIManager.getIcon("Tree.collapsedIcon")
    }

    fun addComponent(component: JComponent) {
        component.alignmentX = LEFT_ALIGNMENT  // Add this
        if (component is JScrollPane) {
            // Special handling for scroll panes
            component.maximumSize = Dimension(Int.MAX_VALUE, component.preferredSize.height)
        } else {
            // For other components, preserve their height but allow width to expand
            component.maximumSize = Dimension(Int.MAX_VALUE, component.preferredSize.height)
        }

        contentPanel.add(component)
        contentPanel.add(Box.createVerticalStrut(5))

        // Update the panel's preferred size
        contentPanel.revalidate()
    }
}
