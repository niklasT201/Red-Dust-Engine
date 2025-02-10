import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.SwingUtilities

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("3D Engine")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        val game = Game3D()
        val editorUI = EditorUI(game)

        frame.jMenuBar = editorUI.createMenuBar()
        frame.add(game, BorderLayout.CENTER)
        frame.add(editorUI.sideBar, BorderLayout.WEST)  // Changed from EAST to WEST

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