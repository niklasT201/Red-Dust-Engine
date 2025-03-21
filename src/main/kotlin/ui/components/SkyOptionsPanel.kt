package ui.components

import Game3D
import java.awt.*
import java.awt.event.*
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.filechooser.FileNameExtensionFilter

enum class SkyDisplayMode {
    COLOR, IMAGE_STRETCH, IMAGE_TILE;

    override fun toString(): String {
        return when (this) {
            COLOR -> "Solid Color"
            IMAGE_STRETCH -> "Image (Stretched)"
            IMAGE_TILE -> "Image (Tiled)"
        }
    }
}

class SkyOptionsPanel(private val game3D: Game3D) : JPanel() {
    private val colorOptions = arrayOf(
        "Blue Sky" to Color(135, 206, 235),
        "Sunset" to Color(255, 178, 102),
        "Night" to Color(25, 25, 50),
        "Stormy" to Color(105, 105, 105),
        "Dawn" to Color(255, 204, 204),
        "Custom..." to Color.WHITE
    )

    private val colorComboBox = JComboBox(colorOptions.map { it.first }.toTypedArray())
    private val displayModeComboBox = JComboBox(SkyDisplayMode.entries.toTypedArray())
    private val imagePathLabel = JLabel("No image selected")
    private val browseButton = JButton("Browse...")
    private var skyImage: Image? = null
    private var customColor: Color = Color(135, 206, 235)
    private val previewPanel = SkyPreviewPanel()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(50, 52, 55)
        border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color(70, 73, 75)),
            "Sky Options",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            null,
            Color.WHITE
        )

        // Initialize with current sky color from game
        val currentColor = game3D.getSkyColor()?:Color.BLUE
        val colorIndex = colorOptions.indexOfFirst {
            it.second.red == currentColor.red &&
                    it.second.green == currentColor.green &&
                    it.second.blue == currentColor.blue
        }.takeIf { it >= 0 } ?: 0
        colorComboBox.selectedIndex = colorIndex
        customColor = currentColor

        // Set up UI components styling
        colorComboBox.foreground = Color.WHITE
        colorComboBox.background = Color(40, 42, 45)
        (colorComboBox.renderer as JComponent).background = Color(40, 42, 45)

        displayModeComboBox.foreground = Color.WHITE
        displayModeComboBox.background = Color(40, 42, 45)
        (displayModeComboBox.renderer as JComponent).background = Color(40, 42, 45)
        displayModeComboBox.selectedItem = SkyDisplayMode.COLOR

        imagePathLabel.foreground = Color.LIGHT_GRAY
        browseButton.background = Color(40, 42, 45)
        browseButton.foreground = Color.WHITE

        // Create a panel for the color dropdown
        val colorPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        colorPanel.background = Color(50, 52, 55)
        colorPanel.add(JLabel("Sky Color:").apply { foreground = Color.WHITE })
        colorPanel.add(colorComboBox)
        add(colorPanel)

        // Create a panel for the display mode dropdown
        val displayModePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        displayModePanel.background = Color(50, 52, 55)
        displayModePanel.add(JLabel("Display Mode:").apply { foreground = Color.WHITE })
        displayModePanel.add(displayModeComboBox)
        add(displayModePanel)

        // Create a panel for the image selection
        val imagePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        imagePanel.background = Color(50, 52, 55)
        imagePanel.add(JLabel("Sky Image:").apply { foreground = Color.WHITE })
        imagePanel.add(imagePathLabel)
        imagePanel.add(browseButton)
        add(imagePanel)

        // Add a preview panel
        previewPanel.preferredSize = Dimension(200, 100)
        add(Box.createRigidArea(Dimension(0, 10)))
        add(previewPanel)

        // Set up color selection listener
        colorComboBox.addActionListener {
            val selectedIndex = colorComboBox.selectedIndex
            if (selectedIndex in colorOptions.indices) {
                if (selectedIndex == colorOptions.size - 1) {
                    // Custom color option selected
                    val newColor = JColorChooser.showDialog(
                        this,
                        "Choose Sky Color",
                        customColor
                    )
                    if (newColor != null) {
                        customColor = newColor
                        game3D.setSkyColor(customColor)
                    }
                } else {
                    game3D.setSkyColor(colorOptions[selectedIndex].second)
                }

                // Set display mode to COLOR when a color is selected
                displayModeComboBox.selectedItem = SkyDisplayMode.COLOR
                updateSkyRenderer()
                previewPanel.repaint()
            }
        }

        // Set up display mode selection listener
        displayModeComboBox.addActionListener {
            val useImage = displayModeComboBox.selectedItem != SkyDisplayMode.COLOR

            // Enable/disable image selection based on mode
            imagePathLabel.isEnabled = useImage
            browseButton.isEnabled = useImage

            // Only update renderer if we're switching to COLOR mode or we already have an image
            if (!useImage || skyImage != null) {
                updateSkyRenderer()
            }

            previewPanel.repaint()
        }

        // Set up browse button for image selection
        browseButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.dialogTitle = "Select Sky Image"
            fileChooser.fileFilter = FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png", "gif")

            val result = fileChooser.showOpenDialog(this)
            if (result == JFileChooser.APPROVE_OPTION) {
                updateSkyImage(fileChooser.selectedFile)
            }
        }

        // Initial state of image controls based on display mode
        val useImage = displayModeComboBox.selectedItem != SkyDisplayMode.COLOR
        imagePathLabel.isEnabled = useImage
        browseButton.isEnabled = useImage
    }

    private fun updateSkyRenderer() {
        val displayMode = displayModeComboBox.selectedItem as SkyDisplayMode

        when (displayMode) {
            SkyDisplayMode.COLOR -> {
                // Just update the color in Game3D
                game3D.setSkyRenderer(SkyRenderer(game3D.getSkyColor()))
            }
            SkyDisplayMode.IMAGE_STRETCH, SkyDisplayMode.IMAGE_TILE -> {
                // Only show warning if user tries to apply without selecting an image
                // For now, just enable the browse button and wait for user to select image
                if (skyImage != null) {
                    // We have an image, so we can set the renderer
                    game3D.setSkyRenderer(SkyRenderer(
                        game3D.getSkyColor(),
                        skyImage,
                        displayMode == SkyDisplayMode.IMAGE_TILE
                    ))
                }
                // Don't revert to COLOR mode here - let the user select an image
            }
        }
    }

    private fun updateSkyImage(selectedFile: File) {
        try {
            skyImage = ImageIO.read(selectedFile)
            imagePathLabel.text = selectedFile.name

            // Now that we have an image, update the renderer with the current display mode
            updateSkyRenderer()
            previewPanel.repaint()
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                this,
                "Error loading image: ${e.message}",
                "Image Load Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    fun refreshFromGameState() {
        // Update the UI controls to match the game state
        val currentColor = game3D.getSkyColor()
        val colorIndex = colorOptions.indexOfFirst {
            it.second.red == currentColor.red &&
                    it.second.green == currentColor.green &&
                    it.second.blue == currentColor.blue
        }.takeIf { it >= 0 } ?: 0

        // Only update if different to avoid event loops
        if (colorComboBox.selectedIndex != colorIndex) {
            colorComboBox.selectedIndex = colorIndex
        }

        // Refresh the sky renderer state if needed
        val renderer = game3D.getSkyRenderer()
        if (renderer != null) {
            if (renderer.skyImage != null) {
                skyImage = renderer.skyImage
                displayModeComboBox.selectedItem = if (renderer.tileImage)
                    SkyDisplayMode.IMAGE_TILE else SkyDisplayMode.IMAGE_STRETCH
                imagePathLabel.text = "Image loaded"
            } else {
                displayModeComboBox.selectedItem = SkyDisplayMode.COLOR
            }
        }
    }

    // Preview panel to show what the sky will look like
    inner class SkyPreviewPanel : JPanel() {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D

            // Draw border
            g2.color = Color(70, 73, 75)
            g2.drawRect(0, 0, width - 1, height - 1)

            val displayMode = displayModeComboBox.selectedItem as SkyDisplayMode

            when (displayMode) {
                SkyDisplayMode.COLOR -> {
                    // Just draw the solid color
                    val selectedIndex = colorComboBox.selectedIndex
                    g2.color = if (selectedIndex == colorOptions.size - 1) customColor
                    else colorOptions[selectedIndex].second
                    g2.fillRect(1, 1, width - 2, height - 2)
                }
                SkyDisplayMode.IMAGE_STRETCH -> {
                    if (skyImage != null) {
                        g2.drawImage(skyImage, 1, 1, width - 2, height - 2, null)
                    } else {
                        // Fallback to color if no image
                        g2.color = game3D.getSkyColor()
                        g2.fillRect(1, 1, width - 2, height - 2)
                    }
                }
                SkyDisplayMode.IMAGE_TILE -> {
                    if (skyImage != null) {
                        // Create a tiled pattern
                        val img = skyImage!!
                        val imgWidth = img.getWidth(null)
                        val imgHeight = img.getHeight(null)

                        if (imgWidth > 0 && imgHeight > 0) {
                            for (x in 1 until width - 1 step imgWidth) {
                                for (y in 1 until height - 1 step imgHeight) {
                                    g2.drawImage(img, x, y, null)
                                }
                            }

                            // Set clipping to prevent drawing outside the panel
                            g2.clip = Rectangle(1, 1, width - 2, height - 2)
                        }
                    } else {
                        // Fallback to color if no image
                        g2.color = game3D.getSkyColor()
                        g2.fillRect(1, 1, width - 2, height - 2)
                    }
                }
            }

            // Add a small label to show this is a preview
            g2.color = Color(255, 255, 255, 180)
            g2.font = Font("SansSerif", Font.BOLD, 10)
            g2.drawString("Preview", 5, 12)
        }
    }
}