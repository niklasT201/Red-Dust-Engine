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
// default value saving
// adding physics
// fixing image sliding
// optional border around objects
// disable keys option
// player drawing in cell fix
// sky panel design improvement
// sky image duplication bug fix
// sky image having its one folder, assets/textures/sky_image/

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