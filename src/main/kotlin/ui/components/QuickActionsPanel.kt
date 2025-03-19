package ui.components

import ObjectType
import grideditor.GridEditor
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

class QuickActionsPanel(private val gridEditor: GridEditor) : JPanel() {
    // Store references to object type buttons
    private val addWallButton: JButton
    private val addFloorButton: JButton
    private val addPlayerSpawnButton: JButton
    private val clearAllButton: JButton

    // Colors taken from MenuBuilder
    private val backgroundColor = Color(40, 44, 52)
    private val borderColor = Color(28, 31, 36)
    private val hoverColor = Color(80, 83, 85)
    private val defaultButtonColor = Color(60, 63, 65)
    private val selectedButtonColor = Color(220, 95, 60) // Used warm orange/red from About dialog
    private val clearButtonColor = Color(70, 40, 40) // Dark red for clear button
    private val clearButtonHoverColor = Color(90, 50, 50) // Lighter red for hover

    init {
        // Set up panel with gradient background
        layout = BorderLayout()
        background = backgroundColor
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor),
            EmptyBorder(10, 10, 10, 10)
        )

        // Create a content panel with gradient background
        val contentPanel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                val gradientPaint = GradientPaint(
                    0f, 0f, Color(30, 33, 40),
                    0f, height.toFloat(), Color(45, 48, 55)
                )
                g2d.paint = gradientPaint
                g2d.fillRect(0, 0, width, height)
            }
        }.apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = EmptyBorder(10, 5, 10, 5)
            isOpaque = false
        }

        // Add title at the top
        contentPanel.add(JLabel("QUICK ACTIONS", SwingConstants.CENTER).apply {
            foreground = Color(220, 95, 60) // Warm orange/red color
            font = Font("Arial", Font.BOLD, 16)
            alignmentX = Component.CENTER_ALIGNMENT
        })

        // Add stylized separator
        contentPanel.add(Box.createVerticalStrut(5))
        contentPanel.add(object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                // Draw glowing line
                val width = this.width
                val y = this.height / 2

                val gradient = LinearGradientPaint(
                    0f, y.toFloat(), width.toFloat(), y.toFloat(),
                    floatArrayOf(0.0f, 0.5f, 1.0f),
                    arrayOf(Color(45, 48, 55), Color(220, 95, 60), Color(45, 48, 55))
                )

                g2d.stroke = BasicStroke(2f)
                g2d.paint = gradient
                g2d.drawLine(0, y, width, y)
            }
        }.apply {
            preferredSize = Dimension(1, 10)
            maximumSize = Dimension(Short.MAX_VALUE.toInt(), 10)
            alignmentX = Component.CENTER_ALIGNMENT
        })
        contentPanel.add(Box.createVerticalStrut(15))

        // Create buttons with stored references
        addWallButton = createButton("Add Wall").apply {
            addActionListener {
                gridEditor.setObjectType(ObjectType.WALL)
                updateButtonStates(ObjectType.WALL)
                restoreFocusToGridEditor()
            }
        }

        addFloorButton = createButton("Add Floor").apply {
            addActionListener {
                gridEditor.setObjectType(ObjectType.FLOOR)
                updateButtonStates(ObjectType.FLOOR)
                restoreFocusToGridEditor()
            }
        }

        addPlayerSpawnButton = createButton("Add Player Spawn").apply {
            addActionListener {
                gridEditor.setObjectType(ObjectType.PLAYER_SPAWN)
                updateButtonStates(ObjectType.PLAYER_SPAWN)
                restoreFocusToGridEditor()
            }
        }

        // Add spacer before Clear All button
        contentPanel.add(addWallButton)
        contentPanel.add(Box.createVerticalStrut(8))
        contentPanel.add(addFloorButton)
        contentPanel.add(Box.createVerticalStrut(8))
        contentPanel.add(addPlayerSpawnButton)
        contentPanel.add(Box.createVerticalStrut(15))

        // Add another separator before Clear All
        contentPanel.add(object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D

                // Draw a simple line
                g2d.color = Color(70, 73, 75)
                g2d.drawLine(0, height/2, width, height/2)
            }
        }.apply {
            preferredSize = Dimension(1, 8)
            maximumSize = Dimension(Short.MAX_VALUE.toInt(), 8)
            alignmentX = Component.CENTER_ALIGNMENT
        })
        contentPanel.add(Box.createVerticalStrut(15))

        // Create Clear All button with custom colors
        clearAllButton = JButton("Clear All").apply {
            foreground = Color.WHITE
            background = clearButtonColor // Use the dark red color
            font = Font("Arial", Font.BOLD, 12)
            isFocusPainted = false
            alignmentX = Component.CENTER_ALIGNMENT
            maximumSize = Dimension(170, 30)
            preferredSize = Dimension(150, 30)

            // Add rounded corners and proper border
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(100, 60, 60)), // Slightly lighter border
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
            )

            // Special hover effect for Clear button
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    background = clearButtonHoverColor // Lighter red on hover
                }

                override fun mouseExited(e: MouseEvent) {
                    background = clearButtonColor // Back to dark red when not hovering
                }
            })

            addActionListener {
                gridEditor.clearGrid()
            }
        }
        contentPanel.add(clearAllButton)

        // Add the content panel to the main panel
        add(contentPanel, BorderLayout.CENTER)
    }

    /**
     * Updates the visual state of object type buttons
     */
    private fun updateButtonStates(selectedType: ObjectType) {
        addWallButton.background = if (selectedType == ObjectType.WALL) selectedButtonColor else defaultButtonColor
        addFloorButton.background = if (selectedType == ObjectType.FLOOR) selectedButtonColor else defaultButtonColor
        addPlayerSpawnButton.background = if (selectedType == ObjectType.PLAYER_SPAWN) selectedButtonColor else defaultButtonColor
    }

    /**
     * Creates a styled button with consistent appearance
     */
    private fun createButton(text: String): JButton {
        return JButton(text).apply {
            foreground = Color.WHITE
            background = defaultButtonColor
            font = Font("Arial", Font.BOLD, 12)
            isFocusPainted = false
            alignmentX = Component.CENTER_ALIGNMENT
            maximumSize = Dimension(170, 30)
            preferredSize = Dimension(150, 30)

            // Add rounded corners and proper border
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(80, 83, 85)),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
            )

            // Add hover effects
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    if (background != selectedButtonColor) {
                        background = hoverColor
                    }
                }

                override fun mouseExited(e: MouseEvent) {
                    if (background != selectedButtonColor) {
                        background = defaultButtonColor
                    }
                }
            })
        }
    }

    /**
     * Restore focus to the grid editor after button click
     */
    private fun restoreFocusToGridEditor() {
        SwingUtilities.invokeLater {
            gridEditor.requestFocusInWindow()
        }
    }
}