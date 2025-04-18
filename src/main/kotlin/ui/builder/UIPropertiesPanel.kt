package ui.builder

import player.uis.*
import player.uis.components.*
import player.uis.components.TextComponent
import java.awt.*
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

class UIPropertiesPanel : JPanel() {
    private var currentComponent: UIComponent? = null
    private var changeListener: (() -> Unit)? = null

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = EmptyBorder(10, 10, 10, 10)

        add(JLabel("Select a component to edit its properties").apply {
            alignmentX = Component.LEFT_ALIGNMENT
        })
    }

    fun setComponent(component: UIComponent?) {
        currentComponent = component

        // Clear existing controls
        removeAll()

        if (component == null) {
            add(JLabel("Select a component to edit its properties").apply {
                alignmentX = Component.LEFT_ALIGNMENT
            })
        } else {
            // Create property editors for the component
            add(createHeader("Component Properties: ${component.javaClass.simpleName}"))

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
            add(JButton("Delete Component").apply {
                alignmentX = Component.LEFT_ALIGNMENT
                addActionListener {
                    if (JOptionPane.showConfirmDialog(
                            this@UIPropertiesPanel, // Context for the dialog
                            "Delete this component?",
                            "Confirm Delete",
                            JOptionPane.YES_NO_OPTION
                        ) == JOptionPane.YES_OPTION
                    ) {
                        // Find the UIBuilder instance - START SEARCHING FROM THIS PANEL
                        val parentUI = SwingUtilities.getAncestorOfClass(
                            UIBuilder::class.java, // The class to find
                            this@UIPropertiesPanel // The component to start searching from <--- FIX HERE
                        ) as? UIBuilder

                        if (parentUI != null) {
                            // Get the component to delete (make sure currentComponent isn't null)
                            val componentToDelete = currentComponent
                            if (componentToDelete != null) {
                                // Remove component
                                parentUI.customizableGameUI.removeComponent(componentToDelete)
                                setComponent(null) // Clear the properties panel
                                parentUI.previewPanel.repaint() // Update the preview
                            } else {
                                // Optional: Handle case where currentComponent is somehow null
                                println("Error: Tried to delete but no component was selected.")
                            }
                        } else {
                            // Optional: Handle case where UIBuilder ancestor wasn't found (shouldn't happen in this structure)
                            println("Error: Could not find the parent UIBuilder component.")
                        }
                    }
                }
            })
        }

        revalidate()
        repaint()
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
        val fontStylePanel = JPanel()
        fontStylePanel.layout = BoxLayout(fontStylePanel, BoxLayout.X_AXIS)
        fontStylePanel.alignmentX = Component.LEFT_ALIGNMENT

        fontStylePanel.add(JLabel("Style: ").apply {
            preferredSize = Dimension(80, preferredSize.height)
        })

        val fontStyles = arrayOf("Plain", "Bold", "Italic", "Bold+Italic")
        val styleBox = JComboBox(fontStyles)
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

        val imageTypePanel = JPanel()
        imageTypePanel.layout = BoxLayout(imageTypePanel, BoxLayout.X_AXIS)
        imageTypePanel.alignmentX = Component.LEFT_ALIGNMENT

        imageTypePanel.add(JLabel("Type: ").apply {
            preferredSize = Dimension(80, preferredSize.height)
        })

        val imageTypes = arrayOf("face", "weapon", "key", "ammo", "custom")
        val typeBox = JComboBox(imageTypes)
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
            val pathPanel = JPanel()
            pathPanel.layout = BoxLayout(pathPanel, BoxLayout.X_AXIS)
            pathPanel.alignmentX = Component.LEFT_ALIGNMENT
            pathPanel.border = EmptyBorder(3, 0, 3, 0)

            val pathLabel = JLabel("Path: ")
            pathLabel.preferredSize = Dimension(80, pathLabel.preferredSize.height)

            val pathField = JTextField(component.imagePath)
            pathField.isEditable = false

            val browseButton = JButton("Browse...")
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
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.alignmentX = Component.LEFT_ALIGNMENT
        panel.border = EmptyBorder(3, 0, 3, 0)

        panel.add(JLabel("$label: ").apply {
            preferredSize = Dimension(80, preferredSize.height)
        })

        val textField = JTextField(initialValue, 10)
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
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.alignmentX = Component.LEFT_ALIGNMENT
        panel.border = EmptyBorder(3, 0, 3, 0)

        panel.add(JLabel("$label: ").apply {
            preferredSize = Dimension(80, preferredSize.height)
        })

        val spinner = JSpinner(SpinnerNumberModel(initialValue, 8, 72, 1))
        spinner.preferredSize = Dimension(70, spinner.preferredSize.height)
        spinner.addChangeListener {
            onValueChanged(spinner.value as Int)
        }

        panel.add(spinner)
        return panel
    }

    private fun createSlider(label: String, initialValue: Int, min: Int, max: Int, onValueChanged: (Int) -> Unit): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.alignmentX = Component.LEFT_ALIGNMENT
        panel.border = EmptyBorder(3, 0, 3, 0)

        panel.add(JLabel("$label: ").apply {
            preferredSize = Dimension(80, preferredSize.height)
        })

        val slider = JSlider(JSlider.HORIZONTAL, min, max, initialValue)
        slider.preferredSize = Dimension(100, slider.preferredSize.height)

        val valueLabel = JLabel("$initialValue")
        valueLabel.preferredSize = Dimension(40, valueLabel.preferredSize.height)

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

        val statTypePanel = JPanel()
        statTypePanel.layout = BoxLayout(statTypePanel, BoxLayout.X_AXIS)
        statTypePanel.alignmentX = Component.LEFT_ALIGNMENT

        statTypePanel.add(JLabel("Type: ").apply {
            preferredSize = Dimension(80, preferredSize.height)
        })

        val statTypes = arrayOf("kills", "items", "secrets", "armor", "custom")
        val typeBox = JComboBox(statTypes)
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

    private fun createHeader(text: String): JLabel {
        val label = JLabel(text)
        label.font = Font(label.font.name, Font.BOLD, 14)
        label.alignmentX = Component.LEFT_ALIGNMENT
        label.border = EmptyBorder(5, 0, 5, 0)
        return label
    }

    private fun createNumberField(label: String, initialValue: Int, onValueChanged: (Int) -> Unit): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.alignmentX = Component.LEFT_ALIGNMENT
        panel.border = EmptyBorder(3, 0, 3, 0)

        panel.add(JLabel("$label: ").apply {
            preferredSize = Dimension(80, preferredSize.height)
        })

        val spinner = JSpinner(SpinnerNumberModel(initialValue, 0, 1000, 1))
        spinner.preferredSize = Dimension(70, spinner.preferredSize.height)
        spinner.addChangeListener {
            onValueChanged(spinner.value as Int)
        }

        panel.add(spinner)
        return panel
    }

    private fun createColorField(label: String, initialColor: Color, onColorChanged: (Color) -> Unit): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.alignmentX = Component.LEFT_ALIGNMENT
        panel.border = EmptyBorder(3, 0, 3, 0)

        panel.add(JLabel("$label: ").apply {
            preferredSize = Dimension(80, preferredSize.height)
        })

        val colorButton = JButton()
        colorButton.background = initialColor
        colorButton.preferredSize = Dimension(70, 20)
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
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.alignmentX = Component.LEFT_ALIGNMENT
        panel.border = EmptyBorder(3, 0, 3, 0)

        val checkbox = JCheckBox(label, initialValue)
        checkbox.addActionListener {
            onValueChanged(checkbox.isSelected)
        }

        panel.add(checkbox)
        return panel
    }

    fun setChangeListener(listener: () -> Unit) {
        changeListener = listener
    }
}