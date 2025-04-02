package ui

import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import ui.topbar.MenuBuilder
import java.io.File

class WelcomeScreen(
    private val onCreateOpenWorld: () -> Unit,
    private val onCreateLevelBased: () -> Unit,
    private val onLoadExisting: () -> Unit
) : JPanel() {

    init {
        layout = BorderLayout()
        background = MenuBuilder.BACKGROUND_COLOR
        border = EmptyBorder(30, 30, 30, 30)

        // Title panel
        add(createTitlePanel(), BorderLayout.NORTH)

        // Main content panel with options
        add(createContentPanel(), BorderLayout.CENTER)

        // Check if any worlds or levels exist
        checkExistingWorlds()
    }

    private fun createTitlePanel(): JPanel {
        return JPanel().apply {
            layout = BorderLayout(0, 15)
            isOpaque = false

            // Title label
            add(JLabel("BOOMER SHOOTER ENGINE", SwingConstants.CENTER).apply {
                foreground = Color(220, 95, 60)
                font = Font("Impact", Font.BOLD, 36)
            }, BorderLayout.NORTH)

            // Subtitle
            add(JLabel("World Builder", SwingConstants.CENTER).apply {
                foreground = Color.WHITE
                font = Font("Arial", Font.BOLD, 18)
            }, BorderLayout.CENTER)

            // Separator
            add(object : JPanel() {
                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    val g2d = g as Graphics2D
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

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
                    preferredSize = Dimension(1, 15)
                }
            }, BorderLayout.SOUTH)
        }
    }

    private fun createContentPanel(): JPanel {
        return JPanel().apply {
            layout = GridLayout(1, 2, 20, 0)
            isOpaque = false
            border = EmptyBorder(20, 0, 10, 0)

            // Left panel - New Project Options
            add(createNewProjectPanel())

            // Right panel - Recent Projects
            add(createRecentProjectsPanel())
        }
    }

    private fun createNewProjectPanel(): JPanel {
        return JPanel().apply {
            layout = BorderLayout(0, 15)
            background = MenuBuilder.BORDER_COLOR
            border = EmptyBorder(15, 15, 15, 15)

            // Title
            add(JLabel("NEW PROJECT", SwingConstants.CENTER).apply {
                foreground = Color.WHITE
                font = Font("Arial", Font.BOLD, 18)
            }, BorderLayout.NORTH)

            // Options panel
            add(JPanel().apply {
                layout = GridLayout(2, 1, 0, 15)
                isOpaque = false

                // Open World Option
                add(createOptionButton(
                    "OPEN WORLD",
                    "Create a single large world",
                    Color(50, 120, 200),
                    onCreateOpenWorld
                ))

                // Level Based Option
                add(createOptionButton(
                    "LEVEL BASED",
                    "Create multiple separate levels",
                    Color(200, 100, 50),
                    onCreateLevelBased
                ))

            }, BorderLayout.CENTER)
        }
    }

    private fun createRecentProjectsPanel(): JPanel {
        return JPanel().apply {
            layout = BorderLayout(0, 15)
            background = MenuBuilder.BORDER_COLOR
            border = EmptyBorder(15, 15, 15, 15)

            // Title
            add(JLabel("LOAD EXISTING", SwingConstants.CENTER).apply {
                foreground = Color.WHITE
                font = Font("Arial", Font.BOLD, 18)
            }, BorderLayout.NORTH)

            // Recent projects list or Load button
            add(JScrollPane(JList<String>().apply {
                model = DefaultListModel<String>().apply {
                    // Will be populated in checkExistingWorlds()
                }
                selectionMode = ListSelectionModel.SINGLE_SELECTION
                background = MenuBuilder.BACKGROUND_COLOR
                foreground = Color.WHITE
                font = Font("SansSerif", Font.PLAIN, 14)
                fixedCellHeight = 30
                border = EmptyBorder(5, 5, 5, 5)
            }).apply {
                border = null
                background = MenuBuilder.BORDER_COLOR
            }, BorderLayout.CENTER)

            // Load button at the bottom
            add(JButton("Browse Saved Worlds").apply {
                font = Font("Arial", Font.BOLD, 14)
                foreground = Color.WHITE
                background = Color(60, 63, 65)
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color(80, 83, 85)),
                    BorderFactory.createEmptyBorder(8, 15, 8, 15)
                )
                isFocusPainted = false

                addActionListener {
                    onLoadExisting()
                }

                addMouseListener(object : java.awt.event.MouseAdapter() {
                    override fun mouseEntered(e: java.awt.event.MouseEvent) {
                        background = Color(80, 83, 85)
                    }

                    override fun mouseExited(e: java.awt.event.MouseEvent) {
                        background = Color(60, 63, 65)
                    }
                })
            }, BorderLayout.SOUTH)
        }
    }

    private fun createOptionButton(title: String, description: String, accentColor: Color, onClick: () -> Unit): JPanel {
        return JPanel().apply {
            layout = BorderLayout(10, 0)
            background = MenuBuilder.BACKGROUND_COLOR
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor, 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
            )

            // Left panel with accent color
            add(JPanel().apply {
                preferredSize = Dimension(5, 0)
                background = accentColor
            }, BorderLayout.WEST)

            // Center panel with title and description
            add(JPanel().apply {
                layout = BorderLayout(0, 5)
                isOpaque = false

                add(JLabel(title).apply {
                    foreground = Color.WHITE
                    font = Font("Arial", Font.BOLD, 16)
                }, BorderLayout.NORTH)

                add(JLabel("<html><body width='250'>$description</body></html>").apply {
                    foreground = Color(200, 200, 200)
                    font = Font("SansSerif", Font.PLAIN, 14)
                }, BorderLayout.CENTER)

                // Add a small icon indicating it's clickable
                add(JPanel().apply {
                    layout = BorderLayout()
                    isOpaque = false
                    border = EmptyBorder(5, 0, 0, 0)

                    add(JLabel("Click to select", SwingConstants.LEFT).apply {
                        foreground = Color(150, 150, 150)
                        font = Font("SansSerif", Font.ITALIC, 12)
                    }, BorderLayout.WEST)
                }, BorderLayout.SOUTH)

            }, BorderLayout.CENTER)

            // Right arrow panel with glow effect
            add(object : JPanel() {
                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    val g2d = g as Graphics2D
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                    // Draw glowing arrow
                    g2d.color = accentColor
                    val arrowSize = 12
                    val x = width / 2
                    val y = height / 2

                    val xPoints = intArrayOf(x - arrowSize, x, x - arrowSize)
                    val yPoints = intArrayOf(y - arrowSize, y, y + arrowSize)

                    // Draw arrow
                    g2d.fillPolygon(xPoints, yPoints, 3)
                }

                init {
                    preferredSize = Dimension(30, 0)
                    isOpaque = false
                }
            }, BorderLayout.EAST)

            // Make the panel clickable with improved hover effect
            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseEntered(e: java.awt.event.MouseEvent) {
                    background = Color(50, 53, 60)
                    cursor = Cursor(Cursor.HAND_CURSOR)
                    border = BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(accentColor.brighter(), 2),
                        BorderFactory.createEmptyBorder(15, 15, 15, 15)
                    )
                }

                override fun mouseExited(e: java.awt.event.MouseEvent) {
                    background = MenuBuilder.BACKGROUND_COLOR
                    cursor = Cursor(Cursor.DEFAULT_CURSOR)
                    border = BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(accentColor, 2),
                        BorderFactory.createEmptyBorder(15, 15, 15, 15)
                    )
                }

                override fun mousePressed(e: java.awt.event.MouseEvent) {
                    background = Color(40, 43, 50)
                }

                override fun mouseReleased(e: java.awt.event.MouseEvent) {
                    background = Color(50, 53, 60)
                }

                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    onClick()
                }
            })
        }
    }

    private fun checkExistingWorlds() {
        // Create directories if they don't exist
        createDirectoryStructure()

        // TODO: Populate the recent projects list based on existing worlds/levels
        // This will be implemented when the FileManager is updated
    }

    private fun createDirectoryStructure() {
        val worldDir = File("World")
        if (!worldDir.exists()) worldDir.mkdir()

        val savesDir = File(worldDir, "saves")
        if (!savesDir.exists()) savesDir.mkdir()

        val openWorldDir = File(savesDir, "open_world")
        if (!openWorldDir.exists()) openWorldDir.mkdir()

        val levelsDir = File(savesDir, "levels")
        if (!levelsDir.exists()) levelsDir.mkdir()

        val quicksavesDir = File(worldDir, "quicksaves")
        if (!quicksavesDir.exists()) quicksavesDir.mkdir()
    }
}