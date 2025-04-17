package ui.builder

import Game3D
import player.uis.*
import player.uis.components.*
import player.uis.components.TextComponent
import java.awt.*
import java.awt.event.*
import javax.swing.*
import java.io.File
import javax.swing.border.EmptyBorder

class UIBuilder(private val game3D: Game3D) : JPanel() {
    val customizableGameUI = CustomizableGameUI()
    val previewPanel = UIPreviewPanel(game3D, customizableGameUI)
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

        // Set preview panel as the component selection listener for palette additions
        componentPalette.setSelectionListener { component ->
            propertiesPanel.setComponent(component)
        }

        // Add this: Connect preview panel's selection to properties panel
        previewPanel.setSelectionListener { component ->
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

        private var selectionListener: ((UIComponent?) -> Unit)? = null
        private var backgroundImage: Image? = null

        private var currentWidth = 800
        private var currentHeight = 600

        init {
            preferredSize = Dimension(800, 600)
            background = Color(35, 35, 35)

            // Add mouse listeners for drag and drop
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    // Find component under cursor using properly scaled coordinates
                    val component = customUI.getComponentAt(e.x, e.y, currentWidth, currentHeight)
                    if (component != null) {
                        selectedComponent = component
                        dragStartX = e.x
                        dragStartY = e.y

                        // Calculate drag offsets in design coordinates
                        val scaleX = customUI.designWidth.toFloat() / currentWidth
                        val scaleY = customUI.designHeight.toFloat() / currentHeight

                        dragOffsetX = (e.x * scaleX).toInt() - component.x
                        dragOffsetY = (e.y * scaleY).toInt() - component.y

                        // Notify selection listener
                        selectionListener?.invoke(component)
                        repaint()
                    } else {
                        // Clear selection if clicked on empty space
                        selectedComponent = null
                        selectionListener?.invoke(null)
                        repaint()
                    }
                }

                override fun mouseReleased(e: MouseEvent) {
                    // Don't clear the selection, just stop dragging
                    // selectedComponent remains set so the component stays highlighted
                    repaint()
                }
            })

            addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseDragged(e: MouseEvent) {
                    val component = selectedComponent ?: return

                    // Calculate scale factors
                    val scaleX = customUI.designWidth.toFloat() / currentWidth
                    val scaleY = customUI.designHeight.toFloat() / currentHeight

                    // Convert screen coordinates to design coordinates
                    component.x = ((e.x * scaleX).toInt() - dragOffsetX)
                    component.y = ((e.y * scaleY).toInt() - dragOffsetY)

                    // Keep within design bounds
                    component.x = component.x.coerceIn(0, customUI.designWidth - component.width)
                    component.y = component.y.coerceIn(0, customUI.designHeight - component.height)

                    repaint()
                }
            })

            // Take screenshot of game to use as background
            updateBackgroundImage()

            addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent) {
                    currentWidth = width
                    currentHeight = height
                    updateBackgroundImage()
                    repaint()
                }
            })
        }

        // Add a method to set the selection listener
        fun setSelectionListener(listener: (UIComponent?) -> Unit) {
            this.selectionListener = listener
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

            // Calculate scale factors for drawing
            val scaleX = currentWidth.toFloat() / customUI.designWidth
            val scaleY = currentHeight.toFloat() / customUI.designHeight

            // Draw background
            backgroundImage?.let { g2.drawImage(it, 0, 0, currentWidth, currentHeight, null) }

            // Apply scaling transformation
            val originalTransform = g2.transform
            g2.scale(scaleX.toDouble(), scaleY.toDouble())

            // Draw UI components in design coordinates
            customUI.render(g2, customUI.designWidth, customUI.designHeight)

            // Highlight selected component
            selectedComponent?.let {
                g2.color = Color(255, 255, 0, 100)
                g2.fillRect(it.x, it.y, it.width, it.height)

                g2.color = Color(255, 255, 0)
                g2.drawRect(it.x, it.y, it.width, it.height)

                // Draw handle points
                val handleSize = 8
                g2.fillRect(it.x - handleSize/2, it.y - handleSize/2, handleSize, handleSize) // Top-left
                g2.fillRect(it.x + it.width - handleSize/2, it.y - handleSize/2, handleSize, handleSize) // Top-right
                g2.fillRect(it.x - handleSize/2, it.y + it.height - handleSize/2, handleSize, handleSize) // Bottom-left
                g2.fillRect(it.x + it.width - handleSize/2, it.y + it.height - handleSize/2, handleSize, handleSize) // Bottom-right
            }

            // Restore original transformation
            g2.transform = originalTransform
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

            add(createHeader("Basic Elements"))
            add(createComponentButton("Background Panel", BackgroundComponent(20, 20, 200, 100)))
            add(createComponentButton("Text Label", TextComponent(20, 20, 100, 20)))
            add(createComponentButton("Face Image", ImageComponent(20, 20, 64, 64)))
            add(createComponentButton("Progress Bar", ProgressBarComponent(20, 20, 180, 16)))
            add(createComponentButton("Stat Display", StatComponent(20, 20, 100, 20)))

            add(createHeader("Game UI Elements"))
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
}