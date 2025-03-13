package ui.topbar

import controls.KeyBindings
import ui.MenuSystem
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

class ControlsManager {
    private val keyBindingManager = KeyBindings.getManager()

    fun showControlsDialog(parentComponent: Component) {
        // Create and show a dialog with all the controls
        val dialog = JDialog(SwingUtilities.getWindowAncestor(parentComponent) as? JFrame, "Controls List", true)
        dialog.layout = BorderLayout()
        dialog.background = MenuSystem.BACKGROUND_COLOR

        // Create tabbed pane for categories
        val tabbedPane = JTabbedPane().apply {
            background = MenuSystem.BACKGROUND_COLOR
            foreground = Color.WHITE
            border = EmptyBorder(10, 10, 10, 10)
        }

        // Add configurable controls tab
        tabbedPane.addTab("Configurable Controls", createConfigurableControlsPanel(dialog))

        // Add fixed controls tab
        tabbedPane.addTab("Fixed Controls", createFixedControlsPanel())

        dialog.add(tabbedPane, BorderLayout.CENTER)

        // Add close button
        dialog.add(JPanel().apply {
            background = MenuSystem.BACKGROUND_COLOR
            layout = FlowLayout(FlowLayout.RIGHT)

            add(JButton("Close").apply {
                background = MenuSystem.HOVER_COLOR
                foreground = Color.WHITE
                addActionListener { dialog.dispose() }
            })
        }, BorderLayout.SOUTH)

        dialog.setSize(600, 400)
        dialog.setLocationRelativeTo(parentComponent)
        dialog.isVisible = true
    }

    private fun createConfigurableControlsPanel(dialog: JDialog): JPanel {
        val panel = JPanel(BorderLayout()).apply {
            background = MenuSystem.BACKGROUND_COLOR
            border = EmptyBorder(10, 10, 10, 10)
        }

        // Get all configurable key bindings
        val bindings = keyBindingManager.getConfigurableBindings()

        // Create table model
        val tableModel = object : DefaultTableModel() {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                // Only the key column (1) is editable
                return column == 1
            }
        }

        tableModel.addColumn("Action")
        tableModel.addColumn("Key")

        // Add rows for each binding
        bindings.entries.sortedBy { keyBindingManager.getBindingDisplayName(it.key) }.forEach { (key, value) ->
            tableModel.addRow(arrayOf(
                keyBindingManager.getBindingDisplayName(key),
                keyBindingManager.getKeyName(value)
            ))
        }

        // Create the table
        val table = object : JTable(tableModel) {
            override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
                val component = super.prepareRenderer(renderer, row, column)
                component.background = MenuSystem.BACKGROUND_COLOR
                component.foreground = Color.WHITE

                // Highlight the cell if it's selected
                if (isRowSelected(row) && column == 1) {
                    component.background = MenuSystem.HOVER_COLOR
                }

                return component
            }
        }.apply {
            background = MenuSystem.BACKGROUND_COLOR
            foreground = Color.WHITE
            gridColor = MenuSystem.BORDER_COLOR
            tableHeader.background = MenuSystem.HOVER_COLOR
            tableHeader.foreground = Color.WHITE
            selectionBackground = MenuSystem.HOVER_COLOR
            selectionForeground = Color.WHITE
            rowHeight = 30

            // Custom cell editor for the key column
            val keyColumnEditor = KeyBindingCellEditor(this)
            getColumnModel().getColumn(1).cellEditor = keyColumnEditor
        }

        // Add table to scroll pane
        panel.add(JScrollPane(table).apply {
            background = MenuSystem.BACKGROUND_COLOR
            border = LineBorder(MenuSystem.BORDER_COLOR)
            viewport.background = MenuSystem.BACKGROUND_COLOR
        }, BorderLayout.CENTER)

        // Add button panel
        panel.add(JPanel().apply {
            background = MenuSystem.BACKGROUND_COLOR
            layout = FlowLayout(FlowLayout.CENTER)

            add(JButton("Reset All to Defaults").apply {
                background = MenuSystem.HOVER_COLOR
                foreground = Color.WHITE
                addActionListener {
                    // Show confirmation dialog
                    val response = JOptionPane.showConfirmDialog(
                        dialog,
                        "Reset all key bindings to defaults?",
                        "Confirm Reset",
                        JOptionPane.YES_NO_OPTION
                    )

                    if (response == JOptionPane.YES_OPTION) {
                        keyBindingManager.resetAllKeyBindings()
                        keyBindingManager.saveKeyBindings()

                        // Update table
                        val bindings = keyBindingManager.getConfigurableBindings()
                        for (row in 0 until tableModel.rowCount) {
                            val action = tableModel.getValueAt(row, 0).toString()
                            val key = bindings.entries.find { keyBindingManager.getBindingDisplayName(it.key) == action }?.value
                            if (key != null) {
                                tableModel.setValueAt(keyBindingManager.getKeyName(key), row, 1)
                            }
                        }
                    }
                }
            })

            add(JButton("Save").apply {
                background = MenuSystem.HOVER_COLOR
                foreground = Color.WHITE
                addActionListener {
                    keyBindingManager.saveKeyBindings()
                    JOptionPane.showMessageDialog(
                        dialog,
                        "Key bindings saved successfully!",
                        "Save Success",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
            })
        }, BorderLayout.SOUTH)

        return panel
    }

    private fun createFixedControlsPanel(): JPanel {
        val panel = JPanel(BorderLayout()).apply {
            background = MenuSystem.BACKGROUND_COLOR
            border = EmptyBorder(10, 10, 10, 10)
        }

        // Create text area for fixed controls
        val textArea = JTextArea().apply {
            background = MenuSystem.BACKGROUND_COLOR
            foreground = Color.WHITE
            isEditable = false
            font = Font("Monospace", Font.PLAIN, 12)

            // Add fixed controls info
            append("Direction Keys (Cannot Be Changed):\n")
            append("--------------------------------\n")
            append("Rotate North: N\n")
            append("Rotate South: S\n")
            append("Rotate East: O\n")
            append("Rotate West: W\n\n")

            append("Shortcuts (Cannot Be Changed):\n")
            append("----------------------------\n")
            append("Rotate Wall: R\n")
            append("Wall Tool: 1\n")
            append("Floor Tool: 2\n")
            append("Player Spawn Tool: 3\n\n")

            append("Other Shortcuts:\n")
            append("----------------\n")
            append("New Project: Ctrl+N\n")
            append("Open Project: Ctrl+O\n")
            append("Save: Ctrl+S\n")
            append("Save As: Ctrl+Shift+S\n")
            append("Quick Save: F5\n")
            append("Quick Load: F9\n")
            append("Undo: Ctrl+Z\n")
            append("Redo: Ctrl+Y\n")
        }

        // Add text area to scroll pane
        panel.add(JScrollPane(textArea).apply {
            background = MenuSystem.BACKGROUND_COLOR
            border = LineBorder(MenuSystem.BORDER_COLOR)
            viewport.background = MenuSystem.BACKGROUND_COLOR
        }, BorderLayout.CENTER)

        return panel
    }

    fun showControlsConfigDialog(parentComponent: Component) {
        showControlsDialog(parentComponent)
    }

    /**
     * Custom cell editor for key binding configuration
     */
    inner class KeyBindingCellEditor(private val table: JTable) : DefaultCellEditor(JTextField()) {
        private var currentRow = -1
        private var currentValue: String? = null

        init {
            val textField = component as JTextField
            textField.horizontalAlignment = JTextField.CENTER

            // Add key listener
            textField.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
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
                    val keyName = keyBindingManager.getConfigurableBindings().entries
                        .find { keyBindingManager.getBindingDisplayName(it.key) == actionName }?.key

                    if (keyName != null) {
                        val conflictingAction = keyBindingManager.getConflictingBinding(e.keyCode, keyName)
                        if (conflictingAction != null) {
                            // Show warning about conflict
                            JOptionPane.showMessageDialog(
                                table,
                                "This key is already assigned to '${keyBindingManager.getBindingDisplayName(conflictingAction)}'.\n" +
                                        "If you proceed, it will be unassigned from that action.",
                                "Key Conflict",
                                JOptionPane.WARNING_MESSAGE
                            )
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

            val textField = super.getTableCellEditorComponent(table, "Press any key...", isSelected, row, column) as JTextField
            textField.background = MenuSystem.HOVER_COLOR
            textField.foreground = Color.WHITE
            textField.border = LineBorder(Color.WHITE)

            return textField
        }

        override fun stopCellEditing(): Boolean {
            val textField = component as JTextField

            // Get the action name
            val actionName = table.getValueAt(currentRow, 0).toString()

            // Find the key in keyBindingManager
            val keyName = keyBindingManager.getConfigurableBindings().entries
                .find { keyBindingManager.getBindingDisplayName(it.key) == actionName }?.key

            if (keyName != null && currentValue != null) {
                val keyCode = currentValue!!.toInt()
                keyBindingManager.setKeyBinding(keyName, keyCode)

                // Reload table to show any conflicting bindings that were resolved
                SwingUtilities.invokeLater {
                    val model = table.model as DefaultTableModel
                    val bindings = keyBindingManager.getConfigurableBindings()

                    for (row in 0 until model.rowCount) {
                        val rowAction = model.getValueAt(row, 0).toString()
                        val key = bindings.entries.find { keyBindingManager.getBindingDisplayName(it.key) == rowAction }?.value
                        if (key != null) {
                            model.setValueAt(keyBindingManager.getKeyName(key), row, 1)
                        }
                    }
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
}