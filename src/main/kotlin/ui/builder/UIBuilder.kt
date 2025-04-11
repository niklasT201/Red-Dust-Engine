package ui.builder

import Game3D
import player.uis.*
import java.awt.*
import java.awt.event.*
import javax.swing.*
import java.io.File
import javax.swing.border.EmptyBorder

class UIBuilder(private val game3D: Game3D) : JPanel() {
    private val customizableGameUI = CustomizableGameUI()
    private val previewPanel = UIPreviewPanel(game3D, customizableGameUI)
    private val controlPanel = UIControlPanel(customizableGameUI, previewPanel)

    private val componentPalette = UIComponentPalette(customizableGameUI, previewPanel)
    private val propertiesPanel = UIPropertiesPanel()

    init {
        layout = BorderLayout()

        // Create toolbar
        val toolbar = createToolbar()

        // Create a split pane for component palette and properties
        val leftPanel = JSplitPane(JSplitPane.VERTICAL_SPLIT)
        leftPanel.topComponent = JScrollPane(componentPalette)
        leftPanel.bottomComponent = JScrollPane(propertiesPanel)
        leftPanel.resizeWeight = 0.5

        // Create main split pane
        val mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        mainSplitPane.leftComponent = leftPanel
        mainSplitPane.rightComponent = previewPanel
        mainSplitPane.resizeWeight = 0.2

        add(toolbar, BorderLayout.NORTH)
        add(mainSplitPane, BorderLayout.CENTER)
        add(controlPanel, BorderLayout.SOUTH)

        // Create default UI layout
        customizableGameUI.createDefaultLayout(800, 600)

        // Set preview panel as the component selection listener
        componentPalette.setSelectionListener { component ->
            propertiesPanel.setComponent(component)
        }

        // Allow property changes to update the preview
        propertiesPanel.setChangeListener {
            previewPanel.repaint()
        }
    }

    private fun createToolbar(): JToolBar {
        val toolbar = JToolBar()
        toolbar.isFloatable = false

        // New layout button
        val newButton = JButton("New Layout")
        newButton.addActionListener {
            if (JOptionPane.showConfirmDialog(
                    this,
                    "Create a new layout? Current layout will be lost.",
                    "New Layout",
                    JOptionPane.YES_NO_OPTION
                ) == JOptionPane.YES_OPTION
            ) {
                customizableGameUI.createDefaultLayout(previewPanel.width, previewPanel.height)
                previewPanel.repaint()
            }
        }
        toolbar.add(newButton)

        // Save layout button
        val saveButton = JButton("Save Layout")
        saveButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.dialogTitle = "Save UI Layout"
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                var file = fileChooser.selectedFile
                if (!file.name.endsWith(".ui")) {
                    file = File(file.absolutePath + ".ui")
                }
                customizableGameUI.saveToFile(file)
            }
        }
        toolbar.add(saveButton)

        // Load layout button
        val loadButton = JButton("Load Layout")
        loadButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.dialogTitle = "Load UI Layout"
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                customizableGameUI.loadFromFile(fileChooser.selectedFile)
                previewPanel.repaint()
            }
        }
        toolbar.add(loadButton)

        // Apply to game button
        val applyButton = JButton("Apply to Game")
        applyButton.addActionListener {
            // Apply current UI to the game
            game3D.setCustomUI(customizableGameUI)
            JOptionPane.showMessageDialog(
                this,
                "UI layout applied to game successfully.",
                "UI Applied",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
        toolbar.add(applyButton)

        return toolbar
    }

    // Preview panel to show the UI
    class UIPreviewPanel(private val game3D: Game3D, private val customUI: CustomizableGameUI) : JPanel() {
        // Selected component for drag and drop
        private var selectedComponent: UIComponent? = null
        private var dragStartX = 0
        private var dragStartY = 0
        private var dragOffsetX = 0
        private var dragOffsetY = 0

        private var backgroundImage: Image? = null

        init {
            preferredSize = Dimension(800, 600)
            background = Color(35, 35, 35)

            // Add mouse listeners for drag and drop
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    // Find component under cursor
                    val component = customUI.getComponentAt(e.x, e.y)
                    if (component != null) {
                        selectedComponent = component
                        dragStartX = e.x
                        dragStartY = e.y
                        dragOffsetX = e.x - component.x
                        dragOffsetY = e.y - component.y
                        repaint()
                    }
                }

                override fun mouseReleased(e: MouseEvent) {
                    selectedComponent = null
                    repaint()
                }
            })

            addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseDragged(e: MouseEvent) {
                    val component = selectedComponent ?: return

                    // Move component
                    component.x = e.x - dragOffsetX
                    component.y = e.y - dragOffsetY

                    // Ensure component stays within bounds
                    component.x = component.x.coerceIn(0, width - component.width)
                    component.y = component.y.coerceIn(0, height - component.height)

                    repaint()
                }
            })

            // Take screenshot of game to use as background
            updateBackgroundImage()
        }

        fun updateBackgroundImage() {
            // Create an image of the current game state
            backgroundImage = createImage(width, height)
            val g = backgroundImage?.graphics as? Graphics2D ?: return

            // Draw sky
            val skyRenderer = game3D.getSkyRenderer()
            skyRenderer.render(g, width, height)

            // We could add more game elements here but let's keep it simple

            g.dispose()
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // Draw background image
            backgroundImage?.let { g2.drawImage(it, 0, 0, width, height, null) }

            // Draw all UI components
            customUI.render(g2, width, height)

            // Highlight selected component
            selectedComponent?.let {
                g2.color = Color(255, 255, 0, 100)
                g2.fillRect(it.x, it.y, it.width, it.height)

                g2.color = Color(255, 255, 0)
                g2.drawRect(it.x, it.y, it.width, it.height)

                // Draw handle points
                val handleSize = 8
                g2.fillRect(it.x - handleSize / 2, it.y - handleSize / 2, handleSize, handleSize) // Top-left
                g2.fillRect(
                    it.x + it.width - handleSize / 2,
                    it.y - handleSize / 2,
                    handleSize,
                    handleSize
                ) // Top-right
                g2.fillRect(
                    it.x - handleSize / 2,
                    it.y + it.height - handleSize / 2,
                    handleSize,
                    handleSize
                ) // Bottom-left
                g2.fillRect(
                    it.x + it.width - handleSize / 2,
                    it.y + it.height - handleSize / 2,
                    handleSize,
                    handleSize
                ) // Bottom-right
            }
        }
    }

    // Panel for adding components
    class UIComponentPalette(
        private val customUI: CustomizableGameUI,
        private val previewPanel: UIPreviewPanel
    ) : JPanel() {
        private var selectionListener: ((UIComponent?) -> Unit)? = null

        init {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = EmptyBorder(10, 10, 10, 10)

            add(createHeader("Add Components"))

            // Add component buttons
            add(createComponentButton("Health Bar", HealthBarComponent(20, 20, 210, 100)))
            add(createComponentButton("Ammo Bar", AmmoBarComponent(20, 20, 210, 100)))
            add(createComponentButton("Face Panel", FaceComponent(20, 20, 170, 100)))
            add(createComponentButton("Weapon Selector", WeaponSelectorComponent(20, 20)))

            add(Box.createVerticalGlue())
        }

        private fun createHeader(text: String): JLabel {
            val label = JLabel(text)
            label.font = Font(label.font.name, Font.BOLD, 14)
            label.alignmentX = Component.LEFT_ALIGNMENT
            label.border = EmptyBorder(0, 0, 10, 0)
            return label
        }

        private fun createComponentButton(text: String, templateComponent: UIComponent): JButton {
            val button = JButton(text)
            button.alignmentX = Component.LEFT_ALIGNMENT
            button.maximumSize = Dimension(Integer.MAX_VALUE, button.preferredSize.height)

            button.addActionListener {
                // Create a new component from the template
                val newComponent = templateComponent.clone()
                newComponent.id = "${text.toLowerCase().replace(" ", "_")}_${System.currentTimeMillis()}"

                // Add it to the UI
                customUI.addComponent(newComponent)

                // Notify selection listener
                selectionListener?.invoke(newComponent)

                // Repaint preview
                previewPanel.repaint()
            }

            return button
        }

        fun setSelectionListener(listener: (UIComponent?) -> Unit) {
            this.selectionListener = listener
        }
    }


    class UIPropertiesPanel : JPanel() {
        private var currentComponent: UIComponent? = null
        private var changeListener: (() -> Unit)? = null

        init {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = EmptyBorder(10, 10, 10, 10)

            add(JLabel("Select a component to edit its properties").apply {
                alignmentX = Component.LEFT_ALIGNMENT
            })
        }

        fun setComponent(component: UIComponent?) {
            currentComponent = component

            // Clear existing controls
            removeAll()

            if (component == null) {
                add(JLabel("Select a component to edit its properties").apply {
                    alignmentX = Component.LEFT_ALIGNMENT
                })
            } else {
                // Create property editors for the component
                add(createHeader("Component Properties: ${component.javaClass.simpleName}"))

                // Position properties
                add(createHeader("Position"))
                add(createNumberField("X", component.x) { value ->
                    component.x = value
                    changeListener?.invoke()
                })
                add(createNumberField("Y", component.y) { value ->
                    component.y = value
                    changeListener?.invoke()
                })

                // Size properties
                add(createHeader("Size"))
                add(createNumberField("Width", component.width) { value ->
                    component.width = value
                    changeListener?.invoke()
                })
                add(createNumberField("Height", component.height) { value ->
                    component.height = value
                    changeListener?.invoke()
                })

                // Visibility
                add(createHeader("Visibility"))
                add(createCheckBox("Visible", component.visible) { value ->
                    component.visible = value
                    changeListener?.invoke()
                })

                // Component-specific properties
                when (component) {
                    is HealthBarComponent -> addHealthBarProperties(component)
                    is AmmoBarComponent -> addAmmoBarProperties(component)
                    is FaceComponent -> addFaceProperties(component)
                    is WeaponSelectorComponent -> addWeaponSelectorProperties(component)
                }

                // Delete button
                add(Box.createVerticalStrut(20))
                add(JButton("Delete Component").apply {
                    alignmentX = Component.LEFT_ALIGNMENT
                    addActionListener {
                        if (JOptionPane.showConfirmDialog(
                                this@UIPropertiesPanel, // Context for the dialog
                                "Delete this component?",
                                "Confirm Delete",
                                JOptionPane.YES_NO_OPTION
                            ) == JOptionPane.YES_OPTION
                        ) {
                            // Find the UIBuilder instance - START SEARCHING FROM THIS PANEL
                            val parentUI = SwingUtilities.getAncestorOfClass(
                                UIBuilder::class.java, // The class to find
                                this@UIPropertiesPanel // The component to start searching from <--- FIX HERE
                            ) as? UIBuilder

                            if (parentUI != null) {
                                // Get the component to delete (make sure currentComponent isn't null)
                                val componentToDelete = currentComponent
                                if (componentToDelete != null) {
                                    // Remove component
                                    parentUI.customizableGameUI.removeComponent(componentToDelete)
                                    setComponent(null) // Clear the properties panel
                                    parentUI.previewPanel.repaint() // Update the preview
                                } else {
                                    // Optional: Handle case where currentComponent is somehow null
                                    println("Error: Tried to delete but no component was selected.")
                                }
                            } else {
                                // Optional: Handle case where UIBuilder ancestor wasn't found (shouldn't happen in this structure)
                                println("Error: Could not find the parent UIBuilder component.")
                            }
                        }
                    }
                })
            }

            revalidate()
            repaint()
        }

        private fun addHealthBarProperties(component: HealthBarComponent) {
            // Color properties
            add(createHeader("Colors"))
            add(createColorField("Health Color", component.healthColor) { color ->
                component.healthColor = color
                changeListener?.invoke()
            })
            add(createColorField("Background", component.backgroundColor) { color ->
                component.backgroundColor = color
                changeListener?.invoke()
            })
            add(createColorField("Border", component.borderColor) { color ->
                component.borderColor = color
                changeListener?.invoke()
            })
            add(createColorField("Text", component.textColor) { color ->
                component.textColor = color
                changeListener?.invoke()
            })
        }

        private fun addAmmoBarProperties(component: AmmoBarComponent) {
            // Color properties
            add(createHeader("Colors"))
            add(createColorField("Ammo Color", component.ammoColor) { color ->
                component.ammoColor = color
                changeListener?.invoke()
            })
            add(createColorField("Background", component.backgroundColor) { color ->
                component.backgroundColor = color
                changeListener?.invoke()
            })
            add(createColorField("Border", component.borderColor) { color ->
                component.borderColor = color
                changeListener?.invoke()
            })
            add(createColorField("Text", component.textColor) { color ->
                component.textColor = color
                changeListener?.invoke()
            })
        }

        private fun addFaceProperties(component: FaceComponent) {
            // Color properties
            add(createHeader("Colors"))
            add(createColorField("Background", component.backgroundColor) { color ->
                component.backgroundColor = color
                changeListener?.invoke()
            })
            add(createColorField("Border", component.borderColor) { color ->
                component.borderColor = color
                changeListener?.invoke()
            })
            add(createColorField("Text", component.textColor) { color ->
                component.textColor = color
                changeListener?.invoke()
            })
        }

        private fun addWeaponSelectorProperties(component: WeaponSelectorComponent) {
            // Color properties
            add(createHeader("Colors"))
            add(createColorField("Background", component.backgroundColor) { color ->
                component.backgroundColor = color
                changeListener?.invoke()
            })
            add(createColorField("Border", component.borderColor) { color ->
                component.borderColor = color
                changeListener?.invoke()
            })
            add(createColorField("Text", component.textColor) { color ->
                component.textColor = color
                changeListener?.invoke()
            })

            // Current weapon
            add(createHeader("Weapon"))
            add(createNumberField("Current Weapon", component.currentWeapon) { value ->
                component.currentWeapon = value.coerceIn(1, 5)
                changeListener?.invoke()
            })
        }

        private fun createHeader(text: String): JLabel {
            val label = JLabel(text)
            label.font = Font(label.font.name, Font.BOLD, 14)
            label.alignmentX = Component.LEFT_ALIGNMENT
            label.border = EmptyBorder(5, 0, 5, 0)
            return label
        }

        private fun createNumberField(label: String, initialValue: Int, onValueChanged: (Int) -> Unit): JPanel {
            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
            panel.alignmentX = Component.LEFT_ALIGNMENT
            panel.border = EmptyBorder(3, 0, 3, 0)

            panel.add(JLabel("$label: ").apply {
                preferredSize = Dimension(80, preferredSize.height)
            })

            val spinner = JSpinner(SpinnerNumberModel(initialValue, 0, 1000, 1))
            spinner.preferredSize = Dimension(70, spinner.preferredSize.height)
            spinner.addChangeListener {
                onValueChanged(spinner.value as Int)
            }

            panel.add(spinner)
            return panel
        }

        private fun createColorField(label: String, initialColor: Color, onColorChanged: (Color) -> Unit): JPanel {
            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
            panel.alignmentX = Component.LEFT_ALIGNMENT
            panel.border = EmptyBorder(3, 0, 3, 0)

            panel.add(JLabel("$label: ").apply {
                preferredSize = Dimension(80, preferredSize.height)
            })

            val colorButton = JButton()
            colorButton.background = initialColor
            colorButton.preferredSize = Dimension(70, 20)
            colorButton.addActionListener {
                val newColor = JColorChooser.showDialog(this, "Choose $label Color", colorButton.background)
                if (newColor != null) {
                    colorButton.background = newColor
                    onColorChanged(newColor)
                }
            }

            panel.add(colorButton)
            return panel
        }

        private fun createCheckBox(label: String, initialValue: Boolean, onValueChanged: (Boolean) -> Unit): JPanel {
            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
            panel.alignmentX = Component.LEFT_ALIGNMENT
            panel.border = EmptyBorder(3, 0, 3, 0)

            val checkbox = JCheckBox(label, initialValue)
            checkbox.addActionListener {
                onValueChanged(checkbox.isSelected)
            }

            panel.add(checkbox)
            return panel
        }

        fun setChangeListener(listener: () -> Unit) {
            changeListener = listener
        }
    }

    // Control panel for common operations
    class UIControlPanel(
        private val customUI: CustomizableGameUI,
        private val previewPanel: UIPreviewPanel
    ) : JPanel() {

        init {
            layout = BorderLayout()
            border = EmptyBorder(10, 10, 10, 10)

            // Create layout buttons
            val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 5, 0))

            // Reset to default
            val resetButton = JButton("Reset to Default")
            resetButton.addActionListener {
                if (JOptionPane.showConfirmDialog(
                        this,
                        "Reset to default layout? Current layout will be lost.",
                        "Reset Layout",
                        JOptionPane.YES_NO_OPTION
                    ) == JOptionPane.YES_OPTION
                ) {
                    customUI.createDefaultLayout(previewPanel.getWidth(), previewPanel.getHeight())
                    previewPanel.repaint()
                }
            }
            buttonPanel.add(resetButton)

            // Clear all
            val clearButton = JButton("Clear All")
            clearButton.addActionListener {
                if (JOptionPane.showConfirmDialog(
                        this,
                        "Remove all components? This cannot be undone.",
                        "Clear Layout",
                        JOptionPane.YES_NO_OPTION
                    ) == JOptionPane.YES_OPTION
                ) {
                    val componentsList = customUI.getComponents().toList()
                    for (component in componentsList) {
                        customUI.removeComponent(component)
                    }
                    previewPanel.repaint()
                }
            }
            buttonPanel.add(clearButton)

            // Refresh preview
            val refreshButton = JButton("Refresh Preview")
            refreshButton.addActionListener {
                previewPanel.updateBackgroundImage()
                previewPanel.repaint()
            }
            buttonPanel.add(refreshButton)

            add(buttonPanel, BorderLayout.CENTER)

            // Status label
            val statusLabel = JLabel("UI Builder - Drag components to position them")
            statusLabel.horizontalAlignment = SwingConstants.CENTER
            add(statusLabel, BorderLayout.SOUTH)
        }
    }
}