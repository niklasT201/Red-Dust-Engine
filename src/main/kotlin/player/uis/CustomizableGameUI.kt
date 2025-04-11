package player.uis

import java.awt.Graphics2D
import java.io.*

class CustomizableGameUI {
    // List of all UI components
    private val components = mutableListOf<UIComponent>()

    // Add a component
    fun addComponent(component: UIComponent) {
        components.add(component)
    }

    // Remove a component
    fun removeComponent(component: UIComponent) {
        components.remove(component)
    }

    // Get all components
    fun getComponents(): List<UIComponent> {
        return components
    }

    // Render all components
    fun render(g2: Graphics2D, width: Int, height: Int) {
        for (component in components) {
            component.render(g2, width, height)
        }
    }

    // Find component at position
    fun getComponentAt(x: Int, y: Int): UIComponent? {
        // Check in reverse order to get the topmost component
        for (i in components.size - 1 downTo 0) {
            if (components[i].contains(x, y)) {
                return components[i]
            }
        }
        return null
    }

    // Save UI layout to file
    fun saveToFile(file: File) {
        try {
            ObjectOutputStream(FileOutputStream(file)).use { out ->
                out.writeObject(components)
            }
        } catch (e: Exception) {
            println("Error saving UI layout: ${e.message}")
        }
    }

    // Load UI layout from file
    @Suppress("UNCHECKED_CAST")
    fun loadFromFile(file: File) {
        try {
            ObjectInputStream(FileInputStream(file)).use { input ->
                components.clear()
                components.addAll(input.readObject() as List<UIComponent>)
            }
        } catch (e: Exception) {
            println("Error loading UI layout: ${e.message}")
        }
    }

    // Create default UI layout (similar to existing GameUI)
    fun createDefaultLayout(screenWidth: Int, screenHeight: Int) {
        components.clear()

        // Calculate positions
        val statusBarHeight = 120
        val panelY = screenHeight - statusBarHeight + 10
        val panelWidth = 210
        val panelHeight = statusBarHeight - 20

        // Health bar (left side)
        val healthBar = HealthBarComponent(15, panelY, panelWidth, panelHeight)
        healthBar.id = "health"
        addComponent(healthBar)

        // Face component (center)
        val facePanelWidth = 170
        val facePanelX = screenWidth / 2 - facePanelWidth / 2
        val faceComponent = FaceComponent(facePanelX, panelY, facePanelWidth, panelHeight)
        faceComponent.id = "face"
        addComponent(faceComponent)

        // Ammo bar (right side)
        val ammoBar = AmmoBarComponent(screenWidth - panelWidth - 15, panelY, panelWidth, panelHeight)
        ammoBar.id = "ammo"
        addComponent(ammoBar)

        // Weapon selector
        val weaponSelector = WeaponSelectorComponent(screenWidth - 80, panelY)
        weaponSelector.id = "weapon"
        addComponent(weaponSelector)
    }
}