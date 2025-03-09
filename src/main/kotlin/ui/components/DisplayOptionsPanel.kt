package ui.components

import SettingsSaver
import grideditor.GridEditor
import java.awt.Color
import java.awt.Component
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JPanel
import java.awt.BorderLayout
import javax.swing.BorderFactory

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
    private val settingsSaver = SettingsSaver()

    init {
        // Setup panel properties
        layout = BorderLayout()
        background = Color(40, 44, 52)

        val optionsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }

        // Create checkboxes for each label option
        labelOptions.forEach { (key, value) ->
            val (label, defaultState) = value
            createCheckbox(key, label, defaultState, optionsPanel)
        }

        // Add the options panel to the main panel
        add(optionsPanel, BorderLayout.CENTER)

        // Add buttons panel for save/load functionality
        val buttonsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)  // Changed from X_AXIS to Y_AXIS
            background = Color(40, 44, 52)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }

        // Save button
        val saveButton = JButton("Save Settings").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            alignmentX = Component.LEFT_ALIGNMENT  // Added for vertical alignment
            addActionListener {
                if (settingsSaver.saveDisplayOptions(this@DisplayOptionsPanel)) {
                    println("Display settings saved successfully.")
                } else {
                    println("Failed to save display settings.")
                }
            }
        }

        // Load button
        val loadButton = JButton("Load Settings").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            alignmentX = Component.LEFT_ALIGNMENT  // Added for vertical alignment
            addActionListener {
                if (settingsSaver.loadDisplayOptions(this@DisplayOptionsPanel)) {
                    println("Display settings loaded successfully.")
                } else {
                    println("Failed to load display settings.")
                }
            }
        }

        // Reset button
        val resetButton = JButton("Reset").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            alignmentX = Component.LEFT_ALIGNMENT  // Added for vertical alignment
            addActionListener {
                resetToDefaults()
                println("Display settings reset to defaults.")
            }
        }

        // Add buttons to panel with vertical struts instead of horizontal
        buttonsPanel.add(saveButton)
        buttonsPanel.add(Box.createVerticalStrut(5))  // Changed from horizontal to vertical
        buttonsPanel.add(loadButton)
        buttonsPanel.add(Box.createVerticalStrut(5))  // Changed from horizontal to vertical
        buttonsPanel.add(resetButton)

        // Add buttons panel to the main panel
        add(buttonsPanel, BorderLayout.SOUTH)

        // Try to load saved settings on initialization
        settingsSaver.loadDisplayOptions(this)
    }

    /**
     * Creates a checkbox for controlling label visibility
     */
    private fun createCheckbox(key: String, label: String, defaultState: Boolean, panel: JPanel) {
        val checkbox = JCheckBox(label).apply {
            isSelected = defaultState
            background = Color(40, 44, 52)
            foreground = Color.WHITE
            alignmentX = Component.LEFT_ALIGNMENT

            if (key == "all") {
                // Special handling for "Show All Labels" checkbox
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

        // Add to panel
        panel.add(checkbox)
        panel.add(Box.createVerticalStrut(2))
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