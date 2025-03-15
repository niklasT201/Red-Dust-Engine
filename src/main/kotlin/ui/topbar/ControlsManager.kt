package ui.topbar

import controls.KeyBindings
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.CompoundBorder
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import java.awt.event.ItemEvent

class ControlsManager {
    private val keyBindingManager = KeyBindings.getManager()

    // UI Colors
    private val backgroundColor = Color(32, 34, 37)
    private val foregroundColor = Color(220, 221, 222)
    private val accentColor = Color(114, 137, 218)
    private val hoverColor = Color(66, 70, 77)
    private val borderColor = Color(47, 49, 54)
    private val headerColor = Color(47, 49, 54)

    fun showControlsDialog(parentComponent: Component) {
        val dialog = JDialog(SwingUtilities.getWindowAncestor(parentComponent) as? JFrame, "Controls", true)
        dialog.layout = BorderLayout(0, 0)
        dialog.background = backgroundColor

        // Create main panel with padding
        val mainPanel = JPanel(BorderLayout(0, 10))
        mainPanel.background = backgroundColor
        mainPanel.border = EmptyBorder(15, 15, 15, 15)

        // Create title panel
        val titlePanel = JPanel(BorderLayout())
        titlePanel.background = backgroundColor
        titlePanel.border = EmptyBorder(0, 0, 10, 0)

        val titleLabel = JLabel("KEYBOARD CONTROLS")
        titleLabel.font = Font("SansSerif", Font.BOLD, 18)
        titleLabel.foreground = foregroundColor
        titlePanel.add(titleLabel, BorderLayout.WEST)

        // Create toggle for showing fixed controls
        val showFixedControls = JCheckBox("Show Fixed Controls")
        showFixedControls.background = backgroundColor
        showFixedControls.foreground = foregroundColor
        //showFixedControls.focusPainted = false
        showFixedControls.isSelected = false
        titlePanel.add(showFixedControls, BorderLayout.EAST)

        mainPanel.add(titlePanel, BorderLayout.NORTH)

        // Create card layout for switching between views
        val cardPanel = JPanel(CardLayout())
        cardPanel.background = backgroundColor

        // Create configurable controls panel
        val configurablePanel = createConfigurableControlsPanel(dialog)
        cardPanel.add(configurablePanel, "configurable")

        // Create combined panel (configurable + fixed)
        val combinedPanel = JPanel(BorderLayout(0, 10))
        combinedPanel.background = backgroundColor

        // Add configurable controls to top
        val configPanel = createConfigurableControlsPanel(dialog)
        configPanel.border = CompoundBorder(
            EmptyBorder(0, 0, 10, 0),
            configPanel.border
        )
        combinedPanel.add(configPanel, BorderLayout.CENTER)

        // Add fixed controls to bottom
        val fixedPanel = createFixedControlsPanel()
        combinedPanel.add(fixedPanel, BorderLayout.SOUTH)

        cardPanel.add(combinedPanel, "combined")

        mainPanel.add(cardPanel, BorderLayout.CENTER)

        // Add button panel
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonPanel.background = backgroundColor

        val resetButton = createStyledButton("Reset All")
        resetButton.addActionListener {
            val response = JOptionPane.showConfirmDialog(
                dialog,
                "Reset all key bindings to defaults?",
                "Confirm Reset",
                JOptionPane.YES_NO_OPTION
            )

            if (response == JOptionPane.YES_OPTION) {
                keyBindingManager.resetAllKeyBindings()
                keyBindingManager.saveKeyBindings()

                // Find both tables correctly
                val configurableTable = findConfigurableTable(configurablePanel)
                val combinedConfigTable = findConfigurableTable(configPanel)

                // Refresh both tables
                refreshTable(configurableTable)
                refreshTable(combinedConfigTable)
            }
        }

        val saveButton = createStyledButton("Save")
        saveButton.addActionListener {
            keyBindingManager.saveKeyBindings()
            JOptionPane.showMessageDialog(
                dialog,
                "Key bindings saved successfully!",
                "Save Success",
                JOptionPane.INFORMATION_MESSAGE
            )
        }

        val closeButton = createStyledButton("Close")
        closeButton.addActionListener { dialog.dispose() }

        buttonPanel.add(resetButton)
        buttonPanel.add(saveButton)
        buttonPanel.add(closeButton)

        mainPanel.add(buttonPanel, BorderLayout.SOUTH)

        // Add toggle listener
        showFixedControls.addItemListener { e ->
            val cardLayout = cardPanel.layout as CardLayout
            if (e.stateChange == ItemEvent.SELECTED) {
                cardLayout.show(cardPanel, "combined")
            } else {
                cardLayout.show(cardPanel, "configurable")
            }
        }

        dialog.add(mainPanel)
        dialog.setSize(500, 500)
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(parentComponent)) // Center the dialog
        dialog.isVisible = true
    }

    private fun createStyledButton(text: String): JButton {
        return JButton(text).apply {
            background = hoverColor
            foreground = foregroundColor
            //focusPainted = false
            border = CompoundBorder(
                EmptyBorder(8, 12, 8, 12),
                border
            )
            font = Font("SansSerif", Font.BOLD, 12)
        }
    }

    private fun findConfigurableTable(panel: JPanel): JTable? {
        for (component in panel.components) {
            if (component is JScrollPane) {
                val viewport = component.viewport
                if (viewport.view is JTable) {
                    return viewport.view as JTable
                }
            }
        }
        return null
    }

    private fun refreshTable(table: JTable?) {
        if (table == null) return

        val tableModel = table.model as DefaultTableModel
        val bindings = keyBindingManager.getConfigurableBindings()

        for (row in 0..<tableModel.rowCount) {
            val action = tableModel.getValueAt(row, 0).toString()
            val key = bindings.entries.find { keyBindingManager.getBindingDisplayName(it.key) == action }?.value
            if (key != null) {
                tableModel.setValueAt(keyBindingManager.getKeyName(key), row, 1)
            }
        }
    }

    private fun createConfigurableControlsPanel(dialog: JDialog): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = backgroundColor
        panel.border = EmptyBorder(0, 0, 0, 0)

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

        // Create table
        val table = object : JTable(tableModel) {
            override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
                val component = super.prepareRenderer(renderer, row, column)
                component.background = when {
                    isRowSelected(row) -> accentColor
                    row % 2 == 0 -> backgroundColor
                    else -> backgroundColor.darker()
                }
                component.foreground = foregroundColor
                return component
            }
        }.apply {
            background = backgroundColor
            foreground = foregroundColor
            gridColor = borderColor
            rowHeight = 30
            setShowGrid(false)
            showHorizontalLines = true
            intercellSpacing = Dimension(0, 1)
            tableHeader.background = headerColor
            tableHeader.foreground = foregroundColor
            tableHeader.font = Font("SansSerif", Font.BOLD, 12)
            font = Font("SansSerif", Font.PLAIN, 12)
            selectionBackground = accentColor
            selectionForeground = Color.WHITE

            // Custom cell editor
            val keyColumnEditor = KeyBindingCellEditor(this)
            getColumnModel().getColumn(1).cellEditor = keyColumnEditor

            // Column widths
            getColumnModel().getColumn(0).preferredWidth = 250
            getColumnModel().getColumn(1).preferredWidth = 150
        }

        // Create scrollpane
        val scrollPane = JScrollPane(table).apply {
            background = backgroundColor
            viewport.background = backgroundColor
            border = CompoundBorder(
                EmptyBorder(0, 0, 0, 0),
                border
            )
        }

        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    private fun createFixedControlsPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = backgroundColor
        panel.border = CompoundBorder(
            CompoundBorder(
                EmptyBorder(10, 0, 0, 0),
                BorderFactory.createLineBorder(borderColor, 1)
            ),
            EmptyBorder(10, 10, 10, 10)
        )

        // Create title
        val titleLabel = JLabel("FIXED CONTROLS (CANNOT BE CHANGED)")
        titleLabel.foreground = foregroundColor
        titleLabel.font = Font("SansSerif", Font.BOLD, 14)
        titleLabel.border = EmptyBorder(0, 0, 10, 0)
        panel.add(titleLabel, BorderLayout.NORTH)

        // Create grid panel for fixed controls
        val gridPanel = JPanel(GridLayout(0, 2, 20, 5))
        gridPanel.background = backgroundColor

        // Map of fixed controls
        val fixedControls = mapOf(
            "Rotate North" to "N",
            "Rotate South" to "S",
            "Rotate East" to "O",
            "Rotate West" to "W",
            "Rotate Wall" to "R",
            "Wall Tool" to "1",
            "Floor Tool" to "2",
            "Player Spawn Tool" to "3",
            "New Project" to "Ctrl+N",
            "Open Project" to "Ctrl+O",
            "Save" to "Ctrl+S",
            "Save As" to "Ctrl+Shift+S",
            "Quick Save" to "F5",
            "Quick Load" to "F9",
            "Undo" to "Ctrl+Z",
            "Redo" to "Ctrl+Y"
        )

        // Add control labels
        fixedControls.forEach { (action, key) ->
            val actionLabel = JLabel(action)
            actionLabel.foreground = foregroundColor.darker()
            actionLabel.font = Font("SansSerif", Font.PLAIN, 12)

            val keyLabel = JLabel(key)
            keyLabel.foreground = foregroundColor.darker()
            keyLabel.font = Font("Monospace", Font.BOLD, 12)
            keyLabel.horizontalAlignment = SwingConstants.RIGHT

            gridPanel.add(actionLabel)
            gridPanel.add(keyLabel)
        }

        panel.add(gridPanel, BorderLayout.CENTER)

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
                            // Show conflict warning with custom styling
                            val optionPane = JOptionPane(
                                "<html><body style='font-family:SansSerif;font-size:12;color:white;'>" +
                                        "This key is already assigned to:<br/>" +
                                        "<b>${keyBindingManager.getBindingDisplayName(conflictingAction)}</b><br/><br/>" +
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