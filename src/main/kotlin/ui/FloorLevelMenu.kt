package ui

import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class FloorLevelMenu(
    private val onFloorSelected: (Int) -> Unit,
    private val onFloorAdded: (Boolean) -> Unit // true for above, false for below
) : JPanel() {
    private val floorModel = DefaultComboBoxModel<String>()
    private val floorComboBox = JComboBox(floorModel)

    init {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        background = MenuSystem.BACKGROUND_COLOR
        border = EmptyBorder(2, 5, 2, 5)

        // Initialize with ground floor
        floorModel.addElement("Floor 0")

        // Style the combo box
        floorComboBox.apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            maximumSize = Dimension(120, 25)
            addActionListener {
                val floorText = selectedItem?.toString()
                val floorLevel = floorText?.substringAfter("Floor ")?.toInt()
                if (floorLevel != null) {
                    // Call the callback to update the system
                    onFloorSelected(floorLevel)
                }
            }
        }

        // Create plus and minus buttons
        val plusButton = createButton("+") { onFloorAdded(true) }
        val minusButton = createButton("-") { onFloorAdded(false) }

        // Add components
        add(JLabel("Floor Level: ").apply {
            foreground = Color.WHITE
            border = EmptyBorder(0, 0, 0, 5)
        })
        add(floorComboBox)
        add(Box.createHorizontalStrut(5))
        add(plusButton)
        add(Box.createHorizontalStrut(2))
        add(minusButton)
    }

    private fun createButton(text: String, onClick: () -> Unit): JButton {
        return JButton(text).apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(25, 25)
            addActionListener { onClick() }
        }
    }

    fun addFloor(level: Int) {
        // Check if this floor level already exists to prevent duplicates
        for (i in 0..<floorModel.size) {
            val item = floorModel.getElementAt(i)
            val floorLevel = item.substringAfter("Floor ").toInt()
            if (floorLevel == level) {
                // Floor already exists, don't add a duplicate
                return
            }
        }

        // Add the new floor
        floorModel.addElement("Floor $level")

        // Sort floors by level
        val items = (0 until floorModel.size).map { floorModel.getElementAt(it) }
            .sortedBy { it.substringAfter("Floor ").toInt() }
        floorModel.removeAllElements()
        items.forEach { floorModel.addElement(it) }
    }

    fun setCurrentFloor(level: Int) {
        floorComboBox.selectedItem = "Floor $level"
    }
}