package ui.components

import Game3D // Import your main game class
import java.awt.Color
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.event.ChangeListener

class PlayerSettingsPanel(private val game3D: Game3D) : JPanel() {

    // --- Components for Player Settings ---
    private val gravityToggle: JCheckBox
    private val moveSpeedSpinner: JSpinner
    private val playerRadiusSpinner: JSpinner
    private val playerHeightSpinner: JSpinner
    private val headClearanceSpinner: JSpinner
    private val gravityForceSpinner: JSpinner // Renamed from 'gravity' to avoid conflict
    private val jumpStrengthSpinner: JSpinner
    private val terminalVelocitySpinner: JSpinner

    // Flag to prevent recursive updates during initialization
    private var isInitializing = true

    init {
        val player = game3D.player // Get reference to player object

        // Gravity Toggle
        gravityToggle = JCheckBox("Enable Gravity", player.gravityEnabled).apply {
            background = Color(40, 44, 52)
            foreground = Color.WHITE
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                game3D.changeGravityEnabled(isSelected) // Assumes this method exists in Game3D
                updateGravityComponentStates(isSelected) // Enable/disable gravity spinners
            }
        }

        // Move Speed Spinner
        moveSpeedSpinner = JSpinner(
            SpinnerNumberModel(player.moveSpeed, 0.001, 1.0, 0.005) // value, min, max, step
        ).apply {
            addChangeListener(createNumericChangeListener { player.moveSpeed = it })
        }

        // Player Radius Spinner
        playerRadiusSpinner = JSpinner(
            SpinnerNumberModel(player.playerRadius, 0.1, 1.0, 0.05)
        ).apply {
            addChangeListener(createNumericChangeListener { player.playerRadius = it })
        }

        // Player Height Spinner
        playerHeightSpinner = JSpinner(
            SpinnerNumberModel(player.playerHeight, 0.5, 3.0, 0.1)
        ).apply {
            addChangeListener(createNumericChangeListener { player.playerHeight = it })
        }

        // Head Clearance Spinner
        headClearanceSpinner = JSpinner(
            SpinnerNumberModel(player.headClearance, 0.0, 1.0, 0.05)
        ).apply {
            addChangeListener(createNumericChangeListener { player.headClearance = it })
        }

        // Gravity Force Spinner
        gravityForceSpinner = JSpinner(
            SpinnerNumberModel(player.gravity, 0.0, 0.1, 0.001) // Gravity force should be positive
        ).apply {
            addChangeListener(createNumericChangeListener { player.gravity = it })
        }

        // Jump Strength Spinner
        jumpStrengthSpinner = JSpinner(
            SpinnerNumberModel(player.jumpStrength, 0.0, 1.0, 0.01) // Jump strength usually positive
        ).apply {
            addChangeListener(createNumericChangeListener { player.jumpStrength = it })
        }

        // Terminal Velocity Spinner
        // Note: Model stores positive value, but we apply it negatively in Player class
        terminalVelocitySpinner = JSpinner(
            SpinnerNumberModel(kotlin.math.abs(player.terminalVelocity), 0.0, 2.0, 0.05)
        ).apply {
            addChangeListener(createNumericChangeListener {
                // Store absolute value, Player class handles making it negative
                player.terminalVelocity = -it // Ensure it's set negatively in the player
            })
        }

        // --- Setup Panel Layout ---
        setupPanel()

        // --- Final Initialization Steps ---
        updateGravityComponentStates(player.gravityEnabled) // Set initial enabled state
        isInitializing = false // Allow listeners to function now
    }

    // Helper function to create ChangeListener for spinners
    private fun createNumericChangeListener(updateAction: (Double) -> Unit): ChangeListener {
        return ChangeListener { e ->
            if (!isInitializing) { // Only update if not during setup
                val spinner = e.source as JSpinner
                val value = (spinner.value as? Number)?.toDouble() ?: 0.0 // Safely get value
                updateAction(value)
            }
        }
    }

    // Helper function to create a row with label and spinner
    private fun createSettingRow(labelText: String, component: JComponent): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 2)).apply { // Use FlowLayout for horizontal arrangement
            background = Color(40, 44, 52) // Match style
            alignmentX = Component.LEFT_ALIGNMENT

            val label = JLabel(labelText).apply {
                foreground = Color.WHITE
                // Optional: Set preferred size for alignment
                // preferredSize = Dimension(100, preferredSize.height)
            }
            // Set spinner preferred size for consistency
            component.preferredSize = component.preferredSize.apply { width = 80 }

            add(label)
            add(component)
        }
        return panel
    }


    private fun setupPanel() {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)

        // Add components using the helper function for rows
        add(createSettingRow("Move Speed:", moveSpeedSpinner))
        add(createSettingRow("Player Radius:", playerRadiusSpinner))
        add(createSettingRow("Player Height:", playerHeightSpinner))
        add(createSettingRow("Head Clearance:", headClearanceSpinner))
        add(Box.createVerticalStrut(10)) // Spacer

        // Add gravity toggle separately as it's not a label/spinner pair
        add(gravityToggle)
        add(Box.createVerticalStrut(5)) // Spacer

        // Add gravity-related spinners
        add(createSettingRow("Gravity Force:", gravityForceSpinner))
        add(createSettingRow("Jump Strength:", jumpStrengthSpinner))
        add(createSettingRow("Term. Velocity:", terminalVelocitySpinner)) // abs value shown


        // Apply border
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color(70, 73, 75)),
                "Player Settings",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                Color.WHITE
            ),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        )

        alignmentX = Component.LEFT_ALIGNMENT
    }

    // Enable/disable gravity-related spinners based on toggle state
    private fun updateGravityComponentStates(isEnabled: Boolean) {
        gravityForceSpinner.isEnabled = isEnabled
        jumpStrengthSpinner.isEnabled = isEnabled
        terminalVelocitySpinner.isEnabled = isEnabled
    }

    fun updateAllSettingsDisplay() {
        isInitializing = true // Prevent listeners firing during update
        val player = game3D.player

        gravityToggle.isSelected = player.gravityEnabled
        moveSpeedSpinner.value = player.moveSpeed
        playerRadiusSpinner.value = player.playerRadius
        playerHeightSpinner.value = player.playerHeight
        headClearanceSpinner.value = player.headClearance
        gravityForceSpinner.value = player.gravity
        jumpStrengthSpinner.value = player.jumpStrength
        terminalVelocitySpinner.value = kotlin.math.abs(player.terminalVelocity) // Display absolute value

        updateGravityComponentStates(player.gravityEnabled) // Sync enabled state too
        isInitializing = false
    }
}