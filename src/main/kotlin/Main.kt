import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("3D Game Editor")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        val game = Game3D()
        val gridEditor = GridEditor(800, 600)
        val editorUI = EditorUI(game)

        // Set up the bidirectional references
        gridEditor.setGame(game)

        val contentPane = JPanel(CardLayout())
        contentPane.add(game, "game")
        contentPane.add(gridEditor, "editor")

        // Set up the components
        game.setComponents(contentPane, editorUI, gridEditor)

        // Initially hide the sidebar
        editorUI.sideBar.isVisible = false

        frame.jMenuBar = editorUI.createMenuBar()
        frame.add(contentPane, BorderLayout.CENTER)
        frame.add(editorUI.sideBar, BorderLayout.WEST)

        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
        game.requestFocusInWindow()

        // Game update loop
        Thread {
            while (true) {
                game.update()
                Thread.sleep(16)
            }
        }.start()
    }
}

// image floor laufband
// FPS show
// remove tiles dragging adding

// UI:
// Sectiom seperation
// physics optional
// blood options
// player spawnpoint optional
// open world or level based
// floor sections

// Screen:
// Settings Menu
// Credits
// Loading Screen
// start Menu

// performance optimization:
// frustum culling
// distance-based culling
// Cache calculations
// spatial partitioning system (like BSP or quadtree)

// support for different wall heights
// basic lighting system
// support for ceiling textures
// sprite rendering for 2D objects
// simple shading based on wall orientation
// texture mipmapping for better quality at distances

// support for sectors with different floor/ceiling heights
// simple physics system
// support for doors and animated walls
// support for transparent textures
// simple particle system