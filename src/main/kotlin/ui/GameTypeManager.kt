package ui

import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import ui.topbar.MenuBuilder
import java.io.Serializable

// Enum to represent different game types
enum class GameType : Serializable {
    LEVEL_BASED,
    OPEN_WORLD
}

class GameTypeManager(private val onGameTypeSelected: (GameType) -> Unit) {
    // The currently selected game type
    private var currentGameType: GameType? = null

    fun getCurrentGameType(): GameType? = currentGameType

    fun setGameType(gameType: GameType) {
        currentGameType = gameType
        onGameTypeSelected(gameType)
    }

    fun showGameTypeSelector(parent: Component) {
        val dialog = GameTypeSelector(this)
        dialog.setLocationRelativeTo(parent)
        dialog.isVisible = true
    }

    // Check if we're in level-based mode
    fun isLevelBased(): Boolean = currentGameType == GameType.LEVEL_BASED

    // Check if we're in open world mode
    fun isOpenWorld(): Boolean = currentGameType == GameType.OPEN_WORLD
}

class GameTypeSelector(private val gameTypeManager: GameTypeManager) : JDialog() {

    init {
        title = "Select Game Type"
        modalityType = Dialog.ModalityType.APPLICATION_MODAL
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE

        val mainPanel = JPanel().apply {
            layout = BorderLayout()
            background = MenuBuilder.BACKGROUND_COLOR
            border = EmptyBorder(20, 20, 20, 20)
        }

        // Add title
        mainPanel.add(createTitlePanel(), BorderLayout.NORTH)

        // Add selection panel
        mainPanel.add(createSelectionPanel(), BorderLayout.CENTER)

        contentPane = mainPanel
        pack()
        minimumSize = Dimension(450, 350)
    }

    private fun createTitlePanel(): JPanel {
        return JPanel().apply {
            layout = BorderLayout()
            isOpaque = false

            add(JLabel("SELECT GAME TYPE", SwingConstants.CENTER).apply {
                foreground = Color(220, 95, 60)
                font = Font("Impact", Font.BOLD, 24)
                border = EmptyBorder(0, 0, 15, 0)
            }, BorderLayout.CENTER)

            // Add a separator
            add(JSeparator().apply {
                foreground = Color(80, 83, 85)
                background = MenuBuilder.BACKGROUND_COLOR
            }, BorderLayout.SOUTH)
        }
    }

    private fun createSelectionPanel(): JPanel {
        val panel = JPanel().apply {
            layout = GridLayout(1, 2, 15, 0)
            isOpaque = false
            border = EmptyBorder(20, 10, 10, 10)
        }

        // Level-based option
        panel.add(createGameTypeCard(
            "LEVEL BASED",
            "Create multiple separate maps that can be loaded individually. Ideal for traditional FPS games with distinct levels.",
            GameType.LEVEL_BASED
        ))

        // Open world option
        panel.add(createGameTypeCard(
            "OPEN WORLD",
            "Create a single large world where everything exists in one map. Ideal for exploration-focused games.",
            GameType.OPEN_WORLD
        ))

        return panel
    }

    private fun createGameTypeCard(title: String, description: String, gameType: GameType): JPanel {
        return JPanel().apply {
            layout = BorderLayout(0, 10)
            background = MenuBuilder.BORDER_COLOR
            border = EmptyBorder(15, 15, 15, 15)

            // Title
            add(JLabel(title, SwingConstants.CENTER).apply {
                foreground = Color.WHITE
                font = Font("Arial", Font.BOLD, 18)
            }, BorderLayout.NORTH)

            // Description
            add(JTextArea().apply {
                text = description
                lineWrap = true
                wrapStyleWord = true
                background = MenuBuilder.BORDER_COLOR
                foreground = Color(200, 200, 200)
                font = Font("SansSerif", Font.PLAIN, 14)
                isEditable = false
                margin = Insets(10, 5, 10, 5)
            }, BorderLayout.CENTER)

            // Button
            add(JButton("Select").apply {
                font = Font("Arial", Font.BOLD, 14)
                foreground = Color.WHITE
                background = Color(60, 63, 65)
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color(80, 83, 85)),
                    BorderFactory.createEmptyBorder(8, 15, 8, 15)
                )
                isFocusPainted = false

                addActionListener {
                    gameTypeManager.setGameType(gameType)
                    dispose()
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
}