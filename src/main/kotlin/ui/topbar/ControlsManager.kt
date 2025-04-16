package ui.topbar

import keyinput.KeyBindingManager
import keyinput.KeyBindings
import java.awt.Component
import javax.swing.JOptionPane
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class ControlsManager {
    val keyBindingManager = KeyBindings.getManager()

    fun showControlsDialog(parentComponent: Component) {
        val dialog = ControlsDialog(parentComponent, this)
        dialog.show()
    }

    fun showControlsConfigDialog(parentComponent: Component) {
        showControlsDialog(parentComponent)
    }

    fun refreshTable(table: JTable?) {
        if (table == null) return

        val tableModel = table.model as DefaultTableModel
        val bindings = keyBindingManager.getConfigurableBindings()

        for (row in 0..<tableModel.rowCount) {
            val action = tableModel.getValueAt(row, 0).toString()
            val keyEntry = bindings.entries.find { keyBindingManager.getBindingDisplayName(it.key) == action }

            if (keyEntry != null) {
                val keyName = keyEntry.key
                val keyValue = keyEntry.value

                // Update key column
                tableModel.setValueAt(
                    if (keyValue == KeyBindingManager.KEY_UNBOUND) "None"
                    else keyBindingManager.getKeyName(keyValue),
                    row, 1
                )

                // Update status column
                val status = when {
                    keyBindingManager.isActionDisabled(keyName) -> "Disabled"
                    keyValue == KeyBindingManager.KEY_UNBOUND -> "Unbound"
                    else -> "Active"
                }
                tableModel.setValueAt(status, row, 2)
            }
        }
    }

    fun resetKeyBindings(parentComponent: Component): Boolean {
        val response = JOptionPane.showConfirmDialog(
            parentComponent,
            "Reset all key bindings to defaults?",
            "Confirm Reset",
            JOptionPane.YES_NO_OPTION
        )

        if (response == JOptionPane.YES_OPTION) {
            keyBindingManager.resetAllKeyBindings()
            keyBindingManager.saveKeyBindings()
            return true
        }
        return false
    }

    fun saveKeyBindings(parentComponent: Component) {
        keyBindingManager.saveKeyBindings()
        JOptionPane.showMessageDialog(
            parentComponent,
            "Key bindings saved successfully!",
            "Save Success",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
}