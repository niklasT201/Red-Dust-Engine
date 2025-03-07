package ui.components

import grideditor.GridEditor
import java.awt.Color
import java.awt.Component
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JPanel

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

    init {
        // Setup panel properties
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)

        // Create checkboxes for each label option
        labelOptions.forEach { (key, value) ->
            val (label, defaultState) = value
            createCheckbox(key, label, defaultState)
        }
    }

    /**
     * Creates a checkbox for controlling label visibility
     */
    private fun createCheckbox(key: String, label: String, defaultState: Boolean) {
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
        add(checkbox)
        add(Box.createVerticalStrut(2))
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
     * Reset all options to their default states
     */
    fun resetToDefaults() {
        labelOptions.forEach { (key, value) ->
            val (_, defaultState) = value
            setCheckboxState(key, defaultState)
        }
    }
}