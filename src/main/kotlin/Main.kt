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
// add settings file to save values
// default value saving
// adding physics
// fixing image sliding
// optional border around objects
// checkbox for all labels on grid editor

// Object List:
// Pillar, breakable walls and floors, PropObject, Doors, Triggers, Elevator, Light Sources, Items, 3D Object Support

// optional shadows
// weapon system
// create own weapons
// damage system
// all objects in editor menu

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

// simple physics system
// support for doors and animated walls
// support for transparent textures
// simple particle system

/*
can you help me with my kotlin boomer shooter engine? i have this saving system that saves objects and their values. now i want to also add saving for the editorpanel bc their are/will be things to change values like player height, gravity, if gravity is used, sky color etc. so could you check my files and look what is already there and only add saving for this values? please just add it to the save feature in my menu system and not creating a new button or something like this. save only things that are needed, so for example in my editorpanel is a wall properties section where you can select the wall color, this is already been saved so only save things that are beyond objects itself, like for example how many floor levels my engine has. what i can see, that needs to be saved, is the labels/checkbox for the display class and the show walls as line checkbox, so you dont have to change it for your taste always again. please create when possible a new file/class for the settings management
 */