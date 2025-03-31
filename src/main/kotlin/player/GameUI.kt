package player

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.BasicStroke
import java.awt.GradientPaint
import java.awt.Polygon
import java.awt.image.BufferedImage
import java.awt.RenderingHints

/**
 * A UI component that renders a classic FPS status bar at the bottom of the screen
 * inspired by old-school shooters like Wolfenstein 3D and Doom.
 */
class GameUI {
    // UI color constants - using more authentic color palette
    private val statusBarBgColor = Color(48, 48, 48) // Dark gray background like Doom
    private val statusBarTextColor = Color(215, 186, 69) // Gold text like Doom
    private val statusBarBorderColor = Color(132, 132, 132) // Metal border
    private val healthColor = Color(215, 0, 0) // Red for health
    private val ammoColor = Color(0, 96, 176) // Blue for ammo

    // UI size constants - increased height
    private val statusBarHeight = 120 // Much taller status bar for better visibility
    private val statusBarPadding = 10
    private val faceSize = 64 // Larger face

    // Fonts - pixel-like fonts to match the era
    private val digitFont = Font("Courier New", Font.BOLD, 28)
    private val labelFont = Font("Courier New", Font.BOLD, 14)

    // Pre-rendered face variations
    private var faceFront: BufferedImage? = null

    init {
        // Initialize face graphics
        createFaceGraphics()
    }

    private fun createFaceGraphics() {
        // Create a simple face image (would be replaced with actual sprites in a real game)
        faceFront = BufferedImage(faceSize, faceSize, BufferedImage.TYPE_INT_ARGB)
        val g = faceFront!!.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Face background
        g.color = Color(231, 181, 131) // Skin tone
        g.fillOval(2, 2, faceSize-4, faceSize-4)

        // Black border
        g.color = Color.BLACK
        g.drawOval(2, 2, faceSize-4, faceSize-4)

        // Eyes
        g.color = Color.WHITE
        g.fillOval(16, 20, 12, 10)
        g.fillOval(36, 20, 12, 10)

        g.color = Color(30, 50, 210) // Blue eyes
        g.fillOval(18, 21, 8, 8)
        g.fillOval(38, 21, 8, 8)

        // Mouth
        g.color = Color(180, 50, 50) // Reddish mouth
        g.fillArc(22, 38, 20, 14, 0, 180)

        g.dispose()
    }

    fun render(g2: Graphics2D, width: Int, height: Int) {
        // Save original settings to restore later
        val originalStroke = g2.stroke
        val originalFont = g2.font

        // Create and render the status bar rectangle at the bottom of the screen
        val statusBarRect = Rectangle(0, height - statusBarHeight, width, statusBarHeight)

        // Dark background with slight gradient like Doom's UI
        val bgGradient = GradientPaint(
            0f, height - statusBarHeight.toFloat(),
            Color(55, 55, 55),
            0f, height.toFloat(),
            Color(30, 30, 30)
        )
        g2.paint = bgGradient
        g2.fill(statusBarRect)

        // Add metallic border for that industrial look
        g2.color = statusBarBorderColor
        g2.stroke = BasicStroke(2f)
        g2.drawLine(0, height - statusBarHeight, width, height - statusBarHeight)

        // Calculate panel widths to ensure proper spacing
        val panelWidth = 210
        val panelHeight = statusBarHeight - 20
        val panelY = height - statusBarHeight + 10

        // Left panel for HEALTH
        val healthPanelX = 15
        drawDoomStylePanel(g2, healthPanelX, panelY, panelWidth, panelHeight)

        // Face panel in center with proper padding to avoid collision
        val facePanelWidth = 170
        val facePanelX = width / 2 - facePanelWidth / 2 // Center the wider panel
        drawDoomStylePanel(g2, facePanelX, panelY, facePanelWidth, panelHeight)

        val statsX = facePanelX + 10 // Padding from left edge of face panel
        val statsY = panelY + 18     // Near top of panel
        g2.font = labelFont
        g2.color = statusBarTextColor
        g2.drawString("K: 14/45", statsX, statsY)
        g2.drawString("I: 3/12", statsX, statsY + 16) // Position below K
        g2.drawString("S: 1/5", statsX, statsY + 32) // Position below I

        // Draw the face in its container
        if (faceFront != null) {
            // Calculate X position relative to panel, leaving space for stats on the left
            val faceX = facePanelX + facePanelWidth - faceSize - 10 // Align to right within panel, with padding
            val faceY = panelY + (panelHeight - faceSize) / 2 // Vertically center face in panel
            g2.drawImage(faceFront, faceX, faceY, null)
        }

        val armorY = statsY + 32 + 18 // Position below S stat, with some spacing
        g2.drawString("ARMOR 75%", statsX, armorY)

        // Right panel for AMMO
        val ammoPanelX = width - panelWidth - 15 // Position remains the same relative to screen edge
        drawDoomStylePanel(g2, ammoPanelX, panelY, panelWidth, panelHeight)

        drawStatusWithBar(g2, "HEALTH", "100", healthColor, healthPanelX + 10,
            panelY + 20, panelWidth - 20, 36)

        // Draw AMMO section with bar and weapon number
        drawStatusWithBar(g2, "AMMO", "48", ammoColor, ammoPanelX + 10,
            panelY + 20, panelWidth - 20, 36)

        // Draw weapon selector with number (Doom style weapon selector)
        drawWeaponSelector(g2, 2, width - 80, panelY)

        // Restore original settings
        g2.stroke = originalStroke
        g2.font = originalFont
    }

    private fun drawDoomStylePanel(g2: Graphics2D, x: Int, y: Int, width: Int, height: Int) {
        // Draw panel background (dark)
        g2.color = Color(30, 30, 30)
        g2.fillRect(x, y, width, height)

        // Draw embossed edges (classic Doom UI style)
        g2.color = Color(80, 80, 80) // Light edge
        g2.drawLine(x, y, x + width, y) // Top
        g2.drawLine(x, y, x, y + height) // Left

        g2.color = Color(20, 20, 20) // Dark edge
        g2.drawLine(x, y + height, x + width, y + height) // Bottom
        g2.drawLine(x + width, y, x + width, y + height) // Right
    }

    private fun drawStatusWithBar(g2: Graphics2D, label: String, value: String, barColor: Color,
                                  x: Int, y: Int, width: Int, height: Int) {
        // Draw label
        g2.font = labelFont
        g2.color = statusBarTextColor
        g2.drawString(label, x, y)

        // Draw value with larger font
        g2.font = digitFont
        g2.drawString(value, x, y + 26)

        // Draw bar background
        g2.color = Color(15, 15, 15)
        g2.fillRect(x, y + 30, width, 16)

        // Draw colored bar based on value (assuming value is between 0-100)
        val valueInt = value.replace("%", "").toIntOrNull() ?: 100
        val barWidth = (width * valueInt / 100.0).toInt()
        g2.color = barColor
        g2.fillRect(x, y + 30, barWidth, 16)

        // Draw notches on bar (Doom style)
        g2.color = Color(0, 0, 0)
        for (i in 1..9) {
            val notchX = x + (width * i / 10)
            g2.drawLine(notchX, y + 30, notchX, y + 46)
        }
    }

    private fun drawWeaponSelector(g2: Graphics2D, currentWeapon: Int, x: Int, y: Int) {
        // Draw selector background
        drawDoomStylePanel(g2, x, y, 60, 50)

        // Draw weapon number
        g2.font = Font("Courier New", Font.BOLD, 36)
        g2.color = statusBarTextColor
        g2.drawString("$currentWeapon", x + 22, y + 36)

        // Draw small triangles indicating available weapons (Doom style)
        g2.color = Color(180, 180, 180)
        for (i in 1..5) {
            if (i == currentWeapon) {
                g2.color = Color(255, 255, 0) // Highlight selected
            } else {
                g2.color = Color(180, 180, 180)
            }

            // Small triangle indicator
            val triangle = Polygon()
            val triangleX = x + 10 + (i-1) * 9
            triangle.addPoint(triangleX, y - 4)
            triangle.addPoint(triangleX + 5, y - 10)
            triangle.addPoint(triangleX + 10, y - 4)
            g2.fillPolygon(triangle)
        }
    }
}