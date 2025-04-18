package saving

import Renderer
import Game3D
import player.Player
import grideditor.GridEditor
import java.awt.Component
import java.awt.Container
import keyinput.KeyBindings
import ui.components.*
import javax.swing.SwingUtilities

class SettingsManager(
    private val renderer: Renderer,
    private val game3D: Game3D,
    private val player: Player,
    private val gridEditor: GridEditor
) {
    private val settingsSaver = SettingsSaver(gridEditor)
    private val keyBindingManager = KeyBindings.getManager()

    fun saveSettings(projectPath: String?, displayOptionsPanel: DisplayOptionsPanel?): Triple<Boolean, Boolean, Boolean> {
        if (projectPath == null) {
            println("Error: Cannot save settings. No project selected.")
            // Maybe show a user dialog here?
            return Triple(false, false, false)
        }

        var displaySuccess = false
        var worldSuccess = false
        var playerSuccess = false

        // Save display settings if panel is available
        if (displayOptionsPanel != null) {
            displaySuccess = settingsSaver.saveDisplayOptions(projectPath, displayOptionsPanel)
        }

        // Save world settings (includes both renderer and Game3D settings)
        worldSuccess = settingsSaver.saveWorldSettings(projectPath, renderer, game3D)

        // Save player settings (now also passing game3D for crosshair settings)
        playerSuccess = settingsSaver.savePlayerSettings(projectPath, player, game3D)

        // Save key bindings
        keyBindingManager.saveKeyBindings()

        return Triple(displaySuccess, worldSuccess, playerSuccess)
    }

    fun loadSettings(projectPath: String?, displayOptionsPanel: DisplayOptionsPanel?): Triple<Boolean, Boolean, Boolean> {
        if (projectPath == null) {
            println("Error: Cannot load settings. No project selected.")
            // Maybe show a user dialog here?
            return Triple(false, false, false)
        }

        var displaySuccess = false
        var worldSuccess = false
        var playerSuccess = false

        // Load display settings if panel is available
        if (displayOptionsPanel != null) {
            displaySuccess = settingsSaver.loadDisplayOptions(projectPath, displayOptionsPanel)
        }

        // Load world settings (includes both renderer and Game3D settings)
        worldSuccess = settingsSaver.loadWorldSettings(projectPath, renderer, game3D)

        // Load player settings (now also passing game3D for crosshair settings)
        playerSuccess = settingsSaver.loadPlayerSettings(projectPath, player, game3D)

        // Load key bindings
        keyBindingManager.loadKeyBindings()

        // After loading settings, refresh the UI components
        val rootComponent = SwingUtilities.getWindowAncestor(displayOptionsPanel)
        if (playerSuccess) {
            findCrosshairDebugOptionsPanel(rootComponent)?.refreshFromGameState()
            findDebugOptionsPanel(rootComponent)?.refreshFromGameState()
            findMouseControlSettingsPanel(rootComponent)?.updateAllSettingsDisplay()
            findPlayerSettingsPanel(rootComponent)?.refreshFromGameState()
        }

        // Refresh Sky Options Panel after loading world settings
        if (worldSuccess) {
            findSkyOptionsPanel(rootComponent)?.refreshFromGameState()
            findBorderStylePanel(rootComponent)?.refreshFromGameState()
            findRenderOptionsPanel(rootComponent)?.refreshFromGameState()
        }

        return Triple(displaySuccess, worldSuccess, playerSuccess)
    }

    fun findDisplayOptionsPanel(component: Component?): DisplayOptionsPanel? {
        if (component == null) return null
        if (component is DisplayOptionsPanel) return component

        if (component is Container) {
            for (i in 0..<component.componentCount) {
                val result = findDisplayOptionsPanel(component.getComponent(i))
                if (result != null) return result
            }
        }

        return null
    }

    private fun findCrosshairDebugOptionsPanel(component: Component?): PlayerOptionsPanel? {
        if (component == null) return null
        if (component is PlayerOptionsPanel) return component

        if (component is Container) {
            for (i in 0..<component.componentCount) {
                val result = findCrosshairDebugOptionsPanel(component.getComponent(i))
                if (result != null) return result
            }
        }

        return null
    }

    private fun findDebugOptionsPanel(component: Component?): DebugOptionsPanel? {
        if (component == null) return null
        if (component is DebugOptionsPanel) return component

        if (component is Container) {
            for (i in 0..<component.componentCount) {
                val result = findDebugOptionsPanel(component.getComponent(i))
                if (result != null) return result
            }
        }

        return null
    }

    private fun findSkyOptionsPanel(component: Component?): SkyOptionsPanel? {
        if (component == null) return null
        if (component is SkyOptionsPanel) return component

        if (component is Container) {
            for (i in 0..<component.componentCount) {
                val result = findSkyOptionsPanel(component.getComponent(i))
                if (result != null) return result
            }
        }

        return null
    }

    private fun findBorderStylePanel(component: Component?): BorderStylePanel? {
        if (component == null) return null
        if (component is BorderStylePanel) return component

        if (component is Container) {
            for (i in 0..<component.componentCount) {
                val result = findBorderStylePanel(component.getComponent(i))
                if (result != null) return result
            }
        }

        return null
    }

    private fun findRenderOptionsPanel(component: Component?): RenderOptionsPanel? {
        if (component == null) return null
        if (component is RenderOptionsPanel) return component

        if (component is Container) {
            for (i in 0..<component.componentCount) {
                val result = findRenderOptionsPanel(component.getComponent(i))
                if (result != null) return result
            }
        }

        return null
    }

    private fun findMouseControlSettingsPanel(component: Component?): MouseControlSettingsPanel? {
        if (component == null) return null
        if (component is MouseControlSettingsPanel) return component

        if (component is Container) {
            for (i in 0..<component.componentCount) {
                val result = findMouseControlSettingsPanel(component.getComponent(i))
                if (result != null) return result
            }
        }
        return null
    }

    private fun findPlayerSettingsPanel(component: Component?): PlayerSettingsPanel? {
        if (component == null) return null
        if (component is PlayerSettingsPanel) return component // Check for the correct type

        if (component is Container) {
            for (i in 0..<component.componentCount) {
                // Recursive call to search children
                val result = findPlayerSettingsPanel(component.getComponent(i))
                if (result != null) return result
            }
        }
        return null // Not found in this branch
    }
}