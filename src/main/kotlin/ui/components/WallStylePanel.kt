package ui.components

import grideditor.GridEditor
import java.awt.Color
import java.awt.Component
import javax.swing.*
import javax.swing.border.TitledBorder

class WallStylePanel(private val gridEditor: GridEditor) : JPanel() {
    private val wallStyleGroup = ButtonGroup()
    private var onWallStyleChange: ((Boolean) -> Unit)? = null

    // Wall style components
    private val flatWallRadio = JRadioButton("Flat Walls").apply {
        isSelected = true
        background = Color(40, 44, 52)
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
        addActionListener { onWallStyleChange?.invoke(false) }
    }

    private val blockWallRadio = JRadioButton("Block Walls").apply {
        background = Color(40, 44, 52)
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
        addActionListener { onWallStyleChange?.invoke(true) }
    }

    private val visualizationToggle = JCheckBox("Show Flat Walls as Lines").apply {
        background = Color(40, 44, 52)
        foreground = Color.WHITE
        alignmentX = Component.LEFT_ALIGNMENT
        addActionListener {
            gridEditor.setFlatWallVisualization(isSelected)
        }
    }

    init {
        setupPanel()
    }

    private fun setupPanel() {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)

        // Group the radio buttons
        wallStyleGroup.add(flatWallRadio)
        wallStyleGroup.add(blockWallRadio)

        // Add components to panel
        add(flatWallRadio)
        add(Box.createVerticalStrut(2))
        add(blockWallRadio)
        add(Box.createVerticalStrut(5))
        add(visualizationToggle)

        // Apply border with appropriate padding
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color(70, 73, 75)),
                "Wall Style",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                Color.WHITE
            ),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        )
    }

    /**
     * Sets a listener to be notified when the wall style changes
     * @param listener Function that takes a boolean indicating if block walls are selected
     */
    fun setWallStyleChangeListener(listener: (Boolean) -> Unit) {
        onWallStyleChange = listener
    }

    /**
     * Returns whether block walls are currently selected
     */
    fun isBlockWallSelected(): Boolean {
        return blockWallRadio.isSelected
    }

    /**
     * Set the wall style
     * @param isBlockWall true for block walls, false for flat walls
     */
    fun setWallStyle(isBlockWall: Boolean) {
        if (isBlockWall) {
            blockWallRadio.isSelected = true
        } else {
            flatWallRadio.isSelected = true
        }
    }

    /**
     * Sets whether flat walls should be visualized as lines
     */
    fun setFlatWallVisualization(showAsLines: Boolean) {
        visualizationToggle.isSelected = showAsLines
        gridEditor.setFlatWallVisualization(showAsLines)
    }
}