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
        // Ensure the component is serializable before adding (optional but good practice)
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

    // Render all components with proper scaling
    fun render(g2: Graphics2D, width: Int, height: Int) {
        // Calculate scale factors
        val scaleX = width.toFloat() / designWidth.toFloat().coerceAtLeast(1f) // Avoid division by zero
        val scaleY = height.toFloat() / designHeight.toFloat().coerceAtLeast(1f) // Avoid division by zero

        // Save original transformation
        val originalTransform = g2.transform

        try { // Add try-finally to ensure transform is always restored
            // Apply scaling
            g2.scale(scaleX.toDouble(), scaleY.toDouble())

            // Draw each component (they'll be drawn in design coordinates)
            // Use a copy of the list to avoid ConcurrentModificationException if modified during render
            val componentsToRender = ArrayList(components)
            for (component in componentsToRender) {
                component.render(g2, designWidth, designHeight)
            }
        } finally {
            // Restore original transformation even if error occurs
            g2.transform = originalTransform
        }
    }

    // Find component at position (accounting for scaling)
    fun getComponentAt(x: Int, y: Int, currentWidth: Int, currentHeight: Int): UIComponent? {
        // Calculate scale factors, avoid division by zero
        val scaleX = if (currentWidth > 0) designWidth.toFloat() / currentWidth else 1f
        val scaleY = if (currentHeight > 0) designHeight.toFloat() / currentHeight else 1f


        // Convert screen coordinates to design coordinates
        val designX = (x * scaleX).toInt()
        val designY = (y * scaleY).toInt()

        // Check in reverse order to get the topmost component
        // Use indices to avoid iterator issues if components change
        for (i in components.indices.reversed()) {
            try { // Add try-catch for safety if component list changes unexpectedly
                val component = components[i]
                if (component.contains(designX, designY)) {
                    return component
                }
            } catch (e: IndexOutOfBoundsException) {
                println("Warning: Index out of bounds while checking UI component at $i. List size: ${components.size}")
                // Can happen if components are removed concurrently, might need better synchronization if this occurs often
                return null // Stop checking if list is inconsistent
            }
        }
        return null
    }

    // Save UI layout to file
    fun saveToFile(file: File): Boolean {
        return try {
            // Filter out non-serializable components before saving
            val serializableComponents = components.toList()
            if (serializableComponents.size != components.size) {
                println("Warning: Some non-serializable UI components were not saved.")
            }

            ObjectOutputStream(FileOutputStream(file)).use { out ->
                // Save design dimensions as well
                out.writeInt(designWidth)
                out.writeInt(designHeight)
                // Save only the serializable components
                out.writeObject(ArrayList(serializableComponents)) // Use ArrayList for compatibility
            }
            true // Indicate success
        } catch (e: NotSerializableException) {
            println("Error saving UI layout: A component is not serializable. ${e.message}")
            e.printStackTrace() // Print stack trace for easier debugging
            false // Indicate failure
        }
        catch (e: Exception) {
            println("Error saving UI layout: ${e.message}")
            e.printStackTrace()
            false // Indicate failure
        }
    }

    // Load UI layout from file
    fun loadFromFile(file: File): Boolean {
        return try {
            ObjectInputStream(FileInputStream(file)).use { input ->
                // Load design dimensions
                designWidth = input.readInt()
                designHeight = input.readInt()

                // Read the list - make sure it's cast correctly
                val loadedObject = input.readObject()
                if (loadedObject is List<*>) {
                    // Perform a safe cast for each element
                    val loadedComponents = loadedObject.mapNotNull { it as? UIComponent }

                    components.clear()
                    components.addAll(loadedComponents)

                    if (loadedComponents.size != loadedObject.size) {
                        println("Warning: Some objects loaded from the file were not valid UIComponents.")
                    }
                    true // Indicate success
                } else {
                    println("Error loading UI layout: Expected a List<UIComponent> but got ${loadedObject?.javaClass?.name}")
                    components.clear() // Clear components on load failure
                    createDefaultLayout(designWidth, designHeight) // Reset to default
                    false // Indicate failure
                }
            }
        } catch (e: ClassNotFoundException) {
            println("Error loading UI layout: A class definition used in the save file was not found. ${e.message}")
            e.printStackTrace()
            components.clear()
            createDefaultLayout(designWidth, designHeight) // Reset to default
            false // Indicate failure
        } catch (e: InvalidClassException) {
            println("Error loading UI layout: Incompatible class version (serialization mismatch). ${e.message}")
            e.printStackTrace()
            components.clear()
            createDefaultLayout(designWidth, designHeight) // Reset to default
            false // Indicate failure
        }
        catch (e: Exception) {
            println("Error loading UI layout: ${e.message}")
            e.printStackTrace()
            components.clear()
            createDefaultLayout(designWidth, designHeight) // Reset to default
            false // Indicate failure
        }
    }


    // Create default UI layout
    fun createDefaultLayout(screenWidth: Int, screenHeight: Int) {
        components.clear()

        // Use reasonable defaults if dimensions are zero or negative
        designWidth = screenWidth.coerceAtLeast(100)
        designHeight = screenHeight.coerceAtLeast(100)

        // Recalculate positions based on potentially adjusted design dimensions
        val statusBarHeight = (designHeight * 0.15f).toInt().coerceAtLeast(60) // e.g. 15% of height, min 60
        val panelY = designHeight - statusBarHeight - 10 // 10px padding from bottom
        val panelWidth = (designWidth * 0.25f).toInt().coerceAtLeast(150) // e.g. 25% of width, min 150
        val panelHeight = statusBarHeight // Use full calculated status bar height

        val facePanelWidth = 170
        val facePanelX = screenWidth / 2 - facePanelWidth / 2

        val selectorSize = (designHeight * 0.1f).toInt().coerceAtLeast(50) // e.g. 10% of height, min 50
        val selectorX = designWidth - selectorSize - 15 // 15px padding from right

        println("Creating default layout with Design: ${designWidth}x${designHeight}, StatusH: $statusBarHeight, PanelW: $panelWidth, FaceW: $facePanelWidth")


        // Health bar (left side)
        val healthBar = HealthBarComponent(15, panelY, panelWidth, panelHeight)
        healthBar.id = "health"
        addComponent(healthBar)

        // Face component (center)
        val faceComponent = FaceComponent(facePanelX, panelY, facePanelWidth, panelHeight)
        faceComponent.id = "face"
        addComponent(faceComponent)

        // Ammo bar (right side)
        val ammoBar = AmmoBarComponent(designWidth - panelWidth - 15, panelY, panelWidth, panelHeight)
        ammoBar.id = "ammo"
        addComponent(ammoBar)

        // Weapon selector (adjusted position and size)
        // Place it slightly above the ammo bar for clarity, maybe align top
        val weaponSelector = WeaponSelectorComponent(selectorX, panelY, selectorSize, selectorSize)
        weaponSelector.id = "weapon"
        addComponent(weaponSelector)
    }
}