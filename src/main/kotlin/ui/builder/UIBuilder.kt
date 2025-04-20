package ui.builder

import Game3D
import player.uis.*
import ui.topbar.FileManager
import java.awt.*
import javax.swing.*
import java.io.File

class UIBuilder(private val game3D: Game3D, private val fileManager: FileManager) : JPanel() {
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

        // Apply styling to all components in the UIBuilder
        styleComponent(this)
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

        // Save layout button action - MODIFIED
        val saveButton = createStyledButton("Save Layout")
        saveButton.addActionListener {
            // Check if a project is active
            if (fileManager.getCurrentProjectName() == null) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please create or load a project before saving a UI layout.",
                    "Project Required",
                    JOptionPane.WARNING_MESSAGE
                )
                return@addActionListener
            }

            // Get the project-specific UI directory
            val uiDir = fileManager.getUiDirectory()
            if (uiDir == null) {
                JOptionPane.showMessageDialog(
                    this,
                    "Could not determine the UI directory for the current project.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
                return@addActionListener
            }

            val fileChooser = JFileChooser().apply {
                dialogTitle = "Save UI Layout"
                currentDirectory = uiDir // Start in the project's UI directory
                fileFilter = javax.swing.filechooser.FileNameExtensionFilter("UI Layout Files (*.ui)", "ui")
                selectedFile = File(uiDir, FileManager.DEFAULT_UI_FILENAME) // Suggest default name
            }

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                var file = fileChooser.selectedFile
                // Ensure the file has the .ui extension
                if (!file.name.lowercase().endsWith(".ui")) {
                    file = File(file.absolutePath + ".ui")
                }
                // Ensure the file is saved within the project's UI directory
                if (!file.parentFile.absolutePath.equals(uiDir.absolutePath)) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Layout must be saved within the project's 'assets/ui' directory:\n${uiDir.absolutePath}",
                        "Save Location Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                    return@addActionListener
                }

                if (customizableGameUI.saveToFile(file)) {
                    JOptionPane.showMessageDialog(
                        this,
                        "UI Layout saved successfully to:\n${file.name}",
                        "Save Successful",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                    // If saved as the default name, offer to apply it immediately
                    if (file.name == FileManager.DEFAULT_UI_FILENAME) {
                        val choice = JOptionPane.showConfirmDialog(
                            this,
                            "You saved the layout as the default name ('${FileManager.DEFAULT_UI_FILENAME}').\n"+
                                    "This layout will now load automatically when the project opens.\n\n"+
                                    "Apply this layout to the game now?",
                            "Apply Default Layout?",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                        )
                        if (choice == JOptionPane.YES_OPTION) {
                            game3D.setCustomUI(customizableGameUI)
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Failed to save UI Layout.",
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
        toolbar.add(saveButton)
        toolbar.addSeparator(Dimension(10, 10))

        // Load layout button action - MODIFIED
        val loadButton = createStyledButton("Load Layout")
        loadButton.addActionListener {
            // Check if a project is active
            if (fileManager.getCurrentProjectName() == null) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please create or load a project before loading a UI layout.",
                    "Project Required",
                    JOptionPane.WARNING_MESSAGE
                )
                return@addActionListener
            }

            // Get the project-specific UI directory
            val uiDir = fileManager.getUiDirectory()
            if (uiDir == null || !uiDir.exists()) { // Check existence too
                JOptionPane.showMessageDialog(
                    this,
                    "Could not find the UI directory for the current project, or it's empty.\nSave a layout first.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
                return@addActionListener
            }

            val fileChooser = JFileChooser().apply{
                dialogTitle = "Load UI Layout"
                currentDirectory = uiDir // Start in the project's UI directory
                fileFilter = javax.swing.filechooser.FileNameExtensionFilter("UI Layout Files (*.ui)", "ui")
            }

            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                if (customizableGameUI.loadFromFile(fileChooser.selectedFile)) {
                    previewPanel.repaint()
                    JOptionPane.showMessageDialog(
                        this,
                        "UI Layout '${fileChooser.selectedFile.name}' loaded successfully.",
                        "Load Successful",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Failed to load UI Layout from '${fileChooser.selectedFile.name}'.",
                        "Load Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
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
                "Current UI layout applied to the game.",
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

    /**
     * Styles components recursively with the custom UI Builder theme
     * instead of using global UIManager settings
     */
    private fun styleComponent(component: JComponent) {
        when (component) {
            is JPanel -> {
                component.background = BACKGROUND_COLOR_DARK
            }
            is JButton -> {
                component.background = BUTTON_BG
                component.foreground = TEXT_COLOR
                component.font = Font("Arial", Font.BOLD, 12)
                // Keep the compound border if already set, otherwise create a default one
                if (component.border !is javax.swing.border.CompoundBorder) {
                    component.border = BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BUTTON_BORDER),
                        BorderFactory.createEmptyBorder(5, 15, 5, 15)
                    )
                }
                component.isFocusPainted = false
            }
            is JLabel -> {
                component.foreground = TEXT_COLOR
                // Keep custom colors for accent labels
                if (component.foreground != ACCENT_COLOR) {
                    component.foreground = TEXT_COLOR
                }
            }
            is JTextField -> {
                component.background = BACKGROUND_COLOR_LIGHT
                component.foreground = TEXT_COLOR
            }
            is JComboBox<*> -> {
                component.background = BACKGROUND_COLOR_LIGHT
                component.foreground = TEXT_COLOR
            }
            is JSpinner -> {
                component.background = BACKGROUND_COLOR_LIGHT
                component.foreground = TEXT_COLOR
                // Style the editor component of the spinner
                val editor = component.editor
                if (editor is JSpinner.DefaultEditor) {
                    val textField = editor.textField
                    textField.background = BACKGROUND_COLOR_LIGHT
                    textField.foreground = TEXT_COLOR
                }
            }
            is JScrollPane -> {
                component.background = BACKGROUND_COLOR_DARK
                component.border = BorderFactory.createLineBorder(BORDER_COLOR)

                // Style the scrollbars
                component.verticalScrollBar?.apply {
                    background = BACKGROUND_COLOR_DARK
                    foreground = ACCENT_COLOR  // For arrows and other UI elements

                    // You may need to set a custom UI for more complex scroll bar styling
                    // This would replace your UIManager.put("ScrollBarUI", CustomScrollBarUI::class.java.name)
                    setUI(CustomScrollBarUI())
                }

                component.horizontalScrollBar?.apply {
                    background = BACKGROUND_COLOR_DARK
                    foreground = ACCENT_COLOR
                    setUI(CustomScrollBarUI())
                }
            }
            is JSplitPane -> {
                component.background = BACKGROUND_COLOR_DARK
                component.dividerSize = 5
                component.border = BorderFactory.createEmptyBorder()
            }
            is JToolBar -> {
                component.isFloatable = false
                component.background = BACKGROUND_COLOR_DARK
                component.border = BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
                )
            }
        }

        // Recursively style child components if this is a container
        for (i in 0..<component.componentCount) {
            val child = component.getComponent(i)
            if (child is JComponent) {
                styleComponent(child)
            }
        }
    }
}