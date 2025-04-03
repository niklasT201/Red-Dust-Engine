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

            // Animated title label
            add(object : JPanel() {
                private var animationOffset = 0.0
                private val timer = Timer(50) {
                    animationOffset += 0.1
                    if (animationOffset > Math.PI * 2) {
                        animationOffset = 0.0
                    }
                    repaint()
                }

                init {
                    isOpaque = false
                    preferredSize = Dimension(0, 50)
                    timer.start()
                }

                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    val g2d = g as Graphics2D
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

                    val title = "BOOMER SHOOTER ENGINE"
                    val font = Font("Impact", Font.BOLD, 36)
                    g2d.font = font

                    val metrics = g2d.fontMetrics
                    val width = metrics.stringWidth(title)
                    val x = (this.width - width) / 2
                    val y = this.height / 2 + metrics.height / 3

                    // Draw shadow
                    g2d.color = Color(100, 30, 20, 150)
                    g2d.drawString(title, x + 2, y + 2)

                    // Create a gradient that shifts with the animation
                    val baseColor = Color(220, 95, 60)
                    val endColor = Color(255, 160, 80)

                    val gradient = LinearGradientPaint(
                        x.toFloat(), (y - metrics.height).toFloat(),
                        (x + width).toFloat(), y.toFloat(),
                        floatArrayOf(0.0f, 0.5f, 1.0f),
                        arrayOf(
                            baseColor,
                            Color(
                                Math.min(255, (endColor.red * (1 + 0.2 * Math.sin(animationOffset))).toInt()),
                                Math.min(255, (endColor.green * (1 + 0.2 * Math.sin(animationOffset))).toInt()),
                                Math.min(255, (endColor.blue * (1 + 0.1 * Math.sin(animationOffset))).toInt())
                            ),
                            baseColor
                        )
                    )

                    g2d.paint = gradient
                    g2d.drawString(title, x, y)
                }

                override fun removeNotify() {
                    super.removeNotify()
                    timer.stop()
                }
            }, BorderLayout.NORTH)

            // Subtitle with glowing effect
            add(JLabel("World Builder", SwingConstants.CENTER).apply {
                foreground = Color.WHITE
                font = Font("Arial", Font.BOLD, 18)
            }, BorderLayout.CENTER)

            // Separator with animated glow
            add(object : JPanel() {
                private var glowPosition = 0.0
                private val timer = Timer(30) {
                    glowPosition += 0.02
                    if (glowPosition > 1.0) {
                        glowPosition = 0.0
                    }
                    repaint()
                }

                init {
                    preferredSize = Dimension(1, 15)
                    isOpaque = false
                    timer.start()
                }

                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    val g2d = g as Graphics2D
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                    val width = this.width
                    val y = this.height / 2

                    // Static gradient line
                    val baseGradient = LinearGradientPaint(
                        0f, y.toFloat(), width.toFloat(), y.toFloat(),
                        floatArrayOf(0.0f, 0.5f, 1.0f),
                        arrayOf(Color(45, 48, 55), Color(220, 95, 60), Color(45, 48, 55))
                    )

                    g2d.stroke = BasicStroke(2f)
                    g2d.paint = baseGradient
                    g2d.drawLine(0, y, width, y)

                    // Animated glow effect
                    val glowX = (width * glowPosition).toInt()
                    val glowRadius = 50

                    val radialGradient = RadialGradientPaint(
                        glowX.toFloat(), y.toFloat(), glowRadius.toFloat(),
                        floatArrayOf(0.0f, 0.5f, 1.0f),
                        arrayOf(
                            Color(255, 200, 100, 180),
                            Color(220, 95, 60, 100),
                            Color(220, 95, 60, 0)
                        )
                    )

                    g2d.paint = radialGradient
                    g2d.fillOval(glowX - glowRadius, y - glowRadius, glowRadius * 2, glowRadius * 2)
                }

                override fun removeNotify() {
                    super.removeNotify()
                    timer.stop()
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

/*
can you help me with my kotlin boomer shooter engine? I have this welcome screen and want to change something. as you see there i can create new projects or load ones. can you maybe change it so, that when i create a new open world project, then i have to also choose a name for the project, so that there will be saves also my settings, quicksaves and saves, and when i create a new level it lets me chooose if i want to create a new project or add a new level to an already existing project as open world means the project has only one map and level based it has multiple ones. so i basically need a way to also add name for the project, that will then use the name to create a directory where everything will be saved. do you get what i mean? here is a snippet of my settings saver class if you need it? you dont need to show me the whole files again when its bigger like the welcome screen. when you want to show me the whole file then do it only when the whole files has only like 150 lines, then its fine but not when higher.
 */