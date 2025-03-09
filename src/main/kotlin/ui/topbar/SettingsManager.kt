package ui.topbar

import Renderer
import SettingsSaver
import ui.components.DisplayOptionsPanel
import Game3D
import java.awt.Component
import java.awt.Container

class SettingsManager(
    private val renderer: Renderer,
    private val game3D: Game3D
) {
    private val settingsSaver = SettingsSaver()

    fun saveSettings(displayOptionsPanel: DisplayOptionsPanel?): Pair<Boolean, Boolean> {
        var displaySuccess = false
        var worldSuccess = false

        // Save display settings if panel is available
        if (displayOptionsPanel != null) {
            displaySuccess = settingsSaver.saveDisplayOptions(displayOptionsPanel)
        }

        // Save world settings (includes both renderer and Game3D settings)
        worldSuccess = settingsSaver.saveWorldSettings(renderer, game3D)

        return Pair(displaySuccess, worldSuccess)
    }

    fun loadSettings(displayOptionsPanel: DisplayOptionsPanel?): Pair<Boolean, Boolean> {
        var displaySuccess = false
        var worldSuccess = false

        // Load display settings if panel is available
        if (displayOptionsPanel != null) {
            displaySuccess = settingsSaver.loadDisplayOptions(displayOptionsPanel)
        }

        // Load world settings (includes both renderer and Game3D settings)
        worldSuccess = settingsSaver.loadWorldSettings(renderer, game3D)

        return Pair(displaySuccess, worldSuccess)
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