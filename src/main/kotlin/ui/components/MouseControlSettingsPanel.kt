package ui.components

import player.Camera
import Renderer
import java.awt.*
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.ChangeListener
import kotlin.math.PI

class MouseControlSettingsPanel(private val camera: Camera, private val renderer: Renderer) : JPanel() {
    companion object {
        // Define default values
        private const val DEFAULT_ROTATION_SPEED = 0.003
        private const val DEFAULT_FOV = PI/3
    }

    // --- Components for Mouse Control Settings ---
    private val rotationSpeedSpinner: JSpinner
    private val fovSpinner: JSpinner
    private val invertYAxisCheckbox: JCheckBox
    private val fovSlider: JSlider
    private val sensitivitySlider: JSlider

    // Flag to prevent recursive updates during initialization
    private var isInitializing = true

    // Constants for styling
    private val BACKGROUND_COLOR = Color(40, 44, 52)
    private val DARKER_BG_COLOR = Color(30, 33, 40)
    private val LIGHTER_BG_COLOR = Color(45, 48, 55)
    private val ACCENT_COLOR = Color(220, 95, 60) // Warm orange/red color
    private val TEXT_COLOR = Color.WHITE
    private val SECONDARY_TEXT_COLOR = Color(180, 180, 180)
    private val CONTROL_BG_COLOR = Color(60, 63, 65)
    private val CONTROL_BORDER_COLOR = Color(80, 83, 85)

    // Map to store the full label texts (for resizing with ellipsis)
    private val labelTextMap = mutableMapOf<JLabel, String>()

    init {
        // Create styled spinners
        rotationSpeedSpinner = createStyledSpinner(
            SpinnerNumberModel(camera.accessRotationSpeed(), 0.0001, 0.01, 0.0001)
        )

        fovSpinner = createStyledSpinner(
            SpinnerNumberModel(renderer.getFov(), PI / 6, PI / 2, 0.01)
        )

        // Create checkbox
        invertYAxisCheckbox = createStyledCheckBox("Invert Y-Axis", false)

        fovSlider = createStyledSlider(
            (renderer.getFov() * 180 / PI).toInt(), 30, 90
        ).apply {
            // Listener referencing fovSpinner is okay here, as fovSpinner exists
            addChangeListener {
                if (!isInitializing) {
                    val fovValue = value * PI / 180
                    renderer.setFov(fovValue)
                    isInitializing = true
                    fovSpinner.value = fovValue // fovSpinner is initialized
                    isInitializing = false
                    renderer.repaint()
                }
            }
        }

        sensitivitySlider = createStyledSlider(
            (camera.accessRotationSpeed() * 10000).toInt(), 1, 100
        ).apply {
            // Listener referencing rotationSpeedSpinner is okay here
            addChangeListener {
                if (!isInitializing) {
                    val speedValue = value / 10000.0
                    camera.changeRotationSpeed(speedValue)
                    isInitializing = true
                    rotationSpeedSpinner.value = speedValue // rotationSpeedSpinner is initialized
                    isInitializing = false
                }
            }
        }

        rotationSpeedSpinner.addChangeListener(createNumericChangeListener { value ->
            camera.changeRotationSpeed(value)
            // Update the slider to match - NOW SAFE TO ACCESS sensitivitySlider
            isInitializing = true
            sensitivitySlider.value = (value * 10000).toInt()
            isInitializing = false
        })

        fovSpinner.addChangeListener(createNumericChangeListener { value ->
            renderer.setFov(value)
            // Update the slider to match - NOW SAFE TO ACCESS fovSlider
            isInitializing = true
            fovSlider.value = (value * 180 / PI).toInt()
            isInitializing = false
            renderer.repaint()
        })

        setupPanel()
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

    // Create styled JSlider
    private fun createStyledSlider(value: Int, min: Int, max: Int): JSlider {
        return JSlider(JSlider.HORIZONTAL, min, max, value).apply {
            background = DARKER_BG_COLOR
            foreground = TEXT_COLOR
            paintTicks = true
            paintLabels = true
            majorTickSpacing = (max - min) / 4
            minorTickSpacing = (max - min) / 20

            // Style the labels and ticks
            setUI(javax.swing.plaf.basic.BasicSliderUI(this))

            // Create labeled hashmarks
            val labelTable = Hashtable<Int, JLabel>()
            for (i in min..max step majorTickSpacing) {
                labelTable[i] = JLabel(i.toString()).apply {
                    font = Font("Arial", Font.PLAIN, 10)
                    foreground = SECONDARY_TEXT_COLOR
                }
            }
            labelTable[max] = JLabel(max.toString()).apply {
                font = Font("Arial", Font.PLAIN, 10)
                foreground = SECONDARY_TEXT_COLOR
            }
            setLabelTable(labelTable)
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

    // Helper function to create a styled setting row with a slider
    private fun createSliderRow(labelText: String, slider: JSlider): JPanel {
        return JPanel().apply {
            layout = GridBagLayout()
            background = DARKER_BG_COLOR
            border = EmptyBorder(5, 10, 5, 10)

            val label = createStyledLabel(labelText)

            // GridBag constraints for the label
            val labelConstraints = GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.WEST
                insets = Insets(0, 0, 0, 0)
            }

            // GridBag constraints for the slider - full width on new row
            val sliderConstraints = GridBagConstraints().apply {
                gridx = 0
                gridy = 1
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.CENTER
                insets = Insets(5, 0, 0, 0)
            }

            add(label, labelConstraints)
            add(slider, sliderConstraints)

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

    // Create a stylized separator similar to PlayerSettingsPanel
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
        // Use a gradient background panel like in PlayerSettingsPanel
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
            add(JLabel("MOUSE CONTROLS", SwingConstants.CENTER).apply {
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

        // Mouse Sensitivity Section
        val sensitivityPanel = JPanel().apply {
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

            add(JLabel("Mouse Sensitivity").apply {
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
            add(createSettingRow("Mouse Speed:", rotationSpeedSpinner), rowGbc)

            rowGbc.gridy = 2
            add(createSliderRow("Sensitivity:", sensitivitySlider), rowGbc)

            rowGbc.gridy = 3
            add(JPanel().apply {
                layout = BorderLayout()
                background = DARKER_BG_COLOR
                border = EmptyBorder(5, 10, 5, 10)
                add(invertYAxisCheckbox, BorderLayout.WEST)
            }, rowGbc)
        }

        gbc.gridy = 0
        settingsPanel.add(sensitivityPanel, gbc)

        // Field of View Section
        val fovPanel = JPanel().apply {
            layout = GridBagLayout()
            isOpaque = false
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)

            val labelGbc = GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.WEST
                insets = Insets(5, 5, 5, 0)
            }

            add(JLabel("Field of View").apply {
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

            rowGbc.gridy = 1
            add(createSettingRow("FOV (radians):", fovSpinner), rowGbc)

            rowGbc.gridy = 2
            add(createSliderRow("FOV (degrees):", fovSlider), rowGbc)

            // Add FOV preview visualization
            rowGbc.gridy = 3
            add(createFovPreview(), rowGbc)
        }

        gbc.gridy = 1
        settingsPanel.add(fovPanel, gbc)

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
                    // Reset values to defaults
                    camera.changeRotationSpeed(DEFAULT_ROTATION_SPEED)
                    renderer.setFov(DEFAULT_FOV)
                    invertYAxisCheckbox.isSelected = false

                    // Update UI to reflect these changes
                    updateAllSettingsDisplay()
                }
            })
        }, BorderLayout.SOUTH)
    }

    // Create a preview for the FOV setting
    private fun createFovPreview(): JPanel {
        return JPanel().apply {
            layout = BorderLayout()
            background = DARKER_BG_COLOR
            border = EmptyBorder(5, 5, 5, 5)
            preferredSize = Dimension(0, 100)

            add(object : JPanel() {
                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    val g2d = g as Graphics2D
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                    // Draw FOV visualization
                    val width = this.width
                    val height = this.height
                    val midX = width / 2
                    val midY = height

                    // Background
                    g2d.color = Color(20, 20, 20)
                    g2d.fillRect(0, 0, width, height)

                    // Get current FOV value
                    val fovVal = renderer.getFov()

                    // Calculate visual representation (half of FOV on each side)
                    val halfFovAngle = fovVal / 2
                    val radius = height * 0.9

                    // Calculate endpoints
                    val leftX = midX - radius * kotlin.math.sin(halfFovAngle)
                    val leftY = midY - radius * kotlin.math.cos(halfFovAngle)
                    val rightX = midX + radius * kotlin.math.sin(halfFovAngle)
                    val rightY = midY - radius * kotlin.math.cos(halfFovAngle)

                    // Draw FOV lines
                    g2d.color = ACCENT_COLOR
                    g2d.stroke = BasicStroke(2f)
                    g2d.drawLine(midX, midY, leftX.toInt(), leftY.toInt())
                    g2d.drawLine(midX, midY, rightX.toInt(), rightY.toInt())

                    // Draw FOV arc
                    g2d.color = Color(ACCENT_COLOR.red, ACCENT_COLOR.green, ACCENT_COLOR.blue, 40)
                    val startAngle = Math.toDegrees(Math.PI - halfFovAngle).toInt()
                    val arcAngle = Math.toDegrees(fovVal).toInt()
                    g2d.fillArc(
                        midX - radius.toInt(),
                        midY - radius.toInt(),
                        (radius * 2).toInt(),
                        (radius * 2).toInt(),
                        startAngle,
                        arcAngle
                    )

                    // Draw FOV value text
                    g2d.color = TEXT_COLOR
                    g2d.font = Font("Arial", Font.BOLD, 12)
                    val fovText = "FOV: ${Math.toDegrees(fovVal).toInt()}°"
                    val textWidth = g2d.fontMetrics.stringWidth(fovText)
                    g2d.drawString(fovText, midX - textWidth / 2, midY - 10)
                }
            }, BorderLayout.CENTER)
        }
    }

    fun updateAllSettingsDisplay() {
        isInitializing = true

        rotationSpeedSpinner.value = camera.accessRotationSpeed()
        fovSpinner.value = renderer.getFov()
        sensitivitySlider.value = (camera.accessRotationSpeed() * 10000).toInt()
        fovSlider.value = (renderer.getFov() * 180 / PI).toInt()

        isInitializing = false
        repaint()
    }
}