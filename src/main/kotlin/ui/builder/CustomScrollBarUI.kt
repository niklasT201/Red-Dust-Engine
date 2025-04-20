package ui.builder

import ui.builder.UIBuilder.Companion.ACCENT_COLOR
import ui.builder.UIBuilder.Companion.BACKGROUND_COLOR_LIGHT
import ui.builder.UIBuilder.Companion.BUTTON_BG
import ui.builder.UIBuilder.Companion.TEXT_COLOR
import java.awt.*
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.plaf.basic.BasicScrollBarUI

class CustomScrollBarUI : BasicScrollBarUI() {
    private val thumbColor = ACCENT_COLOR
    private val trackColor = BACKGROUND_COLOR_LIGHT
    private val arrowButtonColor = BUTTON_BG
    private val arrowColor = TEXT_COLOR

    override fun configureScrollBarColors() {
        // These are often overridden by L&F, hence the direct painting
        // thumbColor = UIManager.getColor("ScrollBar.thumb") ?: ACCENT_COLOR
        // trackColor = UIManager.getColor("ScrollBar.track") ?: BACKGROUND_COLOR_LIGHT
        // Use companion object colors directly for consistency
    }

    override fun paintTrack(g: Graphics, c: JComponent, trackBounds: Rectangle) {
        g.color = trackColor
        g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height)
    }

    override fun paintThumb(g: Graphics, c: JComponent, thumbBounds: Rectangle) {
        if (thumbBounds.isEmpty || !scrollbar.isEnabled) {
            return
        }

        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = thumbColor
        g2.fillRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height)
    }

    // Helper to create a button with no borders, focus paint, etc.
    private fun createZeroButton(): JButton {
        return JButton().apply {
            preferredSize = Dimension(0, 0)
            minimumSize = Dimension(0, 0)
            maximumSize = Dimension(0, 0)
        }
    }

    // Override button creation to return invisible buttons, effectively removing them
    override fun createDecreaseButton(orientation: Int): JButton {
        return createZeroButton()
        // If you want visible buttons, customize them here:
        // return JButton().apply {
        //     background = arrowButtonColor
        //     // Add icon or paint arrow manually
        //     // border = BorderFactory.createLineBorder(BUTTON_BORDER) // etc.
        // }
    }

    override fun createIncreaseButton(orientation: Int): JButton {
        return createZeroButton()
        // If you want visible buttons, customize them here.
    }

    // Optional: Remove default borders
    override fun installDefaults() {
        super.installDefaults()
        scrollbar.border = BorderFactory.createEmptyBorder()
    }
}