package ui.builder

import Game3D
import player.uis.*
import player.uis.components.*
import java.awt.*
import java.awt.event.*
import javax.swing.*

class UIPreviewPanel(private val game3D: Game3D, private val customUI: CustomizableGameUI) : JPanel() {
    // Selected component for drag and drop
    private var selectedComponent: UIComponent? = null
    private var dragStartX = 0
    private var dragStartY = 0
    private var dragOffsetX = 0
    private var dragOffsetY = 0

    private var selectionListener: ((UIComponent?) -> Unit)? = null
    private var backgroundImage: Image? = null

    private var currentWidth = 800
    private var currentHeight = 600

    init {
        preferredSize = Dimension(800, 600)
        background = Color(35, 35, 35)

        // Add mouse listeners for drag and drop
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                // Find component under cursor using properly scaled coordinates
                val component = customUI.getComponentAt(e.x, e.y, currentWidth, currentHeight)
                if (component != null) {
                    selectedComponent = component
                    dragStartX = e.x
                    dragStartY = e.y

                    // Calculate drag offsets in design coordinates
                    val scaleX = customUI.designWidth.toFloat() / currentWidth
                    val scaleY = customUI.designHeight.toFloat() / currentHeight

                    dragOffsetX = (e.x * scaleX).toInt() - component.x
                    dragOffsetY = (e.y * scaleY).toInt() - component.y

                    // Notify selection listener
                    selectionListener?.invoke(component)
                    repaint()
                } else {
                    // Clear selection if clicked on empty space
                    selectedComponent = null
                    selectionListener?.invoke(null)
                    repaint()
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                // Don't clear the selection, just stop dragging
                // selectedComponent remains set so the component stays highlighted
                repaint()
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                val component = selectedComponent ?: return

                // Calculate scale factors
                val scaleX = customUI.designWidth.toFloat() / currentWidth
                val scaleY = customUI.designHeight.toFloat() / currentHeight

                // Convert screen coordinates to design coordinates
                component.x = ((e.x * scaleX).toInt() - dragOffsetX)
                component.y = ((e.y * scaleY).toInt() - dragOffsetY)

                // Keep within design bounds
                component.x = component.x.coerceIn(0, customUI.designWidth - component.width)
                component.y = component.y.coerceIn(0, customUI.designHeight - component.height)

                repaint()
            }
        })

        // Take screenshot of game to use as background
        updateBackgroundImage()

        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                currentWidth = width
                currentHeight = height
                updateBackgroundImage()
                repaint()
            }
        })
    }

    // Add a method to set the selection listener
    fun setSelectionListener(listener: (UIComponent?) -> Unit) {
        this.selectionListener = listener
    }

    fun updateBackgroundImage() {
        // Create an image of the current game state
        backgroundImage = createImage(width, height)
        val g = backgroundImage?.graphics as? Graphics2D ?: return

        // Draw sky
        val skyRenderer = game3D.getSkyRenderer()
        skyRenderer.render(g, width, height)

        // We could add more game elements here but let's keep it simple

        g.dispose()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D

        // Calculate scale factors for drawing
        val scaleX = currentWidth.toFloat() / customUI.designWidth
        val scaleY = currentHeight.toFloat() / customUI.designHeight

        // Draw background
        backgroundImage?.let { g2.drawImage(it, 0, 0, currentWidth, currentHeight, null) }

        // Apply scaling transformation
        val originalTransform = g2.transform
        g2.scale(scaleX.toDouble(), scaleY.toDouble())

        // Draw UI components in design coordinates
        customUI.render(g2, customUI.designWidth, customUI.designHeight)

        // Highlight selected component
        selectedComponent?.let {
            g2.color = Color(255, 255, 0, 100)
            g2.fillRect(it.x, it.y, it.width, it.height)

            g2.color = Color(255, 255, 0)
            g2.drawRect(it.x, it.y, it.width, it.height)

            // Draw handle points
            val handleSize = 8
            g2.fillRect(it.x - handleSize/2, it.y - handleSize/2, handleSize, handleSize) // Top-left
            g2.fillRect(it.x + it.width - handleSize/2, it.y - handleSize/2, handleSize, handleSize) // Top-right
            g2.fillRect(it.x - handleSize/2, it.y + it.height - handleSize/2, handleSize, handleSize) // Bottom-left
            g2.fillRect(it.x + it.width - handleSize/2, it.y + it.height - handleSize/2, handleSize, handleSize) // Bottom-right
        }

        // Restore original transformation
        g2.transform = originalTransform
    }
}