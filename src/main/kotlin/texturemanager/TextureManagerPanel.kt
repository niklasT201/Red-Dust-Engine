package texturemanager

import ImageEntry
import ObjectType
import grideditor.GridEditor
import ui.EditorPanel
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.filechooser.FileNameExtensionFilter

class TextureManagerPanel(private val resourceManager: ResourceManager) : JPanel() {
    var gridEditor: GridEditor? = null
    private val objectTypeComboBox = JComboBox(ObjectType.values())
    private val textureListModel = DefaultListModel<TextureEntry>()
    private val textureList = JList(textureListModel)
    private val previewLabel = JLabel()

    // Map to store textures by object type
    private val texturesByType = mutableMapOf<ObjectType, MutableList<TextureEntry>>()

    // Last browsed directory
    private var lastDirectory: File? = null

    private var editorPanel: EditorPanel? = null

    data class TextureEntry(
        val objectType: ObjectType,
        val imageEntry: ImageEntry,
        val isDefault: Boolean = false
    )

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)
        border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color(70, 73, 75)),
            "Texture Manager",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            null,
            Color.WHITE
        )

        // Object Type Selection
        val typePanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = Color(40, 44, 52)
            add(JLabel("Object Type:").apply { foreground = Color.WHITE })
            add(Box.createHorizontalStrut(10))
            add(objectTypeComboBox.apply {
                background = Color(60, 63, 65)
                foreground = Color.WHITE
            })
            alignmentX = LEFT_ALIGNMENT
        }

        // Texture List
        textureList.apply {
            background = Color(50, 54, 62)
            foreground = Color.WHITE
            selectionBackground = Color(100, 100, 255)
            selectionForeground = Color.WHITE
            visibleRowCount = 5
            fixedCellHeight = 40 // Increased height for thumbnails
            cellRenderer = createTextureListRenderer()
        }

        val scrollPane = JScrollPane(textureList).apply {
            preferredSize = Dimension(0, 150) // Increased height
            border = BorderFactory.createLineBorder(Color(70, 73, 75))
            background = Color(40, 44, 52)
            alignmentX = LEFT_ALIGNMENT
        }

        // Preview Panel
        val previewPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color(70, 73, 75)),
                "Preview",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                Color.WHITE
            )
            alignmentX = LEFT_ALIGNMENT

            // Create a fixed-size panel for the preview
            add(JPanel().apply {
                preferredSize = Dimension(150, 150) // Larger preview
                background = Color(50, 54, 62)
                layout = BorderLayout()
                add(previewLabel, BorderLayout.CENTER)
            })
        }

        // Buttons
        val buttonPanel = JPanel().apply {
            layout = GridLayout(2, 2, 5, 5)
            background = Color(40, 44, 52)
            alignmentX = LEFT_ALIGNMENT

            add(createButton("Add Texture") { addTexture() })
            add(createButton("Add Folder") { addTextureFolder() })
            add(createButton("Remove") { removeSelectedTexture() })
            add(createButton("Set Default") { setAsDefault() })
        }

        // Layout
        add(typePanel)
        add(Box.createVerticalStrut(10))
        add(scrollPane)
        add(Box.createVerticalStrut(10))
        add(previewPanel)
        add(Box.createVerticalStrut(10))
        add(buttonPanel)
        add(Box.createVerticalGlue())

        // Event Listeners
        objectTypeComboBox.addActionListener { updateTextureList() }
        textureList.addListSelectionListener { updatePreview() }
    }

    private fun createButton(text: String, action: () -> Unit): JButton {
        return JButton(text).apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(Int.MAX_VALUE, 30)
            addActionListener { action() }
        }
    }

    private fun createTextureListRenderer(): DefaultListCellRenderer {
        return object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val panel = JPanel(BorderLayout()).apply {
                    background = if (isSelected) Color(100, 100, 255) else Color(50, 54, 62)
                    border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
                }

                val entry = value as? TextureEntry
                if (entry != null) {
                    // Create thumbnail
                    val thumb = createThumbnail(entry.imageEntry.image, 32, 32)
                    val imageLabel = JLabel(ImageIcon(thumb))

                    // Create text label
                    val textLabel = JLabel(if (entry.isDefault) "${entry.imageEntry.name} (Default)" else entry.imageEntry.name)
                    textLabel.foreground = Color.WHITE

                    panel.add(imageLabel, BorderLayout.WEST)
                    panel.add(textLabel, BorderLayout.CENTER)
                }

                return panel
            }
        }
    }

    private fun createThumbnail(image: Image, width: Int, height: Int): Image {
        val thumb = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = thumb.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.drawImage(image, 0, 0, width, height, null)
        g2d.dispose()
        return thumb
    }

    private fun addTexture() {
        val fileChooser = JFileChooser(lastDirectory).apply {
            fileFilter = FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif", "bmp")
            fileSelectionMode = JFileChooser.FILES_ONLY
            isMultiSelectionEnabled = true
        }

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            lastDirectory = fileChooser.currentDirectory
            val files = fileChooser.selectedFiles

            for (file in files) {
                try {
                    val image = ImageIO.read(file)
                    if (image != null) {
                        addTextureFromImage(file.name, file.path, image)
                    } else {
                        JOptionPane.showMessageDialog(this, "Could not load image: ${file.name}", "Error", JOptionPane.ERROR_MESSAGE)
                    }
                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(this, "Error loading image: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }

            updateTextureList()
        }
    }

    private fun addTextureFolder() {
        val fileChooser = JFileChooser(lastDirectory).apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            lastDirectory = fileChooser.selectedFile
            val directory = fileChooser.selectedFile

            val imageFilter = FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif", "bmp")
            val files = directory.listFiles { file ->
                file.isFile && imageFilter.accept(file)
            } ?: return

            var loadedCount = 0
            var errorCount = 0

            for (file in files) {
                try {
                    val image = ImageIO.read(file)
                    if (image != null) {
                        addTextureFromImage(file.name, file.path, image)
                        loadedCount++
                    } else {
                        errorCount++
                    }
                } catch (e: Exception) {
                    errorCount++
                }
            }

            JOptionPane.showMessageDialog(
                this,
                "Loaded $loadedCount images. $errorCount files could not be loaded.",
                "Import Complete",
                JOptionPane.INFORMATION_MESSAGE
            )

            updateTextureList()
        }
    }

    private fun addTextureFromImage(name: String, path: String, image: Image) {
        // Add to resource manager
        val id = resourceManager.addImage(name, path, image)

        // Create texture entry
        val selectedType = objectTypeComboBox.selectedItem as ObjectType
        val textureEntry = TextureEntry(selectedType, ImageEntry(name, path, image))

        // Add to type-specific list
        texturesByType.getOrPut(selectedType) { mutableListOf() }.add(textureEntry)
    }

    private fun removeSelectedTexture() {
        val selectedEntry = textureList.selectedValue as? TextureEntry ?: return
        texturesByType[selectedEntry.objectType]?.remove(selectedEntry)
        updateTextureList()
    }

    private fun setAsDefault() {
        val selectedEntry = textureList.selectedValue as? TextureEntry ?: return
        val objectType = selectedEntry.objectType

        println("Setting default texture for $objectType: ${selectedEntry.imageEntry.name}")

        // Remove default flag from all textures of this type
        texturesByType[objectType]?.forEachIndexed { index, entry ->
            texturesByType[objectType]!![index] = entry.copy(isDefault = entry == selectedEntry)
        }

        // Apply the selected texture to the gridEditor based on type
        gridEditor?.let { editor ->
            when (objectType) {
                ObjectType.WALL -> {
                    println("Applying wall texture to grid editor")
                    editor.setWallTexture(selectedEntry.imageEntry)
                    editor.currentWallTexture = selectedEntry.imageEntry // Explicitly set the texture
                }
                ObjectType.FLOOR -> {
                    println("Applying floor texture to grid editor")
                    editor.setFloorTexture(selectedEntry.imageEntry)
                }
                // Handle other types as needed
                else -> {}
            }
        }

        // Show confirmation popup
        JOptionPane.showMessageDialog(
            this,
            "Set '${selectedEntry.imageEntry.name}' as default for ${objectType.name}",
            "Default Texture Set",
            JOptionPane.INFORMATION_MESSAGE
        )

        updateTextureList()
    }

    private fun updateTextureList() {
        textureListModel.clear()
        val selectedType = objectTypeComboBox.selectedItem as ObjectType
        texturesByType[selectedType]?.forEach { textureListModel.addElement(it) }
    }

    private fun updatePreview() {
        val selectedTexture = textureList.selectedValue as? TextureEntry
        if (selectedTexture != null) {
            val image = selectedTexture.imageEntry.image
            val scaledImage = image.getScaledInstance(150, 150, Image.SCALE_SMOOTH)
            previewLabel.icon = ImageIcon(scaledImage)
        } else {
            previewLabel.icon = null
        }
    }

    fun getAllTexturesForType(type: ObjectType): List<ImageEntry> {
        return texturesByType[type]?.map { it.imageEntry } ?: emptyList()
    }

    fun getDefaultTextureForType(type: ObjectType): ImageEntry? {
        return texturesByType[type]?.find { it.isDefault }?.imageEntry
    }

    fun getTexturesForType(type: ObjectType): List<ImageEntry> {
        return texturesByType[type]?.map { it.imageEntry } ?: emptyList()
    }

    fun getSelectedTextureEntry(): TextureEntry? {
        return textureList.selectedValue
    }

    fun setEditorPanel(panel: EditorPanel) {
        editorPanel = panel
    }

    fun loadTexturesFromResourceManager() {
        for (entry in resourceManager.getAllImages()) {
            val (id, imageEntry) = entry

            // Attempt to determine object type from file name patterns
            val filename = imageEntry.name.toLowerCase()
            val objectType = when {
                filename.contains("wall") || filename.contains("brick") ||
                        filename.contains("wood") || filename.contains("stone") -> ObjectType.WALL

                filename.contains("floor") || filename.contains("ground") ||
                        filename.contains("tile") || filename.contains("carpet") -> ObjectType.FLOOR

                filename.contains("prop") || filename.contains("object") ||
                        filename.contains("item") -> ObjectType.PROP

                filename.contains("spawn") || filename.contains("start") -> ObjectType.PLAYER_SPAWN

                else -> objectTypeComboBox.selectedItem as ObjectType
            }

            val textureEntry = TextureEntry(objectType, imageEntry)
            texturesByType.getOrPut(objectType) { mutableListOf() }.add(textureEntry)
        }
        updateTextureList()
    }
}