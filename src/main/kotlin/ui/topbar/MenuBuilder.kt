package ui.topbar

import grideditor.GridEditor
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

class MenuBuilder(
    private val fileManager: FileManager,
    private val controlsManager: ControlsManager,
    private val settingsManager: SettingsManager,
    private val gridEditor: GridEditor
) {
    companion object {
        val BACKGROUND_COLOR = Color(40, 44, 52)
        val BORDER_COLOR = Color(28, 31, 36)
        val HOVER_COLOR = Color(60, 63, 65)
        val SEPARATOR_COLOR = Color(70, 73, 75)
    }

    private fun createMenuItem(text: String, accelerator: KeyStroke? = null): JMenuItem {
        return JMenuItem(text).apply {
            background = BACKGROUND_COLOR
            foreground = Color.WHITE
            this.accelerator = accelerator
            border = EmptyBorder(5, 15, 5, 15)

            // Hover effect
            addChangeListener { e ->
                if (isArmed) {
                    background = HOVER_COLOR
                } else {
                    background = BACKGROUND_COLOR
                }
            }
        }
    }

    fun createMenu(text: String, mnemonic: Int): JMenu {
        return JMenu(text).apply {
            this.mnemonic = mnemonic
            background = BACKGROUND_COLOR
            foreground = Color.WHITE
            border = EmptyBorder(5, 5, 5, 5)

            // Custom popup menu style
            popupMenu.border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(2, 0, 2, 0)
            )
            popupMenu.background = BACKGROUND_COLOR

            // Add hover effect for top-level menu items
            addChangeListener { e ->
                if (isSelected || isArmed) {
                    background = HOVER_COLOR
                } else {
                    background = BACKGROUND_COLOR
                }
            }
        }
    }

    fun createFileMenu(parentComponent: Component): JMenu {
        return createMenu("File", KeyEvent.VK_F).apply {
            add(createMenuItem("New Project",
                KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK)).apply {
                addActionListener {
                    if (confirmUnsavedChanges(parentComponent)) {
                        gridEditor.clearGrid()
                        fileManager.resetCurrentFile()
                    }
                }
            })

            add(createMenuItem("Open Project",
                KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK)).apply {
                addActionListener {
                    if (confirmUnsavedChanges(parentComponent)) {
                        if (fileManager.loadWorld(parentComponent)) {
                            showNotification(parentComponent, "World loaded successfully")
                        } else {
                            JOptionPane.showMessageDialog(
                                parentComponent,
                                "Failed to load world.",
                                "Load Error",
                                JOptionPane.ERROR_MESSAGE
                            )
                        }
                    }
                }
            })

            add(createMenuItem("Save",
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)).apply {
                addActionListener {
                    if (fileManager.saveWorld(parentComponent, false)) {
                        showNotification(parentComponent, "World saved successfully")
                    } else {
                        JOptionPane.showMessageDialog(
                            parentComponent,
                            "Failed to save world.",
                            "Save Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            })

            add(createMenuItem("Save As...",
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)).apply {
                addActionListener {
                    if (fileManager.saveWorld(parentComponent, true)) {
                        showNotification(parentComponent, "World saved successfully")
                    } else {
                        JOptionPane.showMessageDialog(
                            parentComponent,
                            "Failed to save world.",
                            "Save Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            })

            addSeparator()

            // Quick save/load
            add(createMenuItem("Quick Save",
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)).apply {
                addActionListener {
                    if (fileManager.quickSave(parentComponent) != null) {
                        showNotification(parentComponent, "Quick save successful")
                    } else {
                        JOptionPane.showMessageDialog(
                            parentComponent,
                            "Quick save failed.",
                            "Save Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            })

            add(createMenuItem("Quick Load",
                KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)).apply {
                addActionListener {
                    if (fileManager.quickLoad(parentComponent)) {
                        showNotification(parentComponent, "Quick load successful")
                    } else {
                        JOptionPane.showMessageDialog(
                            parentComponent,
                            "No valid quick saves found.",
                            "Quick Load",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                    }
                }
            })

            addSeparator()

            // Settings section
            add(createMenuItem("Save Settings").apply {
                addActionListener {
                    val displayOptionsPanel = settingsManager.findDisplayOptionsPanel(
                        SwingUtilities.getWindowAncestor(parentComponent)
                    )
                    val (displaySuccess, worldSuccess) = settingsManager.saveSettings(displayOptionsPanel)

                    if (displaySuccess && worldSuccess) {
                        showNotification(parentComponent, "All settings saved successfully")
                    } else if (displaySuccess) {
                        showNotification(parentComponent, "Display settings saved successfully")
                    } else if (worldSuccess) {
                        showNotification(parentComponent, "World settings saved successfully")
                    } else {
                        JOptionPane.showMessageDialog(
                            parentComponent,
                            "Failed to save settings.",
                            "Save Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            })

            add(createMenuItem("Load Settings").apply {
                addActionListener {
                    val displayOptionsPanel = settingsManager.findDisplayOptionsPanel(
                        SwingUtilities.getWindowAncestor(parentComponent)
                    )
                    val (displaySuccess, worldSuccess) = settingsManager.loadSettings(displayOptionsPanel)

                    if (displaySuccess && worldSuccess) {
                        showNotification(parentComponent, "All settings loaded successfully")
                    } else if (displaySuccess) {
                        showNotification(parentComponent, "Display settings loaded successfully")
                    } else if (worldSuccess) {
                        showNotification(parentComponent, "World settings loaded successfully")
                    } else {
                        JOptionPane.showMessageDialog(
                            parentComponent,
                            "Failed to load settings.",
                            "Load Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            })

            add(createMenuItem("Exit",
                KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK)).apply {
                addActionListener {
                    if (confirmUnsavedChanges(parentComponent)) {
                        (SwingUtilities.getWindowAncestor(parentComponent) as? JFrame)?.dispose()
                    }
                }
            })
        }
    }

    private fun confirmUnsavedChanges(parentComponent: Component): Boolean {
        val result = JOptionPane.showConfirmDialog(
            parentComponent,
            "You may have unsaved changes. Continue?",
            "Unsaved Changes",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )
        return result == JOptionPane.YES_OPTION
    }

    // Helper to show a notification
    fun showNotification(parentComponent: Component, message: String) {
        val frame = SwingUtilities.getWindowAncestor(parentComponent) as? JFrame
        val notification = JDialog(frame, "Notification", false).apply {
            layout = BorderLayout()
            add(JLabel("  $message  ", JLabel.CENTER).apply {
                border = EmptyBorder(10, 20, 10, 20)
            }, BorderLayout.CENTER)
            pack()
            setLocationRelativeTo(frame)
            isVisible = true
        }

        // Auto-close notification after 1.5 seconds
        Timer(1500) { notification.dispose() }.apply {
            isRepeats = false
            start()
        }
    }

    fun createEditMenu(parentComponent: Component): JMenu {
        return createMenu("Edit", KeyEvent.VK_E).apply {
            add(createMenuItem("Undo",
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK)).apply {
                addActionListener {
                    // Implement undo functionality
                    // gridEditor.undo()
                    showNotification(parentComponent, "Undo operation")
                }
            })

            add(createMenuItem("Redo",
                KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK)).apply {
                addActionListener {
                    // Implement redo functionality
                    // gridEditor.redo()
                    showNotification(parentComponent, "Redo operation")
                }
            })

            addSeparator()

            add(createMenuItem("Preferences").apply {
                addActionListener {
                    // Show preferences dialog
                    JOptionPane.showMessageDialog(
                        parentComponent,
                        "Preferences dialog will be implemented in a future update.",
                        "Preferences",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
            })
        }
    }

    fun createControlsMenu(parentComponent: Component): JMenu {
        return createMenu("Controls", KeyEvent.VK_C).apply {
            add(createMenuItem("Show All Controls").apply {
                addActionListener {
                    controlsManager.showControlsDialog(parentComponent)
                }
            })

            addSeparator()

            add(createMenuItem("Configure Controls...").apply {
                addActionListener {
                    controlsManager.showControlsConfigDialog(parentComponent)
                }
            })
        }
    }

    fun createHelpMenu(parentComponent: Component): JMenu {
        return createMenu("Help", KeyEvent.VK_H).apply {
            add(createMenuItem("Documentation",
                KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)).apply {
                addActionListener {
                    // Show documentation
                    JOptionPane.showMessageDialog(
                        parentComponent,
                        "Documentation will be available in a future update.",
                        "Documentation",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
            })

            add(createMenuItem("About").apply {
                addActionListener {
                    // Show about dialog
                    JDialog(SwingUtilities.getWindowAncestor(parentComponent) as? JFrame, "About", true).apply {
                        layout = BorderLayout()
                        add(JPanel().apply {
                            layout = BorderLayout()
                            background = BACKGROUND_COLOR

                            add(JLabel("Boomer Shooter Engine", JLabel.CENTER).apply {
                                foreground = Color.WHITE
                                font = Font("Arial", Font.BOLD, 16)
                                border = EmptyBorder(10, 10, 5, 10)
                            }, BorderLayout.NORTH)

                            add(JLabel("Version 0.1", JLabel.CENTER).apply {
                                foreground = Color.WHITE
                                border = EmptyBorder(5, 10, 10, 10)
                            }, BorderLayout.CENTER)

                            add(JButton("Close").apply {
                                addActionListener { dispose() }
                            }, BorderLayout.SOUTH)
                        })

                        pack()
                        setLocationRelativeTo(parentComponent)
                        isVisible = true
                    }
                }
            })
        }
    }
}