package ui.builder

import player.uis.*
import player.uis.components.*
import player.uis.components.TextComponent
import java.awt.*
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

class UIPropertiesPanel : JPanel() {
    private var currentComponent: UIComponent? = null
    private var changeListener: (() -> Unit)? = null

    // Color scheme matching the About dialog and UIBuilder
    companion object {
        val BACKGROUND_COLOR_DARK = Color(30, 33, 40)
        val BACKGROUND_COLOR_LIGHT = Color(45, 48, 55)
        val ACCENT_COLOR = Color(220, 95, 60) // Warm orange/red
        val TEXT_COLOR = Color(200, 200, 200)
        val TEXT_COLOR_DIMMED = Color(180, 180, 180)
        val BORDER_COLOR = Color(25, 28, 35)
        val BUTTON_BG = Color(60, 63, 65)
        val BUTTON_BORDER = Color(80, 83, 85)
        val FIELD_BG = Color(35, 38, 45)
    }

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = EmptyBorder(10, 10, 10, 10)
        background = BACKGROUND_COLOR_DARK

        add(createStyledLabel("Select a component to edit its properties").apply {
            alignmentX = Component.LEFT_ALIGNMENT
            foreground = TEXT_COLOR
            font = Font("Arial", Font.PLAIN, 14)
        })
    }

    fun setComponent(component: UIComponent?) {
        currentComponent = component

        // Clear existing controls
        removeAll()

        if (component == null) {
            add(createStyledLabel("Select a component to edit its properties").apply {
                alignmentX = Component.LEFT_ALIGNMENT
                foreground = TEXT_COLOR
                font = Font("Arial", Font.PLAIN, 14)
            })
        } else {
            // Create property editors for the component
            add(createHeader(component.javaClass.simpleName))

            // Position properties
            add(createHeader("Position"))
            add(createNumberField("X", component.x) { value ->
                component.x = value
                changeListener?.invoke()
            })
            add(createNumberField("Y", component.y) { value ->
                component.y = value
                changeListener?.invoke()
            })

            // Size properties
            add(createHeader("Size"))
            add(createNumberField("Width", component.width) { value ->
                component.width = value
                changeListener?.invoke()
            })
            add(createNumberField("Height", component.height) { value ->
                component.height = value
                changeListener?.invoke()
            })

            // Visibility
            add(createHeader("Visibility"))
            add(createCheckBox("Visible", component.visible) { value ->
                component.visible = value
                changeListener?.invoke()
            })

            // Component-specific properties
            when (component) {
                is HealthBarComponent -> addHealthBarProperties(component)
                is AmmoBarComponent -> addAmmoBarProperties(component)
                is FaceComponent -> addFaceProperties(component)
                is WeaponSelectorComponent -> addWeaponSelectorProperties(component)
                is BackgroundComponent -> addBackgroundProperties(component)
                is TextComponent -> addTextProperties(component)
                is ImageComponent -> addImageProperties(component)
                is ProgressBarComponent -> addProgressBarProperties(component)
                is StatComponent -> addStatProperties(component)
            }

            // Delete button
            add(Box.createVerticalStrut(20))
            add(createDeleteButton(component))
        }

        revalidate()
        repaint()
    }

    private fun createDeleteButton(component: UIComponent): JButton {
        return JButton("Delete Component").apply {
            alignmentX = Component.LEFT_ALIGNMENT
            foreground = Color.WHITE
            background = Color(160, 50, 50) // Reddish background for delete button
            font = Font("Arial", Font.BOLD, 12)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(180, 70, 70)),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
            )
            isFocusPainted = false

            // Hover effect
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    background = Color(180, 70, 70)
                }

                override fun mouseExited(e: MouseEvent) {
                    background = Color(160, 50, 50)
                }
            })

            addActionListener {
                if (JOptionPane.showConfirmDialog(
                        this@UIPropertiesPanel,
                        "Delete this component?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION
                    ) == JOptionPane.YES_OPTION
                ) {
                    // Find the UIBuilder instance
                    val parentUI = SwingUtilities.getAncestorOfClass(
                        UIBuilder::class.java,
                        this@UIPropertiesPanel
                    ) as? UIBuilder

                    if (parentUI != null) {
                        // Remove component
                        parentUI.customizableGameUI.removeComponent(component)
                        setComponent(null) // Clear the properties panel
                        parentUI.previewPanel.repaint() // Update the preview
                    } else {
                        println("Error: Could not find the parent UIBuilder component.")
                    }
                }
            }
        }
    }

    private fun addHealthBarProperties(component: HealthBarComponent) {
        // Color properties
        add(createHeader("Colors"))
        add(createColorField("Health Color", component.healthColor) { color ->
            component.healthColor = color
            changeListener?.invoke()
        })
        add(createColorField("Background", component.backgroundColor) { color ->
            component.backgroundColor = color
            changeListener?.invoke()
        })
        add(createColorField("Border", component.borderColor) { color ->
            component.borderColor = color
            changeListener?.invoke()
        })
        add(createColorField("Text", component.textColor) { color ->
            component.textColor = color
            changeListener?.invoke()
        })
    }

    private fun addAmmoBarProperties(component: AmmoBarComponent) {
        // Color properties
        add(createHeader("Colors"))
        add(createColorField("Ammo Color", component.ammoColor) { color ->
            component.ammoColor = color
            changeListener?.invoke()
        })
        add(createColorField("Background", component.backgroundColor) { color ->
            component.backgroundColor = color
            changeListener?.invoke()
        })
        add(createColorField("Border", component.borderColor) { color ->
            component.borderColor = color
            changeListener?.invoke()
        })
        add(createColorField("Text", component.textColor) { color ->
            component.textColor = color
            changeListener?.invoke()
        })
    }

    private fun addFaceProperties(component: FaceComponent) {
        // Color properties
        add(createHeader("Colors"))
        add(createColorField("Background", component.backgroundColor) { color ->
            component.backgroundColor = color
            changeListener?.invoke()
        })
        add(createColorField("Border", component.borderColor) { color ->
            component.borderColor = color
            changeListener?.invoke()
        })
        add(createColorField("Text", component.textColor) { color ->
            component.textColor = color
            changeListener?.invoke()
        })
    }

    private fun addWeaponSelectorProperties(component: WeaponSelectorComponent) {
        // Color properties
        add(createHeader("Colors"))
        add(createColorField("Background", component.backgroundColor) { color ->
            component.backgroundColor = color
            changeListener?.invoke()
        })
        add(createColorField("Border", component.borderColor) { color ->
            component.borderColor = color
            changeListener?.invoke()
        })
        add(createColorField("Text", component.textColor) { color ->
            component.textColor = color
            changeListener?.invoke()
        })

        // Current weapon
        add(createHeader("Weapon"))
        add(createNumberField("Current Weapon", component.currentWeapon) { value ->
            component.currentWeapon = value.coerceIn(1, 5)
            changeListener?.invoke()
        })
    }

    private fun addBackgroundProperties(component: BackgroundComponent) {
        // Color properties
        add(createHeader("Colors"))
        add(createColorField("Background", component.backgroundColor) { color ->
            component.backgroundColor = color
            changeListener?.invoke()
        })
        add(createColorField("Border", component.borderColor) { color ->
            component.borderColor = color
            changeListener?.invoke()
        })

        // Border properties
        add(createHeader("Border"))
        add(createNumberField("Thickness", component.borderThickness) { value ->
            component.borderThickness = value
            changeListener?.invoke()
        })
        add(createNumberField("Corner Radius", component.cornerRadius) { value ->
            component.cornerRadius = value
            changeListener?.invoke()
        })
    }

    private fun addTextProperties(component: TextComponent) {
        // Text properties
        add(createHeader("Text"))
        add(createTextField("Content", component.text) { value ->
            component.text = value
            changeListener?.invoke()
        })

        // Font properties
        add(createHeader("Font"))
        add(createFontSizeField("Size", component.fontSize) { value ->
            component.fontSize = value
            changeListener?.invoke()
        })

        // Add font style dropdown
        val fontStylePanel = createStyledPanel()
        fontStylePanel.layout = BoxLayout(fontStylePanel, BoxLayout.X_AXIS)
        fontStylePanel.alignmentX = Component.LEFT_ALIGNMENT

        fontStylePanel.add(createStyledLabel("Style: ").apply {
            preferredSize = Dimension(80, preferredSize.height)
        })

        val fontStyles = arrayOf("Plain", "Bold", "Italic", "Bold+Italic")
        val styleBox = JComboBox(fontStyles).apply {
            background = FIELD_BG
            foreground = TEXT_COLOR
            border = BorderFactory.createLineBorder(BORDER_COLOR)
        }

        when (component.fontStyle) {
            Font.PLAIN -> styleBox.selectedIndex = 0
            Font.BOLD -> styleBox.selectedIndex = 1
            Font.ITALIC -> styleBox.selectedIndex = 2
            Font.BOLD + Font.ITALIC -> styleBox.selectedIndex = 3
        }

        styleBox.addActionListener {
            component.fontStyle = when (styleBox.selectedIndex) {
                0 -> Font.PLAIN
                1 -> Font.BOLD
                2 -> Font.ITALIC
                3 -> Font.BOLD + Font.ITALIC
                else -> Font.PLAIN
            }
            changeListener?.invoke()
        }

        fontStylePanel.add(styleBox)
        add(fontStylePanel)

        // Color properties
        add(createHeader("Color"))
        add(createColorField("Text Color", component.textColor) { color ->
            component.textColor = color
            changeListener?.invoke()
        })
    }

    private fun addImageProperties(component: ImageComponent) {
        // Image type
        add(createHeader("Image Type"))

        val imageTypePanel = createStyledPanel()
        imageTypePanel.layout = BoxLayout(imageTypePanel, BoxLayout.X_AXIS)
        imageTypePanel.alignmentX = Component.LEFT_ALIGNMENT

        imageTypePanel.add(createStyledLabel("Type: ").apply {
            preferredSize = Dimension(80, preferredSize.height)
        })

        val imageTypes = arrayOf("face", "weapon", "key", "ammo", "custom")
        val typeBox = JComboBox(imageTypes).apply {
            background = FIELD_BG
            foreground = TEXT_COLOR
            border = BorderFactory.createLineBorder(BORDER_COLOR)
        }
        typeBox.selectedItem = component.imageType

        typeBox.addActionListener {
            val newType = typeBox.selectedItem as String
            component.imageType = newType

            // If selecting custom, prompt for image file
            if (newType == "custom") {
                val fileChooser = JFileChooser()
                fileChooser.dialogTitle = "Select Image"
                fileChooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                    "Image files", "jpg", "jpeg", "png", "gif", "bmp"
                )

                if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    component.imagePath = fileChooser.selectedFile.absolutePath
                    component.updateImage()
                    changeListener?.invoke()
                }
            } else {
                component.updateImage()
                changeListener?.invoke()
            }
        }

        imageTypePanel.add(typeBox)
        add(imageTypePanel)

        // Custom image section
        if (component.imageType == "custom") {
            add(createHeader("Custom Image"))

            // Show current image path
            val pathPanel = createStyledPanel()
            pathPanel.layout = BoxLayout(pathPanel, BoxLayout.X_AXIS)
            pathPanel.alignmentX = Component.LEFT_ALIGNMENT
            pathPanel.border = EmptyBorder(3, 0, 3, 0)

            val pathLabel = createStyledLabel("Path: ")
            pathLabel.preferredSize = Dimension(80, pathLabel.preferredSize.height)

            val pathField = JTextField(component.imagePath).apply {
                isEditable = false
                background = FIELD_BG
                foreground = TEXT_COLOR
                border = BorderFactory.createLineBorder(BORDER_COLOR)
            }

            val browseButton = JButton("Browse...").apply {
                foreground = TEXT_COLOR
                background = BUTTON_BG
                font = Font("Arial", Font.BOLD, 12)
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BUTTON_BORDER),
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)
                )
                isFocusPainted = false

                // Hover effect
                addMouseListener(object : MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent) {
                        background = BUTTON_BORDER
                    }

                    override fun mouseExited(e: MouseEvent) {
                        background = BUTTON_BG
                    }
                })
            }

            browseButton.addActionListener {
                val fileChooser = JFileChooser()
                fileChooser.dialogTitle = "Select Image"
                fileChooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                    "Image files", "jpg", "jpeg", "png", "gif", "bmp"
                )

                if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    component.imagePath = fileChooser.selectedFile.absolutePath
                    pathField.text = component.imagePath
                    component.updateImage()
                    changeListener?.invoke()
                }
            }

            pathPanel.add(pathLabel)
            pathPanel.add(pathField)
            pathPanel.add(browseButton)
            add(pathPanel)
        }

        // Sizing options
        add(createHeader("Size Options"))

        // Aspect ratio toggle
        add(createCheckBox("Preserve Aspect Ratio", component.preserveAspectRatio) { value ->
            component.preserveAspectRatio = value
            changeListener?.invoke()
        })

        // Scale (only visible when preserving aspect ratio)
        if (component.preserveAspectRatio) {
            add(createSlider("Scale", (component.scale * 100).toInt(), 10, 200) { value ->
                component.scale = value / 100.0
                changeListener?.invoke()
            })
        } else {
            // Width and height fields (only visible when not preserving aspect)
            add(createNumberField("Width", component.width) { value ->
                component.width = value
                changeListener?.invoke()
            })
            add(createNumberField("Height", component.height) { value ->
                component.height = value
                changeListener?.invoke()
            })
        }
    }

    private fun addProgressBarProperties(component: ProgressBarComponent) {
        // Color properties
        add(createHeader("Colors"))
        add(createColorField("Bar Color", component.barColor) { color ->
            component.barColor = color
            changeListener?.invoke()
        })
        add(createColorField("Background", component.backgroundColor) { color ->
            component.backgroundColor = color
            changeListener?.invoke()
        })
        add(createColorField("Border", component.borderColor) { color ->
            component.borderColor = color
            changeListener?.invoke()
        })

        // Progress properties
        add(createHeader("Progress"))
        add(createSlider("Fill %", component.fillPercentage, 0, 100) { value ->
            component.fillPercentage = value
            changeListener?.invoke()
        })

        // Notch properties
        add(createHeader("Notches"))
        add(createCheckBox("Show Notches", component.showNotches) { value ->
            component.showNotches = value
            changeListener?.invoke()
        })
        add(createNumberField("Notch Count", component.notchCount) { value ->
            component.notchCount = value.coerceIn(0, 20)
            changeListener?.invoke()
        })
    }

    private fun createTextField(label: String, initialValue: String, onValueChanged: (String) -> Unit): JPanel {
        val panel = createStyledPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.alignmentX = Component.LEFT_ALIGNMENT
        panel.border = EmptyBorder(3, 0, 3, 0)

        panel.add(createStyledLabel("$label: ").apply {
            preferredSize = Dimension(80, preferredSize.height)
        })

        val textField = JTextField(initialValue, 10).apply {
            background = FIELD_BG
            foreground = TEXT_COLOR
            caretColor = TEXT_COLOR
            border = BorderFactory.createLineBorder(BORDER_COLOR)
            font = Font("SansSerif", Font.PLAIN, 12)
        }

        textField.addActionListener {
            onValueChanged(textField.text)
        }
        textField.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                onValueChanged(textField.text)
            }
        })

        panel.add(textField)
        return panel
    }

    private fun createFontSizeField(label: String, initialValue: Int, onValueChanged: (Int) -> Unit): JPanel {
        val panel = createStyledPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.alignmentX = Component.LEFT_ALIGNMENT
        panel.border = EmptyBorder(3, 0, 3, 0)

        panel.add(createStyledLabel("$label: ").apply {
            preferredSize = Dimension(80, preferredSize.height)
        })

        val spinner = JSpinner(SpinnerNumberModel(initialValue, 8, 72, 1)).apply {
            preferredSize = Dimension(70, preferredSize.height)
            background = FIELD_BG
            foreground = TEXT_COLOR
            border = BorderFactory.createLineBorder(BORDER_COLOR)

            // Style the spinner's editor
            (getEditor() as JSpinner.DefaultEditor).textField.apply {
                background = FIELD_BG
                foreground = TEXT_COLOR
                caretColor = TEXT_COLOR
            }
        }

        spinner.addChangeListener {
            onValueChanged(spinner.value as Int)
        }

        panel.add(spinner)
        return panel
    }

    private fun createSlider(label: String, initialValue: Int, min: Int, max: Int, onValueChanged: (Int) -> Unit): JPanel {
        val panel = createStyledPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.alignmentX = Component.LEFT_ALIGNMENT
        panel.border = EmptyBorder(3, 0, 3, 0)

        panel.add(createStyledLabel("$label: ").apply {
            preferredSize = Dimension(80, preferredSize.height)
        })

        val slider = JSlider(JSlider.HORIZONTAL, min, max, initialValue).apply {
            preferredSize = Dimension(100, preferredSize.height)
            background = BACKGROUND_COLOR_DARK
            foreground = TEXT_COLOR

            // Customize slider colors
            UIManager.put("Slider.thumb", ACCENT_COLOR)
            UIManager.put("Slider.track", BACKGROUND_COLOR_LIGHT)
            UIManager.put("Slider.tickColor", TEXT_COLOR_DIMMED)
        }

        val valueLabel = createStyledLabel("$initialValue").apply {
            preferredSize = Dimension(40, preferredSize.height)
            horizontalAlignment = SwingConstants.CENTER
        }

        slider.addChangeListener {
            val value = slider.value
            valueLabel.text = "$value"
            onValueChanged(value)
        }

        panel.add(slider)
        panel.add(valueLabel)
        return panel
    }

    private fun addStatProperties(component: StatComponent) {
        // Stat type
        add(createHeader("Stat Type"))

        val statTypePanel = createStyledPanel()
        statTypePanel.layout = BoxLayout(statTypePanel, BoxLayout.X_AXIS)
        statTypePanel.alignmentX = Component.LEFT_ALIGNMENT

        statTypePanel.add(createStyledLabel("Type: ").apply {
            preferredSize = Dimension(80, preferredSize.height)
        })

        val statTypes = arrayOf("kills", "items", "secrets", "armor", "custom")
        val typeBox = JComboBox(statTypes).apply {
            background = FIELD_BG
            foreground = TEXT_COLOR
            border = BorderFactory.createLineBorder(BORDER_COLOR)
        }
        typeBox.selectedItem = component.statType

        typeBox.addActionListener {
            component.statType = typeBox.selectedItem as String
            changeListener?.invoke()
        }

        statTypePanel.add(typeBox)
        add(statTypePanel)

        // Value properties
        add(createHeader("Values"))
        add(createNumberField("Current", component.currentValue) { value ->
            component.currentValue = value
            changeListener?.invoke()
        })
        add(createNumberField("Maximum", component.maxValue) { value ->
            component.maxValue = value
            changeListener?.invoke()
        })

        // Display options
        add(createHeader("Display"))
        add(createColorField("Text Color", component.textColor) { color ->
            component.textColor = color
            changeListener?.invoke()
        })
        add(createCheckBox("Show as Percentage", component.showAsPercentage) { value ->
            component.showAsPercentage = value
            changeListener?.invoke()
        })
    }

    private fun createHeader(text: String): JPanel {
        val headerPanel = createStyledPanel()
        headerPanel.layout = BoxLayout(headerPanel, BoxLayout.Y_AXIS)
        headerPanel.alignmentX = Component.LEFT_ALIGNMENT
        headerPanel.border = EmptyBorder(10, 0, 5, 0)

        // Add text label
        headerPanel.add(JLabel(text).apply {
            foreground = ACCENT_COLOR
            font = Font("Arial", Font.BOLD, 14)
            alignmentX = Component.LEFT_ALIGNMENT
            border = EmptyBorder(0, 0, 3, 0)
        })

        // Add separator
        headerPanel.add(object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                // Draw glowing line
                val width = this.width
                val y = this.height / 2

                val gradient = LinearGradientPaint(
                    0f, y.toFloat(), width.toFloat(), y.toFloat(),
                    floatArrayOf(0.0f, 0.5f, 1.0f),
                    arrayOf(BACKGROUND_COLOR_LIGHT, ACCENT_COLOR, BACKGROUND_COLOR_LIGHT)
                )

                g2d.stroke = BasicStroke(1f)
                g2d.paint = gradient
                g2d.drawLine(0, y, width, y)
            }

            init {
                background = BACKGROUND_COLOR_DARK
                preferredSize = Dimension(1, 6)
                alignmentX = Component.LEFT_ALIGNMENT
                isOpaque = false
            }
        })

        return headerPanel
    }

    private fun createNumberField(label: String, initialValue: Int, onValueChanged: (Int) -> Unit): JPanel {
        val panel = createStyledPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.alignmentX = Component.LEFT_ALIGNMENT
        panel.border = EmptyBorder(3, 0, 3, 0)

        panel.add(createStyledLabel("$label: ").apply {
            preferredSize = Dimension(80, preferredSize.height)
        })

        val spinner = JSpinner(SpinnerNumberModel(initialValue, 0, 1000, 1)).apply {
            preferredSize = Dimension(70, preferredSize.height)
            background = FIELD_BG
            foreground = TEXT_COLOR
            border = BorderFactory.createLineBorder(BORDER_COLOR)

            // Style the spinner's editor
            (getEditor() as JSpinner.DefaultEditor).textField.apply {
                background = FIELD_BG
                foreground = TEXT_COLOR
                caretColor = TEXT_COLOR
            }
        }

        spinner.addChangeListener {
            onValueChanged(spinner.value as Int)
        }

        panel.add(spinner)
        return panel
    }

    private fun createColorField(label: String, initialColor: Color, onColorChanged: (Color) -> Unit): JPanel {
        val panel = createStyledPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.alignmentX = Component.LEFT_ALIGNMENT
        panel.border = EmptyBorder(3, 0, 3, 0)

        panel.add(createStyledLabel("$label: ").apply {
            preferredSize = Dimension(80, preferredSize.height)
        })

        val colorButton = JButton().apply {
            background = initialColor
            preferredSize = Dimension(70, 20)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
            )
            isFocusPainted = false
        }

        colorButton.addActionListener {
            val newColor = JColorChooser.showDialog(this, "Choose $label Color", colorButton.background)
            if (newColor != null) {
                colorButton.background = newColor
                onColorChanged(newColor)
            }
        }

        panel.add(colorButton)
        return panel
    }

    private fun createCheckBox(label: String, initialValue: Boolean, onValueChanged: (Boolean) -> Unit): JPanel {
        val panel = createStyledPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.alignmentX = Component.LEFT_ALIGNMENT
        panel.border = EmptyBorder(3, 0, 3, 0)

        val checkbox = JCheckBox(label, initialValue).apply {
            foreground = TEXT_COLOR
            background = BACKGROUND_COLOR_DARK
            font = Font("Arial", Font.PLAIN, 12)
            isFocusPainted = false
            icon = createCheckBoxIcon(false)
            selectedIcon = createCheckBoxIcon(true)
        }

        checkbox.addActionListener {
            onValueChanged(checkbox.isSelected)
        }

        panel.add(checkbox)
        return panel
    }

    private fun createCheckBoxIcon(selected: Boolean): Icon {
        return object : Icon {
            override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
                val g2d = g.create() as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                // Draw checkbox
                if (selected) {
                    g2d.color = ACCENT_COLOR
                    g2d.fillRect(x, y, iconWidth, iconHeight)
                    g2d.color = Color.WHITE
                    g2d.drawLine(x + 3, y + iconHeight/2, x + iconWidth/2 - 1, y + iconHeight - 3)
                    g2d.drawLine(x + iconWidth/2 - 1, y + iconHeight - 3, x + iconWidth - 3, y + 3)
                } else {
                    g2d.color = BACKGROUND_COLOR_LIGHT
                    g2d.fillRect(x, y, iconWidth, iconHeight)
                }

                // Draw border
                g2d.color = BORDER_COLOR
                g2d.drawRect(x, y, iconWidth - 1, iconHeight - 1)

                g2d.dispose()
            }

            override fun getIconWidth(): Int = 14
            override fun getIconHeight(): Int = 14
        }
    }

    // Helper methods for consistent component styling
    private fun createStyledLabel(text: String): JLabel {
        return JLabel(text).apply {
            foreground = TEXT_COLOR
            font = Font("Arial", Font.PLAIN, 12)
        }
    }

    private fun createStyledPanel(): JPanel {
        return JPanel().apply {
            background = BACKGROUND_COLOR_DARK
            isOpaque = true
        }
    }

    fun setChangeListener(listener: () -> Unit) {
        changeListener = listener
    }
}