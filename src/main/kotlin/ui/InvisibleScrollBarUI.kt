package ui

import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Rectangle
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.plaf.basic.BasicScrollBarUI

class InvisibleScrollBarUI : BasicScrollBarUI() {
    override fun configureScrollBarColors() {
        thumbColor = Color(0, 0, 0, 0)
        trackColor = Color(0, 0, 0, 0)
    }

    override fun paintThumb(g: Graphics, c: JComponent, thumbBounds: Rectangle) {
        // Don't paint anything
    }

    override fun paintTrack(g: Graphics, c: JComponent, trackBounds: Rectangle) {
        // Don't paint anything
    }

    override fun paintDecreaseHighlight(g: Graphics) {
        // Don't paint anything
    }

    override fun paintIncreaseHighlight(g: Graphics) {
        // Don't paint anything
    }

    // Make the buttons invisible
    override fun createDecreaseButton(orientation: Int): JButton {
        return JButton().apply { preferredSize = Dimension(0, 0) }
    }

    override fun createIncreaseButton(orientation: Int): JButton {
        return JButton().apply { preferredSize = Dimension(0, 0) }
    }
}