package ui.components

import ObjectType
import grideditor.GridEditor
import ui.CollapsibleSection
import javax.swing.JButton
import javax.swing.JRadioButton
import javax.swing.ButtonGroup
import javax.swing.JCheckBox
import java.awt.Color
import java.awt.Component

class EditorSectionsBuilder(
    private val gridEditor: GridEditor,
    private val wallPropertiesManager: WallPropertiesManager,
    private val toolManager: ToolManager
) {
    // Store references to object type buttons
    private lateinit var addWallButton: JButton
    private lateinit var addFloorButton: JButton
    private lateinit var addPlayerSpawnButton: JButton
    private val wallStyleGroup = ButtonGroup()
    private var onWallStyleChange: ((Boolean) -> Unit)? = null

    fun setWallStyleChangeListener(listener: (Boolean) -> Unit) {
        onWallStyleChange = listener
    }

    fun createQuickActionsSection(): CollapsibleSection {
        // Create buttons with stored references
        addWallButton = ButtonFactory.createButton("Add Wall").apply {
            addActionListener {
                gridEditor.setObjectType(ObjectType.WALL)
                updateButtonStates(ObjectType.WALL)
            }
        }

        addFloorButton = ButtonFactory.createButton("Add Floor").apply {
            addActionListener {
                gridEditor.setObjectType(ObjectType.FLOOR)
                updateButtonStates(ObjectType.FLOOR)
            }
        }

        addPlayerSpawnButton = ButtonFactory.createButton("Add Player Spawn").apply {
            addActionListener {
                gridEditor.setObjectType(ObjectType.PLAYER_SPAWN)
                updateButtonStates(ObjectType.PLAYER_SPAWN)
            }
        }

        return CollapsibleSection("Quick Actions").apply {
            addComponent(addWallButton)
            addComponent(addFloorButton)
            addComponent(addPlayerSpawnButton)
            addComponent(ButtonFactory.createButton("Clear All").apply {
                addActionListener {
                    gridEditor.clearGrid()
                }
            })
        }
    }

    fun createWallStyleSection(): CollapsibleSection {
        return CollapsibleSection("Wall Style").apply {
            // Create wall style components
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

            val visualizationToggle = JCheckBox("Show Flat Walls as Lines").apply {
                background = Color(40, 44, 52)
                foreground = Color.WHITE
                alignmentX = Component.LEFT_ALIGNMENT
                addActionListener {
                    gridEditor.setFlatWallVisualization(isSelected)
                }
            }

            addComponent(flatWallRadio)
            addComponent(blockWallRadio)
            addComponent(visualizationToggle)
        }
    }

    fun createWallPropertiesSection(): CollapsibleSection {
        return CollapsibleSection("Wall Properties").apply {
            addComponent(wallPropertiesManager.createColorButton())
            addComponent(wallPropertiesManager.createHeightButton())
            addComponent(wallPropertiesManager.createWidthButton())
        }
    }

    fun createToolsSection(): CollapsibleSection {
        return CollapsibleSection("Tools").apply {
            addComponent(toolManager.createSelectButton())
            addComponent(toolManager.createMoveButton())
            addComponent(toolManager.createRotateButton())
        }
    }

    private fun updateButtonStates(selectedType: ObjectType) {
        addWallButton.background = if (selectedType == ObjectType.WALL)
            ButtonFactory.selectedButtonColor else ButtonFactory.defaultButtonColor
        addFloorButton.background = if (selectedType == ObjectType.FLOOR)
            ButtonFactory.selectedButtonColor else ButtonFactory.defaultButtonColor
        addPlayerSpawnButton.background = if (selectedType == ObjectType.PLAYER_SPAWN)
            ButtonFactory.selectedButtonColor else ButtonFactory.defaultButtonColor
    }
}