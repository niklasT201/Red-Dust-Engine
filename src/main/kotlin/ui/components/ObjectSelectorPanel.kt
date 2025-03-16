package ui.components

import grideditor.GridEditor
import ui.CollapsibleSection
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Component
import javax.swing.*

/**
 * Panel for selecting and editing different object types (walls, floors, etc.)
 */
class ObjectSelectorPanel(private val gridEditor: GridEditor) : JPanel() {

    private val objectTypeComboBox = JComboBox<String>()
    private val contentPanel = JPanel(CardLayout())

    // Object type panels
    private val wallStylePanel = WallStylePanel(gridEditor)
    private val wallPropertiesPanel = WallPropertiesPanel()
    private val floorPanel = JPanel() // Placeholder for floor properties

    // Keep track of object-specific panels
    private val objectPanels = mutableMapOf<String, JPanel>()

    // Wall style change listener
    private var wallStyleChangeListener: ((Boolean) -> Unit)? = null

    init {
        layout = BorderLayout()
        background = Color(40, 44, 52)

        // Setup the dropdown for object selection
        setupObjectTypeComboBox()

        // Setup content panel for different object types
        setupContentPanel()

        // Set default selection
        objectTypeComboBox.selectedIndex = 0

        // Connect wall properties panel to grid editor
        wallPropertiesPanel.setGridEditor(gridEditor)

        // Connect wall style panel to our internal listener
        wallStylePanel.setWallStyleChangeListener { isBlockWall ->
            // Update grid editor directly
            gridEditor.setFlatWallVisualization(!isBlockWall)

            // Also forward to any external listeners
            wallStyleChangeListener?.invoke(isBlockWall)
        }
    }

    private fun setupObjectTypeComboBox() {
        // Add object types
        objectTypeComboBox.addItem("Wall")
        objectTypeComboBox.addItem("Floor")
        // Add future object types here

        // Style the combo box
        objectTypeComboBox.background = Color(60, 63, 65)
        objectTypeComboBox.foreground = Color.WHITE

        // Add action listener
        objectTypeComboBox.addActionListener {
            val selectedItem = objectTypeComboBox.selectedItem as String
            (contentPanel.layout as CardLayout).show(contentPanel, selectedItem)
        }

        // Create combobox container panel with proper alignment
        val comboBoxPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createEmptyBorder(5, 5, 10, 5)

            add(JLabel("Object Type:").apply {
                foreground = Color.WHITE
                border = BorderFactory.createEmptyBorder(0, 0, 0, 10)
            })
            add(objectTypeComboBox)
            add(Box.createHorizontalGlue())
        }

        add(comboBoxPanel, BorderLayout.NORTH)
    }

    private fun setupContentPanel() {
        contentPanel.background = Color(40, 44, 52)

        // Create Wall panel
        val wallPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)

            // Add the wall style panel
            add(createSection("Wall Style", wallStylePanel))
            add(Box.createVerticalStrut(10))

            // Add the wall properties panel
            add(createSection("Wall Properties", wallPropertiesPanel))
            add(Box.createVerticalGlue())
        }

        // Create Floor panel (currently just a placeholder)
        val floorPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)

            // Add a placeholder message for now
            add(JLabel("Floor properties will go here").apply {
                foreground = Color.WHITE
                alignmentX = Component.LEFT_ALIGNMENT
            })
            add(Box.createVerticalGlue())
        }

        // Add panels to the card layout
        contentPanel.add(wallPanel, "Wall")
        contentPanel.add(floorPanel, "Floor")

        // Store references to panels for later access
        objectPanels["Wall"] = wallPanel
        objectPanels["Floor"] = floorPanel

        // Add the content panel to the main panel
        add(contentPanel, BorderLayout.CENTER)
    }

    private fun createSection(title: String, component: JComponent): CollapsibleSection {
        return CollapsibleSection(title).apply {
            addComponent(component)
            background = Color(40, 44, 52)
            isOpaque = true
        }
    }

    fun setWallPropertyChangeListener(listener: WallPropertiesPanel.WallPropertyChangeListener) {
        wallPropertiesPanel.setWallPropertyChangeListener(listener)
    }

    fun setWallStyleChangeListener(listener: (Boolean) -> Unit) {
        // Store the external listener
        this.wallStyleChangeListener = listener
    }

    fun updateWallProperties(color: Color, height: Double, width: Double) {
        wallPropertiesPanel.updateProperties(color, height, width)
    }
}