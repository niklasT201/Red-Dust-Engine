package ui.builder

import Game3D
import player.uis.*
import java.awt.*
import javax.swing.*
import java.io.File

class UIBuilder(private val game3D: Game3D) : JPanel() {
    // Color scheme matching the About dialog
    companion object {
        val BACKGROUND_COLOR_DARK = Color(30, 33, 40)
        val BACKGROUND_COLOR_LIGHT = Color(45, 48, 55)
        val ACCENT_COLOR = Color(220, 95, 60) // Warm orange/red
        val TEXT_COLOR = Color(200, 200, 200)
        val BORDER_COLOR = Color(25, 28, 35)
        val BUTTON_BG = Color(60, 63, 65)
        val BUTTON_BORDER = Color(80, 83, 85)
    }

    val customizableGameUI = CustomizableGameUI()
    val previewPanel = UIPreviewPanel(game3D, customizableGameUI)
    private val controlPanel = UIControlPanel(customizableGameUI, previewPanel)

    private val componentPalette = UIComponentPalette(customizableGameUI, previewPanel)
    private val propertiesPanel = UIPropertiesPanel()

    init {
        layout = BorderLayout()
        background = BACKGROUND_COLOR_DARK

        // Apply gradient background
        UIManager.put("Panel.background", BACKGROUND_COLOR_DARK)
        UIManager.put("Button.background", BUTTON_BG)
        UIManager.put("Button.foreground", TEXT_COLOR)
        UIManager.put("Label.foreground", TEXT_COLOR)
        UIManager.put("TextField.background", BACKGROUND_COLOR_LIGHT)
        UIManager.put("TextField.foreground", TEXT_COLOR)
        UIManager.put("ComboBox.background", BACKGROUND_COLOR_LIGHT)
        UIManager.put("ComboBox.foreground", TEXT_COLOR)
        UIManager.put("Spinner.background", BACKGROUND_COLOR_LIGHT)
        UIManager.put("Spinner.foreground", TEXT_COLOR)

        UIManager.put("ScrollBar.background", BACKGROUND_COLOR_DARK)
        UIManager.put("ScrollBar.foreground", ACCENT_COLOR)
        UIManager.put("ScrollBar.track", BACKGROUND_COLOR_LIGHT)
        UIManager.put("ScrollBar.thumb", ACCENT_COLOR)
        UIManager.put("ScrollBar.width", 12)

        // Try to remove default borders/shadows that might clash
        UIManager.put("ScrollBar.thumbDarkShadow", ACCENT_COLOR.darker())
        UIManager.put("ScrollBar.thumbHighlight", ACCENT_COLOR)
        UIManager.put("ScrollBar.thumbShadow", ACCENT_COLOR)
        UIManager.put("ScrollBarUI", CustomScrollBarUI::class.java.name)

        // Create toolbar with styled components
        val toolbar = createToolbar()

        // Create a split pane for component palette and properties
        val leftPanel = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
            topComponent = JScrollPane(componentPalette).apply {
                border = BorderFactory.createLineBorder(BORDER_COLOR)
            }
            bottomComponent = JScrollPane(propertiesPanel).apply {
                border = BorderFactory.createLineBorder(BORDER_COLOR)
            }
            resizeWeight = 0.5
            border = BorderFactory.createEmptyBorder()
            dividerSize = 5
            background = BACKGROUND_COLOR_DARK // SplitPane itself
        }

        // Create main split pane
        val mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            leftComponent = leftPanel
            rightComponent = previewPanel
            resizeWeight = 0.2
            border = BorderFactory.createEmptyBorder()
            dividerSize = 5
            background = BACKGROUND_COLOR_DARK
        }

        add(toolbar, BorderLayout.NORTH)
        add(mainSplitPane, BorderLayout.CENTER)
        add(controlPanel, BorderLayout.SOUTH)

        // Create default UI layout
        customizableGameUI.createDefaultLayout(800, 600)

        // Set preview panel as the component selection listener for palette additions
        componentPalette.setSelectionListener { component ->
            propertiesPanel.setComponent(component)
        }

        // Connect preview panel's selection to properties panel
        previewPanel.setSelectionListener { component ->
            propertiesPanel.setComponent(component)
        }

        // Allow property changes to update the preview
        propertiesPanel.setChangeListener {
            previewPanel.repaint()
        }
    }

    private fun createToolbar(): JToolBar {
        val toolbar = JToolBar().apply {
            isFloatable = false
            background = BACKGROUND_COLOR_DARK
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            )
        }

        // Helper function to create styled buttons
        fun createStyledButton(text: String): JButton {
            return JButton(text).apply {
                foreground = Color.WHITE
                background = BUTTON_BG
                font = Font("Arial", Font.BOLD, 12)
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BUTTON_BORDER),
                    BorderFactory.createEmptyBorder(5, 15, 5, 15)
                )
                isFocusPainted = false

                // Add hover effect
                addMouseListener(object : java.awt.event.MouseAdapter() {
                    override fun mouseEntered(e: java.awt.event.MouseEvent) {
                        background = BUTTON_BORDER
                    }

                    override fun mouseExited(e: java.awt.event.MouseEvent) {
                        background = BUTTON_BG
                    }
                })
            }
        }

        // New layout button
        val newButton = createStyledButton("New Layout")
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
        toolbar.addSeparator(Dimension(10, 10))

        // Save layout button
        val saveButton = createStyledButton("Save Layout")
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
        toolbar.addSeparator(Dimension(10, 10))

        // Load layout button
        val loadButton = createStyledButton("Load Layout")
        loadButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.dialogTitle = "Load UI Layout"
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                customizableGameUI.loadFromFile(fileChooser.selectedFile)
                previewPanel.repaint()
            }
        }
        toolbar.add(loadButton)
        toolbar.addSeparator(Dimension(10, 10))

        // Apply to game button
        val applyButton = createStyledButton("Apply to Game")
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

        // Add a title to the right side of toolbar
        toolbar.add(Box.createHorizontalGlue())
        toolbar.add(JLabel("UI BUILDER").apply {
            foreground = ACCENT_COLOR
            font = Font("Impact", Font.BOLD, 18)
        })

        return toolbar
    }
}