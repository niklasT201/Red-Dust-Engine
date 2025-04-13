package texturemanager

import ImageEntry
import ObjectType
import grideditor.GridEditor
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.filechooser.FileNameExtensionFilter

class TextureManagerPanel(private val resourceManager: ResourceManager) : JPanel() {
    var gridEditor: GridEditor? = null

    // Filter the object types to only include FLOOR and WALL
    private val allowedObjectTypes = listOf(
        ObjectType.WALL,
        ObjectType.FLOOR,
        ObjectType.PILLAR,
        ObjectType.WATER,
        ObjectType.RAMP,
    )
    private val objectTypeComboBox = JComboBox(allowedObjectTypes.toTypedArray())

    private val textureListModel = DefaultListModel<TextureEntry>()
    private val textureList = JList(textureListModel)
    private val previewLabel = JLabel()

    // Map to store textures by object type
    private val texturesByType = mutableMapOf<ObjectType, MutableList<TextureEntry>>()

    // Last browsed directory
    private var lastDirectory: File? = null

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
            layout = GridLayout(3, 2, 5, 5) // Changed from 2x2 to 3x2 to add new button
            background = Color(40, 44, 52)
            alignmentX = LEFT_ALIGNMENT

            add(createButton("Add Texture") { addTexture() })
            add(createButton("Add Folder") { addTextureFolder() })
            add(createButton("Remove") { removeSelectedTexture() })
            add(createButton("Set Default") {
                val selectedEntry = textureList.selectedValue
                if (selectedEntry != null) {
                    val objectType = selectedEntry.objectType
                    // Mark as default in our manager
                    setAsDefault(selectedEntry, objectType)
                    // Notify listener (EditorPanel) to update GridEditor
                    textureSelectionListener?.onTextureSetAsDefault(selectedEntry, objectType)
                } else {
                    JOptionPane.showMessageDialog(this, "No texture selected!", "Error", JOptionPane.ERROR_MESSAGE)
                }
            })
            // Use color instead of texture
            add(createButton("Use Color") {
                val objectType = objectTypeComboBox.selectedItem as ObjectType
                clearTextureForType(objectType)
            })
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

        loadTexturesFromResourceManager()
    }

    interface TextureSelectionListener {
        fun onTextureSetAsDefault(entry: TextureEntry, objectType: ObjectType)
        fun onTextureCleared(objectType: ObjectType)
    }

    // Add a field to store the listener
    private var textureSelectionListener: TextureSelectionListener? = null

    // Add this method to set the listener
    fun setTextureSelectionListener(listener: TextureSelectionListener) {
        textureSelectionListener = listener
    }

    private fun createButton(text: String, action: () -> Unit): JButton {
        return JButton(text).apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
            maximumSize = Dimension(140, 25)  // Reduced height from 30
            margin = Insets(2, 4, 2, 4)  // Smaller internal margins
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
                    // Load image and store it in the resource manager
                    val imageEntry = resourceManager.loadImageFromFile(file)
                    if (imageEntry != null) {
                        addTextureEntry(imageEntry)
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

            // Use ResourceManager to load textures from directory
            val loadedImages = resourceManager.loadTexturesFromDirectory(directory)

            var loadedCount = 0
            var errorCount = 0

            for (imageEntry in loadedImages) {
                try {
                    addTextureEntry(imageEntry)
                    loadedCount++
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

    private fun addTextureEntry(imageEntry: ImageEntry) {
        // Create texture entry
        val selectedType = objectTypeComboBox.selectedItem as ObjectType
        val textureEntry = TextureEntry(selectedType, imageEntry)

        // Add to type-specific list
        texturesByType.getOrPut(selectedType) { mutableListOf() }.add(textureEntry)
    }

    private fun removeSelectedTexture() {
        val selectedEntry = textureList.selectedValue ?: return

        // Check if this is the default texture for its type
        val isDefault = selectedEntry.isDefault
        val objectType = selectedEntry.objectType

        // Find the ID of the image in the ResourceManager
        val imageId = findImageId(selectedEntry.imageEntry)

        if (imageId != null) {
            // Ask for confirmation
            val confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to permanently delete '${selectedEntry.imageEntry.name}'? This cannot be undone.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            )

            if (confirm == JOptionPane.YES_OPTION) {
                // Remove from resource manager (and file system)
                val success = resourceManager.removeImage(imageId)

                if (success) {
                    // Remove from local tracking
                    texturesByType[objectType]?.remove(selectedEntry)

                    // If the deleted texture was the default, revert to using color
                    if (isDefault) {
                        clearTextureForType(objectType)
                    } else {
                        // Just update the UI
                        updateTextureList()
                    }

                    // Clear preview if this was the selected texture
                    if (previewLabel.icon != null) {
                        previewLabel.icon = null
                    }

                    println("Successfully removed texture: ${selectedEntry.imageEntry.name}")
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Failed to delete texture file. The entry has been removed from the list only.",
                        "Deletion Error",
                        JOptionPane.ERROR_MESSAGE
                    )

                    // Remove from local tracking even if file deletion failed
                    texturesByType[objectType]?.remove(selectedEntry)

                    // If default, revert to using color
                    if (isDefault) {
                        clearTextureForType(objectType)
                    } else {
                        updateTextureList()
                    }
                }
            }
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Could not find the selected texture in the resource manager.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    // Helper method to find the ID of an ImageEntry in the ResourceManager
    private fun findImageId(imageEntry: ImageEntry): String? {
        val allImages = resourceManager.getAllImages()
        for ((id, entry) in allImages) {
            if (entry.path == imageEntry.path) {
                return id
            }
        }
        return null
    }

    private fun setAsDefault(entry: TextureEntry, objectType: ObjectType) {
        // Remove default flag from all textures of this type
        texturesByType[objectType]?.forEachIndexed { index, texture ->
            texturesByType[objectType]!![index] = texture.copy(isDefault = texture == entry)
        }

        // Update the texture list to show the new default
        updateTextureList()

        println("TextureManager: Marked '${entry.imageEntry.name}' as default for ${objectType.name}")
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

    fun getDefaultTextureForType(type: ObjectType): ImageEntry? {
        return texturesByType[type]?.find { it.isDefault }?.imageEntry
    }

    fun getTexturesForType(type: ObjectType): List<ImageEntry> {
        return texturesByType[type]?.map { it.imageEntry } ?: emptyList()
    }

    private fun loadTexturesFromResourceManager() {
        // Clear existing entries first
        texturesByType.clear()

        for (entry in resourceManager.getAllImages()) {
            val (_, imageEntry) = entry

            // Attempt to determine object type from file name patterns
            val filename = imageEntry.name.lowercase()
            val objectType = when {
                // WALL Keywords
                filename.contains("wall") || filename.contains("brick") ||
                        filename.contains("wood") || filename.contains("stone") ||
                        filename.contains("siding") -> ObjectType.WALL

                // FLOOR Keywords
                filename.contains("floor") || filename.contains("ground") ||
                        filename.contains("tile") || filename.contains("carpet") ||
                        filename.contains("grass") || filename.contains("dirt") -> ObjectType.FLOOR

                // PILLAR Keywords
                filename.contains("pillar") || filename.contains("column") ||
                        filename.contains("support") -> ObjectType.PILLAR // Added

                // WATER Keywords
                filename.contains("water") || filename.contains("liquid") ||
                        filename.contains("fluid") -> ObjectType.WATER // Added

                // RAMP Keywords
                filename.contains("ramp") || filename.contains("slope") ||
                        filename.contains("incline") -> ObjectType.RAMP // Added

                // PROP Keywords (General catch-all for items/decorations)
                filename.contains("prop") || filename.contains("object") ||
                        filename.contains("item") || filename.contains("decor") ||
                        filename.contains("sprite") -> ObjectType.PROP

                // PLAYER_SPAWN (Usually not textured, but handle if named)
                filename.contains("spawn") || filename.contains("start") -> ObjectType.PLAYER_SPAWN

                else -> objectTypeComboBox.selectedItem as ObjectType
            }

            if (allowedObjectTypes.contains(objectType)) {
                val textureEntry = TextureEntry(objectType, imageEntry)
                texturesByType.getOrPut(objectType) { mutableListOf() }.add(textureEntry)
            } else if (objectType == ObjectType.PLAYER_SPAWN) {
                // Handle player spawn if needed, or just ignore if it doesn't get textures
                println("Note: Detected PLAYER_SPAWN texture '${imageEntry.name}', but it's not managed in the panel.")
            } else {
                // This case might happen if the fallback type (PROP) isn't in allowedObjectTypes for some reason
                println("Warning: Texture '${imageEntry.name}' guessed as $objectType, which is not in the allowed list for the panel. Assigning to first allowed type.")
                val fallbackType = allowedObjectTypes.firstOrNull() ?: ObjectType.WALL // Failsafe
                val textureEntry = TextureEntry(fallbackType, imageEntry)
                texturesByType.getOrPut(fallbackType) { mutableListOf() }.add(textureEntry)
            }
        }

        updateTextureList() // Refresh the list based on the selected type
    }

    private fun clearTextureForType(objectType: ObjectType) {
        // Find any texture marked as default for this type and unmark it
        texturesByType[objectType]?.forEachIndexed { index, texture ->
            texturesByType[objectType]!![index] = texture.copy(isDefault = false)
        }
        updateTextureList()

        // Notify listeners that texture has been cleared
        textureSelectionListener?.onTextureCleared(objectType)
    }
}