import javax.swing.JFrame
import javax.swing.SwingUtilities
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

fun main() {
    // Flag to control the game loop
    val isRunning = AtomicBoolean(true)

    SwingUtilities.invokeLater {
        val frame = JFrame("3D Engine")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        val game = Game3D()
        frame.add(game)
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true

        // Add window listener to properly shutdown the application
        frame.addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(e: java.awt.event.WindowEvent) {
                isRunning.set(false) // Signal the game loop to stop
                exitProcess(0) // Ensure complete application shutdown
            }
        })

        Thread {
            while (isRunning.get()) {
                game.update()
                Thread.sleep(16)

                // Check if frame is still visible
                if (!frame.isVisible) {
                    isRunning.set(false)
                }
            }
        }.start()
    }
}

// improve general ui design
// fix visible floor tile border
// fixing image sliding
// optional border adding for single objects and not only all ones
// add shader support
// add way to build the game
// player moving a bit when in water
// add checkExistingWorlds feature, double press to load file
// restructure saving in one project


// Object List:
// breakable walls and floors, PropObject, Doors, Triggers, Elevator, Light Sources, Items, 3D Object Support

// weapon system
// render screen with sniper like view
// create own weapons
// damage system

// UI:
// blood options

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