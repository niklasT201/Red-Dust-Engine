package ui.topbar

import Renderer
import SettingsSaver
import ui.components.DisplayOptionsPanel
import Game3D
import player.Player
import grideditor.GridEditor
import java.awt.Component
import java.awt.Container
import controls.KeyBindings

class SettingsManager(
    private val renderer: Renderer,
    private val game3D: Game3D,
    private val player: Player,
    private val gridEditor: GridEditor
) {
    private val settingsSaver = SettingsSaver(gridEditor)
    private val keyBindingManager = KeyBindings.getManager()

    fun saveSettings(displayOptionsPanel: DisplayOptionsPanel?): Triple<Boolean, Boolean, Boolean> {
        var displaySuccess = false
        var worldSuccess = false
        var playerSuccess = false

        // Save display settings if panel is available
        if (displayOptionsPanel != null) {
            displaySuccess = settingsSaver.saveDisplayOptions(displayOptionsPanel)
        }

        // Save world settings (includes both renderer and Game3D settings)
        worldSuccess = settingsSaver.saveWorldSettings(renderer, game3D)

        // Save player settings
        playerSuccess = settingsSaver.savePlayerSettings(player)

        // Save key bindings
        keyBindingManager.saveKeyBindings()

        return Triple(displaySuccess, worldSuccess, playerSuccess)
    }

    fun loadSettings(displayOptionsPanel: DisplayOptionsPanel?): Triple<Boolean, Boolean, Boolean> {
        var displaySuccess = false
        var worldSuccess = false
        var playerSuccess = false

        // Load display settings if panel is available
        if (displayOptionsPanel != null) {
            displaySuccess = settingsSaver.loadDisplayOptions(displayOptionsPanel)
        }

        // Load world settings (includes both renderer and Game3D settings)
        worldSuccess = settingsSaver.loadWorldSettings(renderer, game3D)

        // Load player settings
        playerSuccess = settingsSaver.loadPlayerSettings(player)

        // Load key bindings
        keyBindingManager.loadKeyBindings()

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
}