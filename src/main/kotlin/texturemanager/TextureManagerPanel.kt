package texturemanager

import ImageEntry
import ObjectType
import grideditor.GridEditor
import ui.topbar.FileManager
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Paths
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class TextureManagerPanel(private val resourceManager: ResourceManager, private val fileManager: FileManager) : JPanel() {
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

    private val bgColor = Color(30, 33, 40)
    private val panelColor = Color(35, 38, 45)
    private val listBgColor = Color(40, 43, 50)
    private val highlightColor = Color(220, 95, 60) // The orange/red accent color
    private val buttonColor = Color(60, 63, 70)
    private val buttonHoverColor = Color(80, 83, 90)
    private val borderColor = Color(50, 53, 60)

    data class TextureEntry(
        val objectType: ObjectType,
        val imageEntry: ImageEntry,
        val isDefault: Boolean = false
    )

    init {
        layout = BorderLayout(5, 5)
        background = bgColor
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

        // Title panel with gradient
        val titlePanel = createGradientPanel().apply {
            layout = BorderLayout()
            preferredSize = Dimension(0, 30)

            // Title label
            add(JLabel("TEXTURE MANAGER", SwingConstants.CENTER).apply {
                foreground = highlightColor
                font = Font("Arial", Font.BOLD, 14)
                border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
            }, BorderLayout.CENTER)
        }

        // Main content panel
        val contentPanel = JPanel().apply {
            layout = BorderLayout(10, 10)
            background = bgColor
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }

        // Object Type Selection with styled combo box
        val typePanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = bgColor

            add(JLabel("Object Type:").apply {
                foreground = Color.WHITE
                font = Font("Arial", Font.BOLD, 12)
            })
            add(Box.createHorizontalStrut(10))
            add(objectTypeComboBox.apply {
                background = buttonColor
                foreground = Color.WHITE
                renderer = createStyledComboBoxRenderer()
                maximumSize = Dimension(120, 25)
            })
            border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
        }

        // Texture List with custom renderer
        textureList.apply {
            background = listBgColor
            foreground = Color.WHITE
            selectionBackground = highlightColor.darker()
            selectionForeground = Color.WHITE
            visibleRowCount = 5
            fixedCellHeight = 32
            cellRenderer = createTextureListRenderer()
        }

        val scrollPane = JScrollPane(textureList).apply {
            preferredSize = Dimension(0, 140)
            border = BorderFactory.createLineBorder(borderColor)
            background = bgColor
        }

        // Left side panel - list and controls
        val leftPanel = JPanel().apply {
            layout = BorderLayout(0, 5)
            background = bgColor

            add(typePanel, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
        }

        // Right side panel - preview and action buttons
        val rightPanel = JPanel().apply {
            layout = BorderLayout(0, 5)
            background = bgColor
        }

        // Preview Panel with styled border
        val previewPanel = JPanel().apply {
            layout = BorderLayout()
            background = panelColor
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            )
            preferredSize = Dimension(120, 120)

            // Preview label centered
            add(JLabel("Preview", SwingConstants.CENTER).apply {
                foreground = Color.WHITE
                font = Font("Arial", Font.BOLD, 12)
                border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
            }, BorderLayout.NORTH)

            // Create a fixed-size panel for the preview
            add(JPanel().apply {
                background = listBgColor
                layout = BorderLayout()
                add(previewLabel, BorderLayout.CENTER)
                border = BorderFactory.createLineBorder(borderColor)
            }, BorderLayout.CENTER)
        }

        // Streamlined action buttons - now in a vertical layout
        val buttonPanel = JPanel().apply {
            layout = GridLayout(0, 1, 0, 5)
            background = bgColor
            border = BorderFactory.createEmptyBorder(5, 0, 0, 0)

            // Group related actions
            add(createActionButton("Add Texture", "Plus", false) { addTexture() })
            add(createActionButton("Add Folder", "FolderPlus", false) { addTextureFolder() })
            add(Box.createVerticalStrut(5)) // Spacer
            add(createActionButton("Set Default", "Star", false) {
                val selectedEntry = textureList.selectedValue
                if (selectedEntry != null) {
                    val objectType = selectedEntry.objectType
                    // Mark as default in our manager
                    setAsDefault(selectedEntry, objectType)
                    // Notify listener (EditorPanel) to update GridEditor
                    textureSelectionListener?.onTextureSetAsDefault(selectedEntry, objectType)
                } else {
                    showErrorMessage("No texture selected!")
                }
            })
            add(createActionButton("Use Color", "Palette", false) {
                val objectType = objectTypeComboBox.selectedItem as ObjectType
                clearTextureForType(objectType)
            })
            add(Box.createVerticalStrut(5)) // Spacer
            add(createActionButton("Remove", "Trash", true) { removeSelectedTexture() })
            add(createActionButton("Project Assets", "FolderOpen", false) { viewProjectAssets() })
        }

        // Add components to right panel
        rightPanel.add(previewPanel, BorderLayout.CENTER)
        rightPanel.add(buttonPanel, BorderLayout.SOUTH)

        // Add main components to content panel
        contentPanel.add(leftPanel, BorderLayout.CENTER)
        contentPanel.add(rightPanel, BorderLayout.EAST)

        // Layout the main panel
        add(titlePanel, BorderLayout.NORTH)
        add(contentPanel, BorderLayout.CENTER)

        // Event Listeners
        objectTypeComboBox.addActionListener { updateTextureList() }
        textureList.addListSelectionListener { updatePreview() }

        loadTexturesFromResourceManager()

        SwingUtilities.invokeLater {
            println("\n==== TEXTURE MANAGER INITIALIZED ====")
            dumpTextureManagementState()
        }
    }

    private fun viewProjectAssets() {
        if (fileManager.getCurrentProjectName() == null) {
            JOptionPane.showMessageDialog(
                this,
                "No project is currently loaded. Please load or create a project first.",
                "No Project Loaded",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        // Find the parent JFrame
        val parentFrame = SwingUtilities.getWindowAncestor(this) as? JFrame
        // Create and show the dialog
        val dialog = ProjectAssetsDialog(parentFrame, fileManager)
        dialog.isVisible = true
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

    private fun createGradientPanel(): JPanel {
        return object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                val gradientPaint = GradientPaint(
                    0f, 0f, Color(30, 33, 40),
                    0f, height.toFloat(), Color(40, 43, 50)
                )
                g2d.paint = gradientPaint
                g2d.fillRect(0, 0, width, height)
            }
        }
    }

    private fun createActionButton(text: String, iconName: String, isDanger: Boolean, action: () -> Unit): JButton {
        return JButton(text).apply {
            background = if (isDanger) Color(150, 50, 50) else buttonColor
            foreground = Color.WHITE
            isFocusPainted = false
            font = Font("Arial", Font.BOLD, 11)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
            )

            // Style button on hover
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    background = if (isDanger) Color(180, 60, 60) else buttonHoverColor
                }

                override fun mouseExited(e: MouseEvent) {
                    background = if (isDanger) Color(150, 50, 50) else buttonColor
                }
            })

            addActionListener { action() }
        }
    }

    private fun createStyledComboBoxRenderer(): DefaultListCellRenderer {
        return object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (component is JLabel) {
                    component.foreground = if (isSelected) Color.WHITE else Color.WHITE
                    component.background = if (isSelected) highlightColor else buttonColor
                    component.border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
                    component.font = Font("Arial", Font.PLAIN, 12)
                }
                return component
            }
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
                val panel = JPanel(BorderLayout(6, 0)).apply {
                    background = if (isSelected) highlightColor.darker() else listBgColor
                    border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
                }

                val entry = value as? TextureEntry
                if (entry != null) {
                    // Create thumbnail with border
                    val thumb = createThumbnail(entry.imageEntry.image, 24, 24)
                    val imagePanel = JPanel(BorderLayout()).apply {
                        background = panel.background
                        preferredSize = Dimension(28, 28)

                        // Add thumbnail with border
                        val imageLabel = JLabel(ImageIcon(thumb))
                        imageLabel.border = BorderFactory.createLineBorder(borderColor)
                        add(imageLabel, BorderLayout.CENTER)
                    }

                    // Create text label with custom styling
                    val textLabel = JLabel(if (entry.isDefault) "${entry.imageEntry.name} ★" else entry.imageEntry.name)
                    textLabel.foreground = Color.WHITE
                    textLabel.font = if (entry.isDefault)
                        Font("Arial", Font.BOLD, 12)
                    else
                        Font("Arial", Font.PLAIN, 12)

                    panel.add(imagePanel, BorderLayout.WEST)
                    panel.add(textLabel, BorderLayout.CENTER)

                    // Add indicator for default textures
                    if (entry.isDefault) {
                        panel.add(JLabel().apply {
                            foreground = Color.YELLOW
                            font = Font("Arial", Font.BOLD, 14)
                            horizontalAlignment = SwingConstants.RIGHT
                        }, BorderLayout.EAST)
                    }
                }

                return panel
            }
        }
    }

    // Show styled error message
    private fun showErrorMessage(message: String) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE
        )
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
                        JOptionPane.showMessageDialog(this, "Please create/save your current project first before using images. Could not load image: ${file.name}", "Error", JOptionPane.ERROR_MESSAGE)
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

        // Save the association
        resourceManager.saveTextureObjectType(imageEntry.path, selectedType)
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
        return resourceManager.getAllImages().entries.find { (_, entry) ->
            // Compare normalized paths for robustness
            try {
                Paths.get(entry.path).normalize() == Paths.get(imageEntry.path).normalize()
            } catch (e: Exception) {
                entry.path == imageEntry.path // Fallback to direct string compare if path invalid
            }
        }?.key
    }

    private fun setAsDefault(entry: TextureEntry, objectType: ObjectType) {
        // Remove default flag from all textures of this type
        texturesByType[objectType]?.forEachIndexed { index, texture ->
            texturesByType[objectType]!![index] = texture.copy(isDefault = texture == entry)
        }

        // Update the texture list to show the new default
        updateTextureList()

        // Update metadata if needed
        resourceManager.saveTextureObjectType(entry.imageEntry.path, objectType)

        println("TextureManager: Marked '${entry.imageEntry.name}' as default for ${objectType.name}")
    }

    private fun updateTextureList() {
        val selectedIndex = textureList.selectedIndex // Preserve selection if possible
        textureListModel.clear()
        val selectedType = objectTypeComboBox.selectedItem as ObjectType
        texturesByType[selectedType]?.forEach { textureListModel.addElement(it) }
        if (selectedIndex != -1 && selectedIndex < textureListModel.size) {
            textureList.selectedIndex = selectedIndex
        } else {
            updatePreview() // Clear preview if selection is lost
        }
    }

    private fun updatePreview() {
        val selectedTexture = textureList.selectedValue
        if (selectedTexture != null) {
            val image = selectedTexture.imageEntry.image
            val scaledImage = image.getScaledInstance(150, 150, Image.SCALE_SMOOTH)
            previewLabel.icon = ImageIcon(scaledImage)
        } else {
            previewLabel.icon = null
        }
    }

    fun dumpTextureManagementState() {
        println("\n==== TEXTURE MANAGER DEBUG STATE ====")
        println("Total textures by type:")
        texturesByType.forEach { (type, textures) ->
            val defaultTexture = textures.find { it.isDefault }
            println("  ${type.name}: ${textures.size} textures" +
                    (defaultTexture?.let { " [Default: ${it.imageEntry.name}]" } ?: " [No default]"))

            // List all textures for this type
            textures.forEachIndexed { index, texture ->
                println("    ${index+1}. ${texture.imageEntry.name}" +
                        (if (texture.isDefault) " (DEFAULT)" else "") +
                        " | Path: ${texture.imageEntry.path}")
            }
        }

        // Check if any types have no textures
        allowedObjectTypes.forEach { type ->
            if (!texturesByType.containsKey(type) || texturesByType[type].isNullOrEmpty()) {
                println("  ${type.name}: No textures")
            }
        }
        println("=============================\n")
    }

    private fun loadTexturesFromResourceManager() {
        // Clear existing entries first
        texturesByType.clear()

        println("\nLoading textures from ResourceManager, total available: ${resourceManager.getAllImages().size}")

        for (entry in resourceManager.getAllImages()) {
            val (id, imageEntry) = entry

            println("Processing image: ${imageEntry.name} (ID: $id)")
            println("  Path: ${imageEntry.path}")

            // First try to get the object type from metadata
            var objectType = resourceManager.getTextureObjectType(imageEntry.path)

            if (objectType == null) {
            // Attempt to determine object type from file name patterns
                val filename = imageEntry.name.lowercase()
                objectType = when {
                    // Pattern matching logic as before
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
                            filename.contains("support") -> ObjectType.PILLAR

                    // WATER Keywords
                    filename.contains("water") || filename.contains("liquid") ||
                            filename.contains("fluid") -> ObjectType.WATER

                    // RAMP Keywords
                    filename.contains("ramp") || filename.contains("slope") ||
                            filename.contains("incline") -> ObjectType.RAMP

                    // PLAYER_SPAWN (Usually not textured, but handle if named)
                    filename.contains("spawn") || filename.contains("start") -> ObjectType.PLAYER_SPAWN

                    else -> objectTypeComboBox.selectedItem as ObjectType
                }

                // Save this guessed association for future use
                resourceManager.saveTextureObjectType(imageEntry.path, objectType)
                println("  Guessed type: $objectType (based on name)")
            }

            // Check if it's an allowed type
            if (allowedObjectTypes.contains(objectType)) {
                // CRITICAL: Check if the image is actually available before adding
                // This could be a source of the issue - missing image data
                val textureEntry = TextureEntry(objectType, imageEntry)
                texturesByType.getOrPut(objectType) { mutableListOf() }.add(textureEntry)
                println("  Added to ${objectType.name} texture list")
            } else if (objectType == ObjectType.PLAYER_SPAWN) {
                println("  NOTICE: PLAYER_SPAWN texture '${imageEntry.name}' not managed in panel")
            } else {
                println("  WARNING: Texture '${imageEntry.name}' type $objectType not in allowed list")
                // Assign to first allowed type as fallback
                val fallbackType = allowedObjectTypes.firstOrNull() ?: ObjectType.WALL
                val textureEntry = TextureEntry(fallbackType, imageEntry)
                texturesByType.getOrPut(fallbackType) { mutableListOf() }.add(textureEntry)
                println("  Added to ${fallbackType.name} texture list as fallback")
            }
        }

        // Log totals
        println("\nLoaded textures by type:")
        for (type in allowedObjectTypes) {
            val count = texturesByType[type]?.size ?: 0
            println("  ${type.name}: $count textures")
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

    // Inside TextureManagerPanel class
    fun refreshTextures() {
        println("==== TextureManagerPanel Refreshing Textures ====")
        // The existing method already clears and reloads from ResourceManager
        loadTexturesFromResourceManager()
        dumpTextureManagementState() // Dump state after refresh
        println("==== TextureManagerPanel Refresh Complete ====")

        // Optional: Select the first item or maintain selection if possible
        if (textureListModel.size > 0) {
            textureList.selectedIndex = 0
        }
        updatePreview()
    }
}