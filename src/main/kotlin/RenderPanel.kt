import player.GameUI
import ui.components.CrosshairShape
import java.awt.*
import javax.swing.JPanel

class RenderPanel(
    private val game3D: Game3D,
    private val renderer: Renderer,
    private val player: player.Player,
    private val walls: List<Wall>,
    private val floors: List<Floor>,
    private val waters: List<WaterSurface>
) : JPanel() {

    // Properties moved from Game3D
    private var isCrosshairVisible = true
    private var crosshairSize = 10
    private var crosshairColor = Color.WHITE
    private var crosshairShape = CrosshairShape.PLUS
    private var isFpsCounterVisible = true
    private var isDirectionVisible = true
    private var isPositionVisible = true

    // Add the GameUI component
    private val gameUI = GameUI()
    private var isGameUIVisible = true

    init {
        preferredSize = Dimension(800, 600)
        background = Color(135, 206, 235)
        isFocusable = true
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Use the sky color property instead of hardcoded value
        val currentSkyRenderer = game3D.getSkyRenderer()
        currentSkyRenderer.render(g2, width, height)

        renderer.drawScene(g2, walls, floors, waters, player.camera)

        if (!game3D.isEditorMode) {
            // Draw crosshair only if it's visible
            if (isCrosshairVisible) {
                g2.color = crosshairColor

                when (crosshairShape) {
                    CrosshairShape.PLUS -> {
                        g2.drawLine(width/2 - crosshairSize, height/2, width/2 + crosshairSize, height/2)
                        g2.drawLine(width/2, height/2 - crosshairSize, width/2, height/2 + crosshairSize)
                    }
                    CrosshairShape.X -> {
                        g2.drawLine(width/2 - crosshairSize, height/2 - crosshairSize, width/2 + crosshairSize, height/2 + crosshairSize)
                        g2.drawLine(width/2 - crosshairSize, height/2 + crosshairSize, width/2 + crosshairSize, height/2 - crosshairSize)
                    }
                    CrosshairShape.DOT -> {
                        val dotSize = crosshairSize / 3
                        g2.fillOval(width/2 - dotSize, height/2 - dotSize, dotSize * 2, dotSize * 2)
                    }
                    CrosshairShape.CIRCLE -> {
                        g2.drawOval(width/2 - crosshairSize, height/2 - crosshairSize, crosshairSize * 2, crosshairSize * 2)
                    }
                }
            }

            g2.font = Font("Monospace", Font.BOLD, 14)
            g2.color = Color.WHITE

            // Draw FPS counter independently of other debug info
            if (isFpsCounterVisible) {
                g2.drawString("FPS: ${game3D.currentFps}", 10, 20)
            }

            // Conditionally draw debug information
            // Get cardinal direction from player
            val direction = player.getCardinalDirection()

            // Get angles in degrees for display
            val yawDegrees = player.getYawDegrees()

            // Draw debug information
            // Adjust y-position based on whether FPS is also being shown
            val startY = if (isFpsCounterVisible) 40 else 20
            var currentY = startY
            if (isDirectionVisible) {
                g2.drawString("Direction: $direction (${yawDegrees}°)", 10, currentY)
                currentY += 20
            }
            if (isPositionVisible) {
                g2.drawString("Position: (${String.format("%.1f", player.position.x)}, ${String.format("%.1f", player.position.y)}, ${String.format("%.1f", player.position.z)})", 10, currentY)
            }

            // Draw game UI if it's visible
            if (isGameUIVisible) {
                gameUI.render(g2, width, height)
            }
        }
    }

    // Getters and setters for the properties

    fun isCrosshairVisible(): Boolean = isCrosshairVisible

    fun setCrosshairVisible(visible: Boolean) {
        isCrosshairVisible = visible
        repaint()  // Refresh the display when changed
    }

    fun getCrosshairSize(): Int = crosshairSize

    fun setCrosshairSize(size: Int) {
        crosshairSize = size
        repaint()  // Refresh the display when changed
    }

    fun getCrosshairColor(): Color = crosshairColor?: Color.WHITE

    fun setCrosshairColor(color: Color) {
        crosshairColor = color
        repaint()  // Refresh the display when changed
    }

    fun getCrosshairShape(): CrosshairShape = crosshairShape

    fun setCrosshairShape(shape: CrosshairShape) {
        crosshairShape = shape
        repaint()  // Refresh the display when changed
    }

    fun isDirectionVisible(): Boolean = isDirectionVisible

    fun setDirectionVisible(visible: Boolean) {
        isDirectionVisible = visible
        repaint()
    }

    fun isPositionVisible(): Boolean = isPositionVisible

    fun setPositionVisible(visible: Boolean) {
        isPositionVisible = visible
        repaint()
    }

    fun isFpsCounterVisible(): Boolean = isFpsCounterVisible

    fun setFpsCounterVisible(visible: Boolean) {
        isFpsCounterVisible = visible
        repaint()
    }

    fun isGameUIVisible(): Boolean = isGameUIVisible

    fun setGameUIVisible(visible: Boolean) {
        isGameUIVisible = visible
        repaint()
    }
}