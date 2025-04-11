package player.uis

import java.awt.Graphics2D
import java.io.*

class CustomizableGameUI {
    // List of all UI components
    private val components = mutableListOf<UIComponent>()

    // Reference screen dimensions used when designing the UI
    var designWidth = 800
    var designHeight = 600

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

    // Update design dimensions
    fun setDesignDimensions(width: Int, height: Int) {
        designWidth = width
        designHeight = height
    }

    // Render all components with proper scaling
    fun render(g2: Graphics2D, width: Int, height: Int) {
        // Calculate scale factors
        val scaleX = width.toFloat() / designWidth
        val scaleY = height.toFloat() / designHeight

        // Save original transformation
        val originalTransform = g2.transform

        // Apply scaling
        g2.scale(scaleX.toDouble(), scaleY.toDouble())

        // Draw each component (they'll be drawn in design coordinates)
        for (component in components) {
            component.render(g2, designWidth, designHeight)
        }

        // Restore original transformation
        g2.transform = originalTransform
    }

    // Find component at position (accounting for scaling)
    fun getComponentAt(x: Int, y: Int, currentWidth: Int, currentHeight: Int): UIComponent? {
        // Calculate scale factors
        val scaleX = designWidth.toFloat() / currentWidth
        val scaleY = designHeight.toFloat() / currentHeight

        // Convert screen coordinates to design coordinates
        val designX = (x * scaleX).toInt()
        val designY = (y * scaleY).toInt()

        // Check in reverse order to get the topmost component
        for (i in components.size - 1 downTo 0) {
            if (components[i].contains(designX, designY)) {
                return components[i]
            }
        }
        return null
    }

    // Save UI layout to file
    fun saveToFile(file: File) {
        try {
            ObjectOutputStream(FileOutputStream(file)).use { out ->
                // Save design dimensions as well
                out.writeInt(designWidth)
                out.writeInt(designHeight)
                // Save components
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
                // Load design dimensions
                designWidth = input.readInt()
                designHeight = input.readInt()
                // Load components
                components.clear()
                components.addAll(input.readObject() as List<UIComponent>)
            }
        } catch (e: Exception) {
            println("Error loading UI layout: ${e.message}")
        }
    }

    // Create default UI layout
    fun createDefaultLayout(screenWidth: Int, screenHeight: Int) {
        components.clear()

        // Set design dimensions to current screen dimensions
        designWidth = screenWidth
        designHeight = screenHeight

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