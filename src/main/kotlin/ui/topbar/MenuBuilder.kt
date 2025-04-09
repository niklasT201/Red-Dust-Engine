package ui.topbar

import grideditor.GridEditor
import saving.SettingsManager
import ui.GameType
import ui.WelcomeScreen
import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
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
                        // Instead of just clearing the grid, show the welcome screen
                        showWelcomeScreen(parentComponent)
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
                            showNotification(parentComponent, "Failed to load world.", "Load Error", JOptionPane.ERROR_MESSAGE)
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
                        showNotification(parentComponent, "Failed to save world.", "Save Error", JOptionPane.ERROR_MESSAGE)
                    }
                }
            })

            /*
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
             */

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
                    // Get the current project path from FileManager
                    val currentProjectPath = fileManager.getProjectDirectory()?.absolutePath

                    // Find the DisplayOptionsPanel (if needed and available)
                    val displayOptionsPanel = settingsManager.findDisplayOptionsPanel(
                        SwingUtilities.getWindowAncestor(parentComponent)
                    )
                    val (displaySuccess, worldSuccess, playerSuccess) = settingsManager.saveSettings(currentProjectPath, displayOptionsPanel)

                    // Handle the results
                    when {
                        // Case 1: Everything saved successfully
                        displaySuccess && worldSuccess && playerSuccess -> {
                            showNotification(parentComponent, "All settings saved successfully")
                        }

                        // Case 2: Saving failed *and* there was no project path (likely the cause)
                        currentProjectPath == null && !displaySuccess && !worldSuccess && !playerSuccess -> {
                            JOptionPane.showMessageDialog(
                                parentComponent,
                                "Cannot save settings. No project is currently loaded or selected.",
                                "Save Error",
                                JOptionPane.ERROR_MESSAGE
                            )
                        }

                        // Case 3: Partial success or general failure with a project path
                        else -> {
                            // Build a message based on what was saved successfully
                            val successList = mutableListOf<String>()
                            if (displaySuccess) successList.add("Display")
                            if (worldSuccess) successList.add("World")
                            if (playerSuccess) successList.add("Player")

                            if (successList.isNotEmpty()) {
                                // If at least one part saved, report that
                                val successMessage = successList.joinToString(", ")
                                showNotification(parentComponent, "$successMessage settings saved successfully")
                            } else {
                                // If nothing saved despite having a project path, show general error
                                JOptionPane.showMessageDialog(
                                    parentComponent,
                                    "Failed to save settings.",
                                    "Save Error",
                                    JOptionPane.ERROR_MESSAGE
                                )
                            }
                        }
                    }
                }
            })

            add(createMenuItem("Load Settings").apply {
                addActionListener {
                    // Get the current project path from FileManager
                    val currentProjectPath = fileManager.getProjectDirectory()?.absolutePath

                    // Find the DisplayOptionsPanel (if needed and available)
                    val displayOptionsPanel = settingsManager.findDisplayOptionsPanel(
                        SwingUtilities.getWindowAncestor(parentComponent)
                    )
                    val (displaySuccess, worldSuccess, playerSuccess) = settingsManager.loadSettings(currentProjectPath, displayOptionsPanel)

                    when {
                        // Case 1: Everything loaded successfully
                        displaySuccess && worldSuccess && playerSuccess -> {
                            showNotification(parentComponent, "All settings loaded successfully")
                        }

                        // Case 2: Loading failed *and* there was no project path (likely the cause)
                        currentProjectPath == null && !displaySuccess && !worldSuccess && !playerSuccess -> {
                            JOptionPane.showMessageDialog(
                                parentComponent,
                                "Cannot load settings. No project is currently loaded or selected.",
                                "Load Error",
                                JOptionPane.ERROR_MESSAGE
                            )
                        }

                        // Case 3: Partial success or general failure with a project path
                        else -> {
                            // Build a message based on what was loaded successfully
                            val successList = mutableListOf<String>()
                            if (displaySuccess) successList.add("Display")
                            if (worldSuccess) successList.add("World")
                            if (playerSuccess) successList.add("Player")

                            if (successList.isNotEmpty()) {
                                // If at least one part loaded, report that
                                val successMessage = successList.joinToString(", ")
                                showNotification(parentComponent, "$successMessage settings loaded successfully")
                            } else {
                                // If nothing loaded despite having a project path, show general error
                                JOptionPane.showMessageDialog(
                                    parentComponent,
                                    "Failed to load settings.",
                                    "Load Error",
                                    JOptionPane.ERROR_MESSAGE
                                )
                            }
                        }
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
        val frame = SwingUtilities.getWindowAncestor(parentComponent) as? JFrame
        val optionPane = JOptionPane(
            "You may have unsaved changes. Continue?",
            JOptionPane.WARNING_MESSAGE,
            JOptionPane.YES_NO_OPTION
        )
        val dialog = optionPane.createDialog(frame, "Unsaved Changes")
        dialog.setLocationRelativeTo(frame) // Center the dialog
        dialog.isVisible = true

        return optionPane.value == JOptionPane.YES_OPTION
    }

    // Helper to show a notification
    private fun showNotification(parentComponent: Component, message: String, title: String = "Notification", messageType: Int = JOptionPane.INFORMATION_MESSAGE) {
        val frame = SwingUtilities.getWindowAncestor(parentComponent) as? JFrame
        val optionPane = JOptionPane(message, messageType)
        val dialog = optionPane.createDialog(frame, title)
        dialog.setLocationRelativeTo(frame) // This centers the dialog
        dialog.isVisible = true

        // For non-error messages, auto-close after a delay
        if (messageType != JOptionPane.ERROR_MESSAGE && messageType != JOptionPane.WARNING_MESSAGE) {
            Timer(1500) { dialog.dispose() }.apply {
                isRepeats = false
                start()
            }
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
                    val frame = SwingUtilities.getWindowAncestor(parentComponent) as? JFrame
                    val optionPane = JOptionPane(
                        "Documentation will be available in a future update.",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                    val dialog = optionPane.createDialog(frame, "Documentation")
                    dialog.setLocationRelativeTo(frame) // Center the dialog
                    dialog.isVisible = true
                }
            })

            add(createMenuItem("About").apply {
                addActionListener {
                    showAboutDialog(parentComponent)
                }
            })
        }
    }

    private fun showAboutDialog(parentComponent: Component) {
        JDialog(SwingUtilities.getWindowAncestor(parentComponent) as? JFrame, "About", true).apply {
            layout = BorderLayout()
            background = MenuBuilder.BACKGROUND_COLOR

            // Create main panel with gradient background
            add(object : JPanel() {
                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    val g2d = g as Graphics2D
                    val gradientPaint = GradientPaint(
                        0f, 0f, Color(30, 33, 40),
                        0f, height.toFloat(), Color(45, 48, 55)
                    )
                    g2d.paint = gradientPaint
                    g2d.fillRect(0, 0, width, height)
                }
            }.apply {
                layout = BorderLayout(0, 15)
                border = EmptyBorder(20, 30, 20, 30)

                // Logo/Title Panel
                add(JPanel().apply {
                    layout = BorderLayout(0, 5)
                    isOpaque = false

                    // Engine Logo/Title
                    add(JLabel("BOOMER SHOOTER ENGINE", SwingConstants.CENTER).apply {
                        foreground = Color(220, 95, 60) // Warm orange/red color typical for shooter games
                        font = Font("Impact", Font.BOLD, 22)
                        border = EmptyBorder(0, 0, 5, 0)
                    }, BorderLayout.NORTH)

                    // Stylized separator
                    add(object : JPanel() {
                        override fun paintComponent(g: Graphics) {
                            super.paintComponent(g)
                            val g2d = g as Graphics2D
                            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                            // Draw glowing line
                            val width = this.width
                            val y = this.height / 2

                            val gradient = LinearGradientPaint(
                                0f, y.toFloat(), width.toFloat(), y.toFloat(),
                                floatArrayOf(0.0f, 0.5f, 1.0f),
                                arrayOf(Color(45, 48, 55), Color(220, 95, 60), Color(45, 48, 55))
                            )

                            g2d.stroke = BasicStroke(2f)
                            g2d.paint = gradient
                            g2d.drawLine(0, y, width, y)
                        }

                        init {
                            preferredSize = Dimension(1, 10)
                        }
                    }, BorderLayout.CENTER)

                }, BorderLayout.NORTH)

                // Content Panel
                add(JPanel().apply {
                    layout = BorderLayout(0, 15)
                    isOpaque = false

                    // Version info
                    add(JPanel().apply {
                        layout = GridLayout(0, 1, 0, 8)
                        isOpaque = false

                        add(JLabel("Version 0.1 - Alpha", SwingConstants.CENTER).apply {
                            foreground = Color.WHITE
                            font = Font("Arial", Font.PLAIN, 14)
                        })

                        add(JLabel("© 2025 Red Dust Studios", SwingConstants.CENTER).apply {
                            foreground = Color(180, 180, 180)
                            font = Font("Arial", Font.PLAIN, 12)
                        })

                        add(JLabel("Built with Kotlin & Swing", SwingConstants.CENTER).apply {
                            foreground = Color(180, 180, 180)
                            font = Font("Arial", Font.ITALIC, 12)
                        })
                    }, BorderLayout.NORTH)

                    // Credits/Description (optional)
                    add(JTextArea().apply {
                        text = "A retro-inspired game engine for creating authentic 90s-style first-person shooters with modern development tools."
                        foreground = Color(200, 200, 200)
                        background = Color(35, 38, 45)
                        font = Font("SansSerif", Font.PLAIN, 12)
                        lineWrap = true
                        wrapStyleWord = true
                        isEditable = false
                        margin = Insets(10, 10, 10, 10)
                        border = BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color(25, 28, 35)),
                            BorderFactory.createEmptyBorder(8, 8, 8, 8)
                        )
                    }, BorderLayout.CENTER)

                }, BorderLayout.CENTER)

                // Button Panel
                add(JPanel().apply {
                    isOpaque = false
                    layout = FlowLayout(FlowLayout.CENTER)

                    // Custom styled close button
                    add(JButton("Close").apply {
                        foreground = Color.WHITE
                        background = Color(60, 63, 65)
                        font = Font("Arial", Font.BOLD, 12)
                        preferredSize = Dimension(100, 30)
                        isFocusPainted = false
                        border = BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color(80, 83, 85)),
                            BorderFactory.createEmptyBorder(5, 15, 5, 15)
                        )

                        // Hover effect
                        addMouseListener(object : MouseAdapter() {
                            override fun mouseEntered(e: MouseEvent) {
                                background = Color(80, 83, 85)
                            }

                            override fun mouseExited(e: MouseEvent) {
                                background = Color(60, 63, 65)
                            }
                        })

                        addActionListener { dispose() }
                    })
                }, BorderLayout.SOUTH)
            }, BorderLayout.CENTER)

            pack()
            minimumSize = Dimension(400, 350)
            setLocationRelativeTo(SwingUtilities.getWindowAncestor(parentComponent))
            isVisible = true
        }
    }

    private fun showWelcomeScreen(parentComponent: Component) {
        val frame = SwingUtilities.getWindowAncestor(parentComponent) as? JFrame ?: return

        // Clear the current grid and reset file
        gridEditor.clearGrid()
        fileManager.resetCurrentFile()

        // Create and show welcome screen dialog
        val welcomeDialog = JDialog(frame, "New Project", true)
        welcomeDialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE

        val welcomeScreen = WelcomeScreen(
            onCreateOpenWorld = {
                fileManager.setGameType(GameType.OPEN_WORLD)
                welcomeDialog.dispose()
            },
            onCreateLevelBased = {
                fileManager.setGameType(GameType.LEVEL_BASED)
                welcomeDialog.dispose()
            },
            onLoadExisting = {
                if (fileManager.loadWorld(parentComponent)) {
                    showNotification(parentComponent, "World loaded successfully")
                    welcomeDialog.dispose()
                } else {
                    showNotification(parentComponent, "Failed to load world.", "Load Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        )

        welcomeDialog.contentPane = welcomeScreen
        welcomeDialog.pack()
        welcomeDialog.minimumSize = Dimension(700, 500)
        welcomeDialog.setLocationRelativeTo(frame)
        welcomeDialog.isVisible = true
    }
}