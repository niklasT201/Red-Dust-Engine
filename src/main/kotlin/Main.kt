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

// fix visible floor tile border
// default value saving
// adding physics
// fixing image sliding
// optional border around objects
// disable keys option
// player drawing in cell fix
// fps disabler

// Object List:
// Pillar, breakable walls and floors, PropObject, Doors, Triggers, Elevator, Light Sources, Items, 3D Object Support

// optional shadows
// weapon system
// create own weapons
// damage system
// all objects in editor menu

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

// simple physics system
// support for doors and animated walls
// support for transparent textures
// simple particle system