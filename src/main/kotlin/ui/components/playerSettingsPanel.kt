package ui.components

import Game3D
import player.Player
import java.awt.*
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.ChangeListener

class PlayerSettingsPanel(private val player: Player, private val game3D: Game3D) : JPanel() {
    companion object {
        // Define default values - match the ones from Player class constructor
        private const val DEFAULT_MOVE_SPEED = 0.05
        private const val DEFAULT_PLAYER_RADIUS = 0.3
        private const val DEFAULT_PLAYER_HEIGHT = 1.7
        private const val DEFAULT_HEAD_CLEARANCE = 0.3
        private const val DEFAULT_GRAVITY_ENABLED = false
        private const val DEFAULT_GRAVITY = 0.01
        private const val DEFAULT_JUMP_STRENGTH = 0.2
        private const val DEFAULT_TERMINAL_VELOCITY = -0.5
    }

    // --- Components for Player Settings ---
    private val gravityToggle: JCheckBox
    private val moveSpeedSpinner: JSpinner
    private val playerRadiusSpinner: JSpinner
    private val playerHeightSpinner: JSpinner
    private val headClearanceSpinner: JSpinner
    private val gravityForceSpinner: JSpinner
    private val jumpStrengthSpinner: JSpinner
    private val terminalVelocitySpinner: JSpinner

    // Flag to prevent recursive updates during initialization
    private var isInitializing = true

    // Constants for styling
    private val BACKGROUND_COLOR = Color(40, 44, 52)
    private val DARKER_BG_COLOR = Color(30, 33, 40)
    private val LIGHTER_BG_COLOR = Color(45, 48, 55)
    private val ACCENT_COLOR = Color(220, 95, 60) // Warm orange/red color from AboutDialog
    private val TEXT_COLOR = Color.WHITE
    private val SECONDARY_TEXT_COLOR = Color(180, 180, 180)
    private val CONTROL_BG_COLOR = Color(60, 63, 65)
    private val CONTROL_BORDER_COLOR = Color(80, 83, 85)

    // Map to store the full label texts (for resizing with ellipsis)
    private val labelTextMap = mutableMapOf<JLabel, String>()

    init {
        val player = player
        // Gravity Toggle - Styled
        gravityToggle = createStyledCheckBox("Enable Gravity", player.gravityEnabled).apply {
            addActionListener {
                game3D.changeGravityEnabled(isSelected)
                updateGravityComponentStates(isSelected)
            }
        }

        // Create styled spinners
        moveSpeedSpinner = createStyledSpinner(
            SpinnerNumberModel(player.moveSpeed, 0.001, 1.0, 0.005)
        ).apply {
            addChangeListener(createNumericChangeListener { player.moveSpeed = it })
        }

        playerRadiusSpinner = createStyledSpinner(
            SpinnerNumberModel(player.playerRadius, 0.1, 1.0, 0.05)
        ).apply {
            addChangeListener(createNumericChangeListener { player.playerRadius = it })
        }

        playerHeightSpinner = createStyledSpinner(
            SpinnerNumberModel(player.playerHeight, 0.5, 3.0, 0.1)
        ).apply {
            addChangeListener(createNumericChangeListener { player.playerHeight = it })
        }

        headClearanceSpinner = createStyledSpinner(
            SpinnerNumberModel(player.headClearance, 0.0, 1.0, 0.05)
        ).apply {
            addChangeListener(createNumericChangeListener { player.headClearance = it })
        }

        gravityForceSpinner = createStyledSpinner(
            SpinnerNumberModel(player.gravity, 0.0, 0.1, 0.001)
        ).apply {
            addChangeListener(createNumericChangeListener { player.gravity = it })
        }

        jumpStrengthSpinner = createStyledSpinner(
            SpinnerNumberModel(player.jumpStrength, 0.0, 1.0, 0.01)
        ).apply {
            addChangeListener(createNumericChangeListener { player.jumpStrength = it })
        }

        terminalVelocitySpinner = createStyledSpinner(
            SpinnerNumberModel(kotlin.math.abs(player.terminalVelocity), 0.0, 2.0, 0.05)
        ).apply {
            addChangeListener(createNumericChangeListener {
                player.terminalVelocity = -it
            })
        }

        // --- Setup Panel Layout ---
        setupPanel()

        // --- Final Initialization Steps ---
        updateGravityComponentStates(player.gravityEnabled)
        isInitializing = false

        // Add a component listener to handle resizing
        addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                resizeLabels()
            }
        })
    }

    // Helper function to create ChangeListener for spinners
    private fun createNumericChangeListener(updateAction: (Double) -> Unit): ChangeListener {
        return ChangeListener { e ->
            if (!isInitializing) {
                val spinner = e.source as JSpinner
                val value = (spinner.value as? Number)?.toDouble() ?: 0.0
                updateAction(value)
            }
        }
    }

    // Create styled checkbox
    private fun createStyledCheckBox(text: String, selected: Boolean): JCheckBox {
        return JCheckBox(text, selected).apply {
            foreground = TEXT_COLOR
            background = DARKER_BG_COLOR
            font = Font("Arial", Font.BOLD, 12)
            alignmentX = Component.LEFT_ALIGNMENT
            isFocusPainted = false

            // Add hover effect
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    foreground = ACCENT_COLOR
                }

                override fun mouseExited(e: MouseEvent) {
                    foreground = TEXT_COLOR
                }
            })
        }
    }

    // Create styled JSpinner with responsive sizing
    private fun createStyledSpinner(model: SpinnerModel): JSpinner {
        return JSpinner(model).apply {
            background = CONTROL_BG_COLOR
            foreground = TEXT_COLOR

            // Make spinner resize with panel
            val minWidth = 60
            val prefWidth = 80
            val maxWidth = 120

            minimumSize = Dimension(minWidth, 25)
            preferredSize = Dimension(prefWidth, 25)
            maximumSize = Dimension(maxWidth, 25)

            // Style the text field component of the spinner
            (editor as? JSpinner.DefaultEditor)?.let { editor ->
                editor.textField.apply {
                    background = CONTROL_BG_COLOR
                    foreground = TEXT_COLOR
                    border = BorderFactory.createLineBorder(CONTROL_BORDER_COLOR)
                    font = Font("Arial", Font.PLAIN, 12)
                }
            }

            // Style the spinner buttons
            UIManager.put("Spinner.arrowButtonBorder", BorderFactory.createEmptyBorder())
            UIManager.put("Spinner.arrowButtonInsets", Insets(0, 0, 0, 0))
            UIManager.put("Spinner.arrowButtonSize", Dimension(16, 8))
            UIManager.put("Spinner.buttonBackground", CONTROL_BORDER_COLOR)
            UIManager.put("Spinner.buttonForeground", TEXT_COLOR)
        }
    }

    // Create styled label with ellipsis capability
    private fun createStyledLabel(text: String): JLabel {
        return JLabel(text).apply {
            foreground = TEXT_COLOR
            font = Font("Arial", Font.BOLD, 12)

            // Store the original text for resizing
            labelTextMap[this] = text
        }
    }

    // Helper function to create a styled setting row with GridBagLayout for responsiveness
    private fun createSettingRow(labelText: String, component: JComponent): JPanel {
        return JPanel().apply {
            layout = GridBagLayout()
            background = DARKER_BG_COLOR
            border = EmptyBorder(5, 10, 5, 10)

            val label = createStyledLabel(labelText)

            // GridBag constraints for the label
            val labelConstraints = GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                weightx = 0.7
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.WEST
                insets = Insets(0, 0, 0, 10)
            }

            // GridBag constraints for the component
            val componentConstraints = GridBagConstraints().apply {
                gridx = 1
                gridy = 0
                weightx = 0.3
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.EAST
            }

            add(label, labelConstraints)
            add(component, componentConstraints)

            // Add subtle highlight effect on hover
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    background = Color(DARKER_BG_COLOR.red + 5, DARKER_BG_COLOR.green + 5, DARKER_BG_COLOR.blue + 5)
                }

                override fun mouseExited(e: MouseEvent) {
                    background = DARKER_BG_COLOR
                }
            })
        }
    }

    // Method to resize labels with ellipsis when space is limited
    private fun resizeLabels() {
        labelTextMap.forEach { (label, fullText) ->
            val metrics = label.getFontMetrics(label.font)
            val availableWidth = label.width - 10  // Leave some padding

            if (availableWidth <= 0) return  // Skip if not properly laid out yet

            if (metrics.stringWidth(fullText) > availableWidth) {
                // Need to truncate
                var truncatedText = fullText
                while (metrics.stringWidth(truncatedText + "...") > availableWidth && truncatedText.isNotEmpty()) {
                    truncatedText = truncatedText.dropLast(1)
                }

                if (truncatedText.isNotEmpty()) {
                    label.text = truncatedText + "..."
                } else {
                    label.text = "..."
                }
            } else {
                // Restore original text
                label.text = fullText
            }
        }
    }

    // Create a stylized separator similar to AboutDialog
    private fun createStylizedSeparator(): JComponent {
        return object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                val width = this.width
                val y = this.height / 2

                val gradient = LinearGradientPaint(
                    0f, y.toFloat(), width.toFloat(), y.toFloat(),
                    floatArrayOf(0.0f, 0.5f, 1.0f),
                    arrayOf(DARKER_BG_COLOR, ACCENT_COLOR, DARKER_BG_COLOR)
                )

                g2d.stroke = BasicStroke(2f)
                g2d.paint = gradient
                g2d.drawLine(0, y, width, y)
            }

            init {
                preferredSize = Dimension(1, 10)
            }
        }
    }

    private fun setupPanel() {
        // Use a gradient background panel like in AboutDialog
        setLayout(BorderLayout())
        background = BACKGROUND_COLOR

        // Create main content panel with gradient background
        val contentPanel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                val gradientPaint = GradientPaint(
                    0f, 0f, DARKER_BG_COLOR,
                    0f, height.toFloat(), LIGHTER_BG_COLOR
                )
                g2d.paint = gradientPaint
                g2d.fillRect(0, 0, width, height)
            }
        }.apply {
            layout = BorderLayout()
            border = EmptyBorder(15, 15, 15, 15)
        }

        add(contentPanel, BorderLayout.CENTER)

        // Title Panel
        val titlePanel = JPanel().apply {
            layout = BorderLayout(0, 5)
            isOpaque = false

            // Title
            add(JLabel("PLAYER SETTINGS", SwingConstants.CENTER).apply {
                foreground = ACCENT_COLOR
                font = Font("Impact", Font.BOLD, 18)
                border = EmptyBorder(0, 0, 5, 0)
            }, BorderLayout.NORTH)

            // Stylized separator
            add(createStylizedSeparator(), BorderLayout.CENTER)
        }

        contentPanel.add(titlePanel, BorderLayout.NORTH)

        // Settings Content Panel - using GridBagLayout for better responsiveness
        val settingsPanel = JPanel().apply {
            layout = GridBagLayout()
            isOpaque = false
            border = EmptyBorder(10, 5, 10, 5)
        }

        contentPanel.add(settingsPanel, BorderLayout.CENTER)

        // Setup GridBagConstraints for the main layout
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            gridx = 0
            insets = Insets(0, 0, 10, 0)
        }

        // Basic Physics Settings Section
        val basicSettingsPanel = JPanel().apply {
            layout = GridBagLayout()
            isOpaque = false
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)

            val labelGbc = GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.WEST
                insets = Insets(0, 5, 5, 0)
            }

            add(JLabel("Basic Settings").apply {
                foreground = ACCENT_COLOR
                font = Font("Arial", Font.BOLD, 14)
                border = EmptyBorder(0, 5, 5, 0)
            }, labelGbc)

            val rowGbc = GridBagConstraints().apply {
                gridx = 0
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                insets = Insets(2, 0, 2, 0)
            }

            rowGbc.gridy = 1
            add(createSettingRow("Move Speed:", moveSpeedSpinner), rowGbc)

            rowGbc.gridy = 2
            add(createSettingRow("Player Radius:", playerRadiusSpinner), rowGbc)

            rowGbc.gridy = 3
            add(createSettingRow("Player Height:", playerHeightSpinner), rowGbc)

            rowGbc.gridy = 4
            add(createSettingRow("Head Clearance:", headClearanceSpinner), rowGbc)
        }

        gbc.gridy = 0
        settingsPanel.add(basicSettingsPanel, gbc)

        // Gravity Settings Section
        val gravitySettingsPanel = JPanel().apply {
            layout = GridBagLayout()
            isOpaque = false
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)

            val toggleGbc = GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.WEST
                insets = Insets(0, 0, 5, 0)
            }

            // Gravity Toggle in its own panel
            add(JPanel().apply {
                layout = BorderLayout()
                background = DARKER_BG_COLOR
                border = EmptyBorder(5, 10, 5, 10)
                add(gravityToggle, BorderLayout.WEST)
            }, toggleGbc)

            val labelGbc = GridBagConstraints().apply {
                gridx = 0
                gridy = 1
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.WEST
                insets = Insets(5, 5, 5, 0)
            }

            add(JLabel("Gravity Parameters").apply {
                foreground = ACCENT_COLOR
                font = Font("Arial", Font.BOLD, 14)
                border = EmptyBorder(5, 5, 5, 0)
            }, labelGbc)

            val rowGbc = GridBagConstraints().apply {
                gridx = 0
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                insets = Insets(2, 0, 2, 0)
            }

            rowGbc.gridy = 2
            add(createSettingRow("Gravity Force:", gravityForceSpinner), rowGbc)

            rowGbc.gridy = 3
            add(createSettingRow("Jump Strength:", jumpStrengthSpinner), rowGbc)

            rowGbc.gridy = 4
            add(createSettingRow("Term. Velocity:", terminalVelocitySpinner), rowGbc)
        }

        gbc.gridy = 1
        settingsPanel.add(gravitySettingsPanel, gbc)

        // Add reset button at bottom
        contentPanel.add(JPanel().apply {
            isOpaque = false
            layout = FlowLayout(FlowLayout.RIGHT)

            add(JButton("Reset to Defaults").apply {
                foreground = TEXT_COLOR
                background = CONTROL_BG_COLOR
                font = Font("Arial", Font.BOLD, 12)
                preferredSize = Dimension(130, 28)
                isFocusPainted = false
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(CONTROL_BORDER_COLOR),
                    BorderFactory.createEmptyBorder(4, 10, 4, 10)
                )

                // Hover effect
                addMouseListener(object : MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent) {
                        background = CONTROL_BORDER_COLOR
                    }

                    override fun mouseExited(e: MouseEvent) {
                        background = CONTROL_BG_COLOR
                    }
                })

                addActionListener {
                    // Reset player values to defaults
                    game3D.player.moveSpeed = DEFAULT_MOVE_SPEED
                    game3D.player.playerRadius = DEFAULT_PLAYER_RADIUS
                    game3D.player.playerHeight = DEFAULT_PLAYER_HEIGHT
                    game3D.player.headClearance = DEFAULT_HEAD_CLEARANCE
                    game3D.player.gravity = DEFAULT_GRAVITY
                    game3D.player.jumpStrength = DEFAULT_JUMP_STRENGTH
                    game3D.player.terminalVelocity = DEFAULT_TERMINAL_VELOCITY
                    game3D.changeGravityEnabled(DEFAULT_GRAVITY_ENABLED)

                    // Update UI to reflect these changes
                    updateAllSettingsDisplay()
                }
            })
        }, BorderLayout.SOUTH)
    }

    // Enable/disable gravity-related spinners based on toggle state
    private fun updateGravityComponentStates(isEnabled: Boolean) {
        gravityForceSpinner.isEnabled = isEnabled
        jumpStrengthSpinner.isEnabled = isEnabled
        terminalVelocitySpinner.isEnabled = isEnabled
    }

    private fun updateAllSettingsDisplay() {
        isInitializing = true
        val player = game3D.player

        gravityToggle.isSelected = player.gravityEnabled
        moveSpeedSpinner.value = player.moveSpeed
        playerRadiusSpinner.value = player.playerRadius
        playerHeightSpinner.value = player.playerHeight
        headClearanceSpinner.value = player.headClearance
        gravityForceSpinner.value = player.gravity
        jumpStrengthSpinner.value = player.jumpStrength
        terminalVelocitySpinner.value = kotlin.math.abs(player.terminalVelocity)

        // Update enabled state of gravity components
        updateGravityComponentStates(player.gravityEnabled)

        isInitializing = false
        repaint() // Ensure panel redraws if needed
    }

    fun refreshFromGameState() {
        updateAllSettingsDisplay()
    }
}