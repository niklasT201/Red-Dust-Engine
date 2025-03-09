package ui.topbar

import Renderer
import SettingsSaver
import ui.components.DisplayOptionsPanel
import java.awt.Component
import java.awt.Container

class SettingsManager(private val renderer: Renderer) {
    private val settingsSaver = SettingsSaver()

    fun saveSettings(displayOptionsPanel: DisplayOptionsPanel?): Pair<Boolean, Boolean> {
        var displaySuccess = false
        var rendererSuccess = false

        // Save display settings if panel is available
        if (displayOptionsPanel != null) {
            displaySuccess = settingsSaver.saveDisplayOptions(displayOptionsPanel)
        }

        // Save renderer settings
        rendererSuccess = settingsSaver.saveRendererSettings(renderer)

        return Pair(displaySuccess, rendererSuccess)
    }

    fun loadSettings(displayOptionsPanel: DisplayOptionsPanel?): Pair<Boolean, Boolean> {
        var displaySuccess = false
        var rendererSuccess = false

        // Load display settings if panel is available
        if (displayOptionsPanel != null) {
            displaySuccess = settingsSaver.loadDisplayOptions(displayOptionsPanel)
        }

        // Load renderer settings
        rendererSuccess = settingsSaver.loadRendererSettings(renderer)

        return Pair(displaySuccess, rendererSuccess)
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