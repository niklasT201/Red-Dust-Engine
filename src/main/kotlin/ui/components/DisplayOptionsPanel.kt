package ui.components

import SettingsSaver
import grideditor.GridEditor
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.border.EmptyBorder
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JLabel

/**
 * Panel that manages display options for the GridEditor
 * Handles showing/hiding various labels in the editor
 */
class DisplayOptionsPanel(private val gridEditor: GridEditor) : JPanel() {

    // Available label types and their default states
    private val labelOptions = mapOf(
        "all" to Pair("Show All Labels", true),
        "mode" to Pair("Show Mode Label", true),
        "direction" to Pair("Show Direction Label", true),
        "texture" to Pair("Show Texture Info", true),
        "stats" to Pair("Show Object Stats", true),
        "player" to Pair("Show Player Position", true)
    )

    // Map to store references to checkboxes
    private val checkboxes = mutableMapOf<String, JCheckBox>()

    // Settings saver instance
    private val settingsSaver = SettingsSaver(gridEditor)

    // UI Colors
    private val backgroundColor = Color(40, 44, 52)
    private val buttonColor = Color(60, 63, 65)
    private val accentColor = Color(97, 175, 239)
    private val textColor = Color(220, 223, 228)

    init {
        // Setup panel properties
        layout = BorderLayout(10, 10)
        background = backgroundColor
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        // Create title panel
        val titlePanel = JPanel(BorderLayout()).apply {
            background = backgroundColor
            border = BorderFactory.createMatteBorder(0, 0, 1, 0, accentColor)
        }

        val titleLabel = JLabel("Display Options").apply {
            font = Font("Dialog", Font.BOLD, 14)
            foreground = textColor
            border = EmptyBorder(0, 2, 5, 0)
        }
        titlePanel.add(titleLabel, BorderLayout.WEST)
        add(titlePanel, BorderLayout.NORTH)

        // Create options panel with improved styling
        val optionsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = backgroundColor
            border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
        }

        // Create checkboxes for each label option with improved styling
        labelOptions.forEach { (key, value) ->
            val (label, defaultState) = value
            createCheckbox(key, label, defaultState, optionsPanel)
        }

        // Add the options panel to the main panel
        add(optionsPanel, BorderLayout.CENTER)

        // Create buttons panel with vertical stacking
        val buttonsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = backgroundColor
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }

        // Create consistently sized buttons
        val buttonDimension = Dimension(140, 30)

        // Save button
        val saveButton = createStyledButton("Save Settings", buttonDimension).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                if (settingsSaver.saveDisplayOptions(this@DisplayOptionsPanel)) {
                    println("Display settings saved successfully.")
                } else {
                    println("Failed to save display settings.")
                }
            }
        }

        // Load button
        val loadButton = createStyledButton("Load Settings", buttonDimension).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                if (settingsSaver.loadDisplayOptions(this@DisplayOptionsPanel)) {
                    println("Display settings loaded successfully.")
                } else {
                    println("Failed to load display settings.")
                }
            }
        }

        // Reset button - now same size as other buttons
        val resetButton = createStyledButton("Reset", buttonDimension).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                resetToDefaults()
                println("Display settings reset to defaults.")
            }
        }

        // Add buttons to panel with vertical spacing
        buttonsPanel.add(saveButton)
        buttonsPanel.add(Box.createVerticalStrut(8))
        buttonsPanel.add(loadButton)
        buttonsPanel.add(Box.createVerticalStrut(8))
        buttonsPanel.add(resetButton)

        // Add buttons panel to the main panel
        add(buttonsPanel, BorderLayout.SOUTH)

        // Try to load saved settings on initialization, but don't fail if it doesn't work
        try {
            settingsSaver.loadDisplayOptions(this)
        } catch (e: Exception) {
            println("Could not load display settings, using defaults: ${e.message}")
        }
    }

    /**
     * Creates a styled button with consistent size
     */
    private fun createStyledButton(text: String, dimension: Dimension): JButton {
        return JButton(text).apply {
            background = buttonColor
            foreground = textColor
            preferredSize = dimension
            minimumSize = dimension
            maximumSize = dimension
            isFocusPainted = false
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
            )
            font = Font("Dialog", Font.BOLD, 12)
        }
    }

    /**
     * Creates a checkbox for controlling label visibility with improved styling
     */
    private fun createCheckbox(key: String, label: String, defaultState: Boolean, panel: JPanel) {
        val checkbox = JCheckBox(label).apply {
            isSelected = defaultState
            background = backgroundColor
            foreground = textColor
            alignmentX = Component.LEFT_ALIGNMENT
            font = Font("Dialog", Font.PLAIN, 12)

            // Add some padding around the checkbox text
            border = BorderFactory.createEmptyBorder(2, 0, 2, 0)

            if (key == "all") {
                // Special handling for "Show All Labels" checkbox
                font = Font("Dialog", Font.BOLD, 12)
                addActionListener {
                    // Update all other checkboxes to match this one's state
                    val newState = isSelected
                    labelOptions.keys.filter { it != "all" }.forEach { labelKey ->
                        checkboxes[labelKey]?.isSelected = newState
                        gridEditor.updateLabelVisibility(labelKey, newState)
                    }
                }
            } else {
                // Regular checkbox behavior
                addActionListener {
                    gridEditor.updateLabelVisibility(key, isSelected)

                    // Update "all" checkbox state based on other checkboxes
                    updateAllCheckboxState()
                }
            }
        }

        // Store reference to checkbox
        checkboxes[key] = checkbox

        // Add to panel with visual indentation for non-"all" checkboxes
        if (key != "all") {
            panel.add(Box.createHorizontalStrut(15), checkbox)
        }
        panel.add(checkbox)
        panel.add(Box.createVerticalStrut(4)) // Slightly more spacing between checkboxes
    }

    private fun updateAllCheckboxState() {
        checkboxes["all"]?.let { allCheckbox ->
            // Check if all other checkboxes are selected
            val allSelected = labelOptions.keys
                .filter { it != "all" }
                .all { checkboxes[it]?.isSelected == true }

            // Update the "all" checkbox without triggering its action listener
            val listener = allCheckbox.actionListeners.firstOrNull()
            if (listener != null) {
                allCheckbox.removeActionListener(listener)
                allCheckbox.isSelected = allSelected
                allCheckbox.addActionListener(listener)
            } else {
                allCheckbox.isSelected = allSelected
            }
        }
    }

    /**
     * Updates a specific checkbox state programmatically
     */
    fun setCheckboxState(key: String, state: Boolean) {
        if (key == "all") {
            // When setting the "all" checkbox, update all other checkboxes
            labelOptions.keys.filter { it != "all" }.forEach { labelKey ->
                checkboxes[labelKey]?.isSelected = state
                gridEditor.updateLabelVisibility(labelKey, state)
            }
        }

        checkboxes[key]?.let { checkbox ->
            checkbox.isSelected = state
            gridEditor.updateLabelVisibility(key, state)
        }

        // Update the "all" checkbox state if needed
        if (key != "all") {
            updateAllCheckboxState()
        }
    }

    /**
     * Get the current state of a specific display option
     */
    fun getCheckboxState(key: String): Boolean {
        return checkboxes[key]?.isSelected ?: false
    }

    /**
     * Get all current display options as a map
     * @return Map of option keys to their boolean states
     */
    fun getAllOptions(): Map<String, Boolean> {
        val options = mutableMapOf<String, Boolean>()
        checkboxes.forEach { (key, checkbox) ->
            options[key] = checkbox.isSelected
        }
        return options
    }

    /**
     * Reset all options to their default states
     */
    fun resetToDefaults() {
        labelOptions.forEach { (key, value) ->
            val (_, defaultState) = value
            setCheckboxState(key, defaultState)
        }
    }

    /**
     * Save the current display options to file
     */
    fun saveSettings(): Boolean {
        return settingsSaver.saveDisplayOptions(this)
    }

    /**
     * Load display options from file
     */
    fun loadSettings(): Boolean {
        return settingsSaver.loadDisplayOptions(this)
    }
}