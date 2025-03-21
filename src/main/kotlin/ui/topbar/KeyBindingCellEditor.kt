package ui.topbar

import controls.KeyBindingManager
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

class KeyBindingCellEditor(
    private val table: JTable,
    private val controlsManager: ControlsManager
) : DefaultCellEditor(JTextField()) {
    private var currentRow = -1
    private var currentValue: String? = null

    private val backgroundColor = Color(32, 34, 37)
    private val accentColor = Color(114, 137, 218)

    init {
        val textField = component as JTextField
        textField.horizontalAlignment = JTextField.CENTER

        // Add key listener
        textField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                // Support for unbinding with Backspace or Delete
                if (e.keyCode == KeyEvent.VK_BACK_SPACE || e.keyCode == KeyEvent.VK_DELETE) {
                    textField.text = "None"
                    currentValue = KeyBindingManager.KEY_UNBOUND.toString()
                    stopCellEditing()
                    e.consume()
                    return
                }

                // Ignore modifier keys
                if (e.keyCode == KeyEvent.VK_SHIFT || e.keyCode == KeyEvent.VK_CONTROL ||
                    e.keyCode == KeyEvent.VK_ALT || e.keyCode == KeyEvent.VK_META) {
                    return
                }

                // Set the text to the key name
                textField.text = KeyEvent.getKeyText(e.keyCode)

                // Store the key code to be used when stopping cell editing
                currentValue = e.keyCode.toString()

                // Check for conflicts
                val actionName = table.getValueAt(currentRow, 0).toString()
                // Find the key in keyBindingManager
                val keyName = controlsManager.keyBindingManager.getConfigurableBindings().entries
                    .find { controlsManager.keyBindingManager.getBindingDisplayName(it.key) == actionName }?.key

                if (keyName != null) {
                    val conflictingAction = controlsManager.keyBindingManager.getConflictingBinding(e.keyCode, keyName)
                    if (conflictingAction != null) {
                        // Show conflict warning with custom styling
                        val optionPane = JOptionPane(
                            "<html><body style='font-family:SansSerif;font-size:12;color:black;'>" +
                                    "This key is already assigned to:<br/>" +
                                    "<b>${controlsManager.keyBindingManager.getBindingDisplayName(conflictingAction)}</b><br/><br/>" +
                                    "If you proceed, it will be unassigned from that action.</body></html>",
                            JOptionPane.WARNING_MESSAGE,
                            JOptionPane.OK_CANCEL_OPTION
                        )

                        // Style the option pane
                        val frame = SwingUtilities.getWindowAncestor(table) as? JFrame
                        val dialog = optionPane.createDialog(frame, "Key Conflict")
                        dialog.contentPane.background = backgroundColor
                        dialog.setLocationRelativeTo(frame) // Center the dialog
                        dialog.isVisible = true

                        val result = optionPane.value
                        if (result == null || result != JOptionPane.OK_OPTION) {
                            // User canceled
                            cancelCellEditing()
                            return
                        }
                    }
                }

                // Stop editing when a key is pressed
                stopCellEditing()

                // Consume the event to prevent it from being processed elsewhere
                e.consume()
            }
        })

        // Ignore mouse events
        textField.addMouseListener(null)
    }

    override fun getTableCellEditorComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        row: Int,
        column: Int
    ): Component {
        currentRow = row
        currentValue = null

        val textField = super.getTableCellEditorComponent(table, "Press Any Key", isSelected, row, column) as JTextField
        textField.background = accentColor
        textField.foreground = Color.WHITE
        textField.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        )
        textField.font = Font("SansSerif", Font.BOLD, 12)

        return textField
    }

    override fun stopCellEditing(): Boolean {
        component as JTextField

        // Get the action name
        val actionName = table.getValueAt(currentRow, 0).toString()

        // Find the key in keyBindingManager
        val keyName = controlsManager.keyBindingManager.getConfigurableBindings().entries
            .find { controlsManager.keyBindingManager.getBindingDisplayName(it.key) == actionName }?.key

        if (keyName != null && currentValue != null) {
            val keyCode = currentValue!!.toInt()
            controlsManager.keyBindingManager.setKeyBinding(keyName, keyCode)

            // Reload table to show any conflicting bindings that were resolved
            SwingUtilities.invokeLater {
                controlsManager.refreshTable(table)
            }
        }

        return super.stopCellEditing()
    }

    override fun getCellEditorValue(): Any {
        return if (currentValue != null) {
            KeyEvent.getKeyText(currentValue!!.toInt())
        } else {
            super.getCellEditorValue()
        }
    }
}