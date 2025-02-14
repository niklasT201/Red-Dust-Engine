import javax.swing.JFrame
import javax.swing.SwingUtilities

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("3D Engine")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        val game = Game3D()
        frame.add(game)
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true

        Thread {
            while (true) {
                game.update()
                Thread.sleep(16)
            }
        }.start()
    }
}

// walls rotation fixing
// wall clipping issue

// add shortcuts for objects
// optional shadows
// weapon system
// create own weapons
// damage system
// all objects in editor menu

// image support
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