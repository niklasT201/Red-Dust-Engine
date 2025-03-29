package ui.components

import player.Player
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class PlayerSettingsPanel(private val player: Player) : JPanel(), ChangeListener {
    // UI Components
    private val gravityToggle = JCheckBox("Enable Gravity").apply {
        background = Color(40, 44, 52)
        foreground = Color.WHITE
        isSelected = player.gravityEnabled
        alignmentX = Component.LEFT_ALIGNMENT
        addActionListener {
            player.setGravity(isSelected)
        }
    }

    // Sliders for numeric values with labels
    private val moveSpeedSlider = createSlider(1, 50, (player.moveSpeed * 100).toInt(), "Move Speed: ")
    private val playerRadiusSlider = createSlider(1, 100, (player.playerRadius * 100).toInt(), "Player Radius: ")
    private val playerHeightSlider = createSlider(100, 250, (player.playerHeight * 100).toInt(), "Player Height: ")
    private val headClearanceSlider = createSlider(1, 100, (player.headClearance * 100).toInt(), "Head Clearance: ")
    private val gravitySlider = createSlider(1, 50, (player.gravity * 1000).toInt(), "Gravity: ")
    private val jumpStrengthSlider = createSlider(1, 100, (player.jumpStrength * 100).toInt(), "Jump Strength: ")
    private val terminalVelocitySlider = createSlider(1, 100, (-player.terminalVelocity * 100).toInt(), "Terminal Velocity: ")

    // Labels to display current values
    private val moveSpeedLabel = JLabel("${player.moveSpeed}")
    private val playerRadiusLabel = JLabel("${player.playerRadius}")
    private val playerHeightLabel = JLabel("${player.playerHeight}")
    private val headClearanceLabel = JLabel("${player.headClearance}")
    private val gravityLabel = JLabel("${player.gravity}")
    private val jumpStrengthLabel = JLabel("${player.jumpStrength}")
    private val terminalVelocityLabel = JLabel("${player.terminalVelocity}")

    // Reset button
    private val resetButton = JButton("Reset to Defaults").apply {
        background = Color(60, 63, 65)
        foreground = Color.WHITE
        addActionListener {
            resetToDefaults()
        }
    }

    init {
        setupPanel()
    }

    private fun setupPanel() {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)

        // Create panels for each section
        val movementPanel = createSectionPanel("Movement")
        movementPanel.add(createLabeledSlider("Move Speed:", moveSpeedSlider, moveSpeedLabel))
        movementPanel.add(createLabeledSlider("Player Radius:", playerRadiusSlider, playerRadiusLabel))
        movementPanel.add(createLabeledSlider("Player Height:", playerHeightSlider, playerHeightLabel))
        movementPanel.add(createLabeledSlider("Head Clearance:", headClearanceSlider, headClearanceLabel))

        val physicsPanel = createSectionPanel("Physics")
        physicsPanel.add(gravityToggle)
        physicsPanel.add(Box.createVerticalStrut(5))
        physicsPanel.add(createLabeledSlider("Gravity:", gravitySlider, gravityLabel))
        physicsPanel.add(createLabeledSlider("Jump Strength:", jumpStrengthSlider, jumpStrengthLabel))
        physicsPanel.add(createLabeledSlider("Terminal Velocity:", terminalVelocitySlider, terminalVelocityLabel))

        // Add sections to main panel
        add(movementPanel)
        add(Box.createVerticalStrut(10))
        add(physicsPanel)
        add(Box.createVerticalStrut(10))

        // Add reset button
        val buttonPanel = JPanel().apply {
            background = Color(40, 44, 52)
            add(resetButton)
        }
        add(buttonPanel)
        add(Box.createVerticalGlue())

        // Apply border with appropriate padding
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color(70, 73, 75)),
                "Player Settings",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                Color.WHITE
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        )

        // Set preferred size
        preferredSize = Dimension(300, 500)
    }

    private fun createSectionPanel(title: String): JPanel {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color(70, 73, 75)),
                    title,
                    TitledBorder.LEFT,
                    TitledBorder.TOP,
                    null,
                    Color.WHITE
                ),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            )
            alignmentX = Component.LEFT_ALIGNMENT
        }
        return panel
    }

    private fun createSlider(min: Int, max: Int, value: Int, sliderName: String): JSlider {
        val slider = JSlider(JSlider.HORIZONTAL, min, max, value)
        slider.background = Color(40, 44, 52)
        slider.foreground = Color.WHITE
        slider.paintTicks = true
        slider.paintLabels = false
        slider.majorTickSpacing = (max - min) / 5
        slider.minorTickSpacing = (max - min) / 10
        slider.name = sliderName
        slider.addChangeListener(this@PlayerSettingsPanel)
        return slider
    }

    private fun createLabeledSlider(labelText: String, slider: JSlider, valueLabel: JLabel): JPanel {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        val labelPanel = JPanel().apply {
            layout = GridLayout(1, 2)
            background = Color(40, 44, 52)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        val textLabel = JLabel(labelText).apply {
            foreground = Color.WHITE
        }

        valueLabel.foreground = Color.WHITE

        labelPanel.add(textLabel)
        labelPanel.add(valueLabel)

        panel.add(labelPanel)
        panel.add(slider)
        panel.add(Box.createVerticalStrut(5))

        return panel
    }

    override fun stateChanged(e: ChangeEvent) {
        val source = e.source as JSlider

        when (source) {
            moveSpeedSlider -> {
                val value = source.value / 100.0
                player.moveSpeed = value
                moveSpeedLabel.text = String.format("%.2f", value)
            }
            playerRadiusSlider -> {
                val value = source.value / 100.0
                player.playerRadius = value
                playerRadiusLabel.text = String.format("%.2f", value)
            }
            playerHeightSlider -> {
                val value = source.value / 100.0
                player.playerHeight = value
                playerHeightLabel.text = String.format("%.2f", value)
            }
            headClearanceSlider -> {
                val value = source.value / 100.0
                player.headClearance = value
                headClearanceLabel.text = String.format("%.2f", value)
            }
            gravitySlider -> {
                val value = source.value / 1000.0
                player.gravity = value
                gravityLabel.text = String.format("%.3f", value)
            }
            jumpStrengthSlider -> {
                val value = source.value / 100.0
                player.jumpStrength = value
                jumpStrengthLabel.text = String.format("%.2f", value)
            }
            terminalVelocitySlider -> {
                val value = -(source.value / 100.0)
                player.terminalVelocity = value
                terminalVelocityLabel.text = String.format("%.2f", value)
            }
        }

        // Update all player settings at once for movement-related changes
        if (source in listOf(moveSpeedSlider, playerRadiusSlider, playerHeightSlider, headClearanceSlider)) {
            player.setMovementSettings(
                player.moveSpeed,
                player.playerRadius,
                player.playerHeight,
                player.headClearance
            )
        }
    }

    private fun resetToDefaults() {
        // Default values from the Player class constructor
        moveSpeedSlider.value = 5 // 0.05
        playerRadiusSlider.value = 30 // 0.3
        playerHeightSlider.value = 170 // 1.7
        headClearanceSlider.value = 30 // 0.3
        gravityToggle.isSelected = false
        gravitySlider.value = 10 // 0.01
        jumpStrengthSlider.value = 20 // 0.2
        terminalVelocitySlider.value = 50 // -0.5

        // Update the player with default values
        player.setMovementSettings(0.05, 0.3, 1.7, 0.3)
        player.setGravity(false)
        player.gravity = 0.01
        player.jumpStrength = 0.2
        player.terminalVelocity = -0.5
    }
}