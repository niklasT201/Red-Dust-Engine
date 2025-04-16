package ui.topbar

import keyinput.KeyBindingManager
import java.awt.*
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

object ControlsPanelFactory {
    // Updated color scheme to match about dialog
    private val backgroundColor = Color(35, 38, 45)
    private val foregroundColor = Color(220, 221, 222)
    private val accentColor = Color(220, 95, 60)
    private val borderColor = Color(25, 28, 35)
    private val headerColor = Color(45, 48, 55)

    fun createConfigurableControlsPanel(dialog: JDialog, controlsManager: ControlsManager): JPanel {
        val panel = JPanel(BorderLayout())
        panel.isOpaque = false
        panel.border = EmptyBorder(0, 0, 0, 0)

        // Get all configurable key bindings
        val bindings = controlsManager.keyBindingManager.getConfigurableBindings()

        // Create table model
        val tableModel = object : DefaultTableModel() {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                // Only the key column (1) is editable
                return column == 1
            }
        }

        tableModel.addColumn("Action")
        tableModel.addColumn("Key")
        tableModel.addColumn("Status")

        // Add rows for each binding
        bindings.entries.sortedBy { controlsManager.keyBindingManager.getBindingDisplayName(it.key) }.forEach { (key, value) ->
            val status = when {
                controlsManager.keyBindingManager.isActionDisabled(key) -> "Disabled"
                value == KeyBindingManager.KEY_UNBOUND -> "Unbound"
                else -> "Active"
            }

            tableModel.addRow(arrayOf(
                controlsManager.keyBindingManager.getBindingDisplayName(key),
                if (value == KeyBindingManager.KEY_UNBOUND) "None" else controlsManager.keyBindingManager.getKeyName(value),
                status
            ))
        }

        // Create table
        val table = object : JTable(tableModel) {
            override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
                val component = super.prepareRenderer(renderer, row, column)
                component.background = when {
                    isRowSelected(row) -> accentColor.darker()
                    row % 2 == 0 -> backgroundColor
                    else -> backgroundColor.brighter()
                }
                component.foreground = Color.WHITE

                // Add padding to the cell contents
                if (component is JLabel) {
                    // Add left padding to the text
                    component.border = EmptyBorder(8, 15, 8, 15)

                    // Set alignment based on column
                    component.horizontalAlignment = when (column) {
                        0 -> JLabel.LEFT
                        1 -> JLabel.CENTER
                        else -> JLabel.LEFT
                    }
                }

                return component
            }
        }.apply {
            background = backgroundColor
            foreground = Color.WHITE
            gridColor = borderColor
            rowHeight = 35
            setShowGrid(false)
            showHorizontalLines = true
            intercellSpacing = Dimension(0, 1)
            tableHeader.background = headerColor
            tableHeader.foreground = Color.WHITE
            tableHeader.font = Font("Arial", Font.BOLD, 12)
            font = Font("SansSerif", Font.PLAIN, 12)
            selectionBackground = accentColor
            selectionForeground = Color.WHITE

            // Custom cell editor
            val keyColumnEditor = KeyBindingCellEditor(this, controlsManager)
            getColumnModel().getColumn(1).cellEditor = keyColumnEditor

            // Column widths
            getColumnModel().getColumn(0).preferredWidth = 250
            getColumnModel().getColumn(1).preferredWidth = 150

            // Add padding to table header
            tableHeader.defaultRenderer = object : TableCellRenderer {
                val renderer = tableHeader.defaultRenderer

                override fun getTableCellRendererComponent(
                    table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
                ): Component {
                    val component = renderer.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column
                    )

                    if (component is JLabel) {
                        component.border = EmptyBorder(10, 15, 10, 15)

                        // Center the "Key" column header
                        if (column == 1) {
                            component.horizontalAlignment = JLabel.CENTER
                        }
                    }

                    return component
                }
            }
        }

        table.componentPopupMenu = createTableContextMenu(table, controlsManager)

        // Create scrollpane
        val scrollPane = JScrollPane(table).apply {
            background = backgroundColor
            viewport.background = backgroundColor
            border = CompoundBorder(
                EmptyBorder(0, 0, 0, 0),
                BorderFactory.createLineBorder(borderColor)
            )
        }

        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    fun createFixedControlsPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.isOpaque = false
        panel.border = CompoundBorder(
            CompoundBorder(
                EmptyBorder(10, 0, 0, 0),
                BorderFactory.createLineBorder(borderColor, 1)
            ),
            EmptyBorder(10, 10, 10, 10)
        )

        // Create title
        val titleLabel = JLabel("FIXED CONTROLS (CANNOT BE CHANGED)")
        titleLabel.foreground = Color(220, 95, 60)
        titleLabel.font = Font("Impact", Font.BOLD, 16)
        titleLabel.border = EmptyBorder(0, 0, 10, 0)
        panel.add(titleLabel, BorderLayout.NORTH)

        // Create grid panel for fixed controls
        val gridPanel = JPanel(GridLayout(0, 2, 20, 5))
        gridPanel.isOpaque = false

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
            actionLabel.foreground = Color(180, 180, 180)
            actionLabel.font = Font("Arial", Font.PLAIN, 12)

            val keyLabel = JLabel(key)
            keyLabel.foreground = Color(220, 95, 60)
            keyLabel.font = Font("Monospace", Font.BOLD, 12)
            keyLabel.horizontalAlignment = SwingConstants.RIGHT

            gridPanel.add(actionLabel)
            gridPanel.add(keyLabel)
        }

        panel.add(gridPanel, BorderLayout.CENTER)

        return panel
    }

    private fun createTableContextMenu(table: JTable, controlsManager: ControlsManager): JPopupMenu {
        val menu = JPopupMenu()
        menu.background = backgroundColor
        menu.border = BorderFactory.createLineBorder(borderColor)

        val unbindItem = JMenuItem("Unbind Key").apply {
            background = backgroundColor
            foreground = foregroundColor
            addActionListener {
                val row = table.selectedRow
                if (row >= 0) {
                    val actionName = table.getValueAt(row, 0).toString()
                    // Find the key in keyBindingManager
                    val keyName = controlsManager.keyBindingManager.getConfigurableBindings().entries
                        .find { controlsManager.keyBindingManager.getBindingDisplayName(it.key) == actionName }?.key

                    if (keyName != null) {
                        controlsManager.keyBindingManager.setKeyBinding(keyName, KeyBindingManager.KEY_UNBOUND)
                        controlsManager.refreshTable(table)
                    }
                }
            }
        }

        val disableItem = JMenuItem("Disable Action").apply {
            background = backgroundColor
            foreground = foregroundColor
            addActionListener {
                val row = table.selectedRow
                if (row >= 0) {
                    val actionName = table.getValueAt(row, 0).toString()
                    // Find the key in keyBindingManager
                    val keyName = controlsManager.keyBindingManager.getConfigurableBindings().entries
                        .find { controlsManager.keyBindingManager.getBindingDisplayName(it.key) == actionName }?.key

                    if (keyName != null) {
                        controlsManager.keyBindingManager.disableAction(keyName)
                        controlsManager.refreshTable(table)
                    }
                }
            }
        }

        val enableItem = JMenuItem("Enable Action").apply {
            background = backgroundColor
            foreground = foregroundColor
            addActionListener {
                val row = table.selectedRow
                if (row >= 0) {
                    val actionName = table.getValueAt(row, 0).toString()
                    // Find the key in keyBindingManager
                    val keyName = controlsManager.keyBindingManager.getConfigurableBindings().entries
                        .find { controlsManager.keyBindingManager.getBindingDisplayName(it.key) == actionName }?.key

                    if (keyName != null) {
                        controlsManager.keyBindingManager.enableAction(keyName)
                        controlsManager.refreshTable(table)
                    }
                }
            }
        }

        menu.add(unbindItem)
        menu.add(disableItem)
        menu.add(enableItem)

        return menu
    }
}