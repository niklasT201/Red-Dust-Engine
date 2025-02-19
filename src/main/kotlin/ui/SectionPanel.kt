package ui

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.*
import javax.swing.border.TitledBorder

class FloorSelectorPanel : JPanel() {
    private val floors = mutableListOf<Floor>()
    private var currentFloorIndex = 0
    private val floorButtons = mutableMapOf<Int, JButton>()

    // Colors matching existing theme
    private val backgroundColor = Color(40, 44, 52)
    private val buttonDefaultColor = Color(60, 63, 65)
    private val buttonSelectedColor = Color(100, 100, 255)
    private val textColor = Color.WHITE

    data class Floor(
        val level: Int,
        val heightOffset: Double = level * 4.0  // 4 units between floors
    )

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = backgroundColor
        border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color(70, 73, 75)),
            "Floor Levels",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            Font("Monospace", Font.BOLD, 12),
            textColor
        )

        // Add ground floor (level 0)
        floors.add(Floor(0))
        setupUI()
    }

    private fun setupUI() {
        // Control panel with add/remove floor buttons
        val controlPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = backgroundColor
            alignmentX = Component.LEFT_ALIGNMENT

            add(createButton("▲ Add Floor Above").apply {
                addActionListener {
                    addFloorAbove()
                }
            })
            add(Box.createHorizontalStrut(5))
            add(createButton("▼ Add Floor Below").apply {
                addActionListener {
                    addFloorBelow()
                }
            })
        }

        // Floor buttons panel with scroll support
        val floorsPanel = JScrollPane(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = backgroundColor
            alignmentX = Component.LEFT_ALIGNMENT
        }).apply {
            border = BorderFactory.createEmptyBorder()
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            background = backgroundColor
            preferredSize = Dimension(200, 200)
        }

        updateFloorButtons((floorsPanel.viewport.view as JPanel))

        add(controlPanel)
        add(Box.createVerticalStrut(5))
        add(floorsPanel)
    }

    private fun createButton(text: String): JButton {
        return JButton(text).apply {
            background = buttonDefaultColor
            foreground = textColor
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
        }
    }

    private fun createFloorButton(floor: Floor): JButton {
        return JButton().apply {
            layout = BorderLayout()
            background = if (floor.level == getCurrentFloor().level) buttonSelectedColor else buttonDefaultColor
            foreground = textColor
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 40)

            // Main floor label
            add(JLabel("Floor ${floor.level}").apply {
                foreground = textColor
                horizontalAlignment = SwingConstants.CENTER
                font = Font("Monospace", Font.BOLD, 12)
            }, BorderLayout.CENTER)

            // Height indicator
            add(JLabel("↕ ${floor.heightOffset}u").apply {
                foreground = Color(200, 200, 200)
                horizontalAlignment = SwingConstants.RIGHT
                border = BorderFactory.createEmptyBorder(0, 0, 0, 5)
                font = Font("Monospace", Font.PLAIN, 10)
            }, BorderLayout.SOUTH)

            // Delete button (except for ground floor)
            if (floor.level != 0) {
                add(JButton("×").apply {
                    background = background
                    foreground = textColor
                    isFocusPainted = false
                    maximumSize = Dimension(30, 30)
                    addActionListener {
                        removeFloor(floor.level)
                    }
                }, BorderLayout.EAST)
            }

            addActionListener {
                setCurrentFloor(floor.level)
            }
        }
    }

    private fun updateFloorButtons(panel: JPanel) {
        panel.removeAll()
        floorButtons.clear()

        // Sort floors by level in descending order (highest floor first)
        floors.sortedByDescending { it.level }.forEach { floor ->
            val button = createFloorButton(floor)
            floorButtons[floor.level] = button
            panel.add(button)
            panel.add(Box.createVerticalStrut(2))
        }

        updateButtonStates()
        panel.revalidate()
        panel.repaint()
    }

    private fun addFloorAbove() {
        val newLevel = floors.maxOf { it.level } + 1
        floors.add(Floor(newLevel))
        updateFloorButtons(findFloorsPanel())
        firePropertyChange("floorsChanged", null, floors)
    }

    private fun addFloorBelow() {
        val newLevel = floors.minOf { it.level } - 1
        floors.add(Floor(newLevel))
        updateFloorButtons(findFloorsPanel())
        firePropertyChange("floorsChanged", null, floors)
    }

    private fun removeFloor(level: Int) {
        if (level != 0 && floors.size > 1) { // Prevent removing ground floor and last floor
            val indexToRemove = floors.indexOfFirst { it.level == level }
            if (indexToRemove != -1) {
                floors.removeAt(indexToRemove)
                // If we're removing the current floor, switch to ground floor
                if (currentFloorIndex >= floors.size || floors[currentFloorIndex].level != getCurrentFloor().level) {
                    currentFloorIndex = floors.indexOfFirst { it.level == 0 }
                }
                updateFloorButtons(findFloorsPanel())
                firePropertyChange("floorsChanged", null, floors)
            }
        }
    }

    private fun updateButtonStates() {
        floorButtons.forEach { (level, button) ->
            button.background = if (level == getCurrentFloor().level) buttonSelectedColor else buttonDefaultColor
        }
    }

    private fun findFloorsPanel(): JPanel {
        return ((components.find { it is JScrollPane } as? JScrollPane)?.viewport?.view as? JPanel)
            ?: throw IllegalStateException("Floors panel not found")
    }

    fun getCurrentFloor(): Floor = floors[currentFloorIndex]

    fun setCurrentFloor(level: Int) {
        val index = floors.indexOfFirst { it.level == level }
        if (index != -1) {
            currentFloorIndex = index
            updateButtonStates()
            firePropertyChange("currentFloorChanged", null, getCurrentFloor())
        }
    }

    // Get height offset for current floor
    fun getCurrentFloorHeight(): Double = getCurrentFloor().heightOffset
}