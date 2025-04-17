package ui.builder

import Game3D
import player.uis.*
import java.awt.*
import javax.swing.*
import java.io.File

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
}