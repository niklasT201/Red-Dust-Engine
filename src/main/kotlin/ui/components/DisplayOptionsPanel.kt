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
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }

        // Save button
        val saveButton = JButton("Save Settings").apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
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
            addActionListener {
                resetToDefaults()
                println("Display settings reset to defaults.")
            }
        }

        // Add buttons to panel
        buttonsPanel.add(saveButton)
        buttonsPanel.add(Box.createHorizontalStrut(5))
        buttonsPanel.add(loadButton)
        buttonsPanel.add(Box.createHorizontalStrut(5))
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
            addActionListener {
                gridEditor.updateLabelVisibility(key, isSelected)
            }
        }

        // Store reference to checkbox
        checkboxes[key] = checkbox

        // Add to panel
        panel.add(checkbox)
        panel.add(Box.createVerticalStrut(2))
    }

    /**
     * Updates a specific checkbox state programmatically
     */
    fun setCheckboxState(key: String, state: Boolean) {
        checkboxes[key]?.let { checkbox ->
            checkbox.isSelected = state
            gridEditor.updateLabelVisibility(key, state)
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