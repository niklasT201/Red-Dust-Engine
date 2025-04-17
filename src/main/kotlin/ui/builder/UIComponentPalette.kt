package ui.builder

import player.uis.*
import player.uis.components.*
import player.uis.components.TextComponent
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class UIComponentPalette(
    private val customUI: CustomizableGameUI,
    private val previewPanel: UIPreviewPanel
) : JPanel() {
    private var selectionListener: ((UIComponent?) -> Unit)? = null

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = EmptyBorder(10, 10, 10, 10)

        add(createHeader("Basic Elements"))
        add(createComponentButton("Background Panel", BackgroundComponent(20, 20, 200, 100)))
        add(createComponentButton("Text Label", TextComponent(20, 20, 100, 20)))
        add(createComponentButton("Face Image", ImageComponent(20, 20, 64, 64)))
        add(createComponentButton("Progress Bar", ProgressBarComponent(20, 20, 180, 16)))
        add(createComponentButton("Stat Display", StatComponent(20, 20, 100, 20)))

        add(createHeader("Game UI Elements"))
        add(createComponentButton("Health Bar", HealthBarComponent(20, 20, 210, 100)))
        add(createComponentButton("Ammo Bar", AmmoBarComponent(20, 20, 210, 100)))
        add(createComponentButton("Face Panel", FaceComponent(20, 20, 170, 100)))
        add(createComponentButton("Weapon Selector", WeaponSelectorComponent(20, 20)))

        add(Box.createVerticalGlue())
    }

    private fun createHeader(text: String): JLabel {
        val label = JLabel(text)
        label.font = Font(label.font.name, Font.BOLD, 14)
        label.alignmentX = Component.LEFT_ALIGNMENT
        label.border = EmptyBorder(0, 0, 10, 0)
        return label
    }

    private fun createComponentButton(text: String, templateComponent: UIComponent): JButton {
        val button = JButton(text)
        button.alignmentX = Component.LEFT_ALIGNMENT
        button.maximumSize = Dimension(Integer.MAX_VALUE, button.preferredSize.height)

        button.addActionListener {
            // Create a new component from the template
            val newComponent = templateComponent.clone()
            newComponent.id = "${text.toLowerCase().replace(" ", "_")}_${System.currentTimeMillis()}"

            // Add it to the UI
            customUI.addComponent(newComponent)

            // Notify selection listener
            selectionListener?.invoke(newComponent)

            // Repaint preview
            previewPanel.repaint()
        }

        return button
    }

    fun setSelectionListener(listener: (UIComponent?) -> Unit) {
        this.selectionListener = listener
    }
}