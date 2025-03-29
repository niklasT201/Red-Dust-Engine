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

    private val editorBackgroundColor = Color(114, 137, 218)

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
                        val message = "<html><body style='font-family:SansSerif;'>" +
                                "This key is already assigned to:<br/>" +
                                "<b style='color:rgb(220,95,60);'>${controlsManager.keyBindingManager.getBindingDisplayName(conflictingAction)}</b><br/><br/>" +
                                "Do you want to unassign it from that action and assign it here?</body></html>"

                        val parentWindow = SwingUtilities.getWindowAncestor(table)
                        val result = StyledWarningDialog.showWarning(parentWindow, "Key Conflict", message)

                        if (result != StyledWarningDialog.Result.OK) {
                            // User canceled
                            cancelCellEditing()
                            e.consume()
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
        textField.background = editorBackgroundColor
        textField.foreground = Color.WHITE
        textField.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        )
        textField.font = Font("SansSerif", Font.BOLD, 12)

        // Request focus immediately so key events are captured
        SwingUtilities.invokeLater { textField.requestFocusInWindow() }

        return textField
    }

    override fun stopCellEditing(): Boolean {
        // Get the action name
        val actionName = table.getValueAt(currentRow, 0).toString()

        val keyEnumName = controlsManager.keyBindingManager.getConfigurableBindings().entries
            .find { controlsManager.keyBindingManager.getBindingDisplayName(it.key) == actionName }?.key

        if (keyEnumName != null && currentValue != null) {
            val keyCode = currentValue!!.toInt()

            println("Attempting to bind '$actionName' (Enum: $keyEnumName) to key code $keyCode")

            controlsManager.keyBindingManager.setKeyBinding(keyEnumName, keyCode)

            SwingUtilities.invokeLater {
                println("Refreshing table after binding...")
                controlsManager.refreshTable(table)
            }
        } else {
            println("Skipping binding: keyEnumName=$keyEnumName, currentValue=$currentValue")
        }

        return super.stopCellEditing()
    }

    override fun getCellEditorValue(): Any {
        return if (currentValue != null && currentValue != KeyBindingManager.KEY_UNBOUND.toString()) {
            KeyEvent.getKeyText(currentValue!!.toInt())
        } else if (currentValue == KeyBindingManager.KEY_UNBOUND.toString()) {
            "None"
        } else {
            table.getValueAt(currentRow, 1) ?: ""
        }
    }
}