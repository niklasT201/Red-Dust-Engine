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

// wall clipping issue
// maybe fix player map offset 1
// pressing gui, wall direction isnt working
// fix visible floor tile border
// objects arent straight when bigger screen bc of mouse movement
// changing texture manager preview
// fix collisions for floor levels

// optional shadows
// weapon system
// create own weapons
// damage system
// all objects in editor menu

// image support
// direction label optional
// FPS show

// UI:
// physics optional
// blood options
// open world or level based

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