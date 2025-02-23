package ui

import ImageEntry
import ObjectType
import java.awt.*
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.filechooser.FileNameExtensionFilter

class TextureManagerPanel(private val resourceManager: ResourceManager) : JPanel() {
    private val objectTypeComboBox = JComboBox(ObjectType.values())
    private val textureListModel = DefaultListModel<TextureEntry>()
    private val textureList = JList(textureListModel)
    private val previewLabel = JLabel()

    // Map to store textures by object type
    private val texturesByType = mutableMapOf<ObjectType, MutableList<TextureEntry>>()

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
            fixedCellHeight = 25
            cellRenderer = createTextureListRenderer()
        }

        val scrollPane = JScrollPane(textureList).apply {
            preferredSize = Dimension(0, 120)
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
                preferredSize = Dimension(100, 100)
                background = Color(50, 54, 62)
                layout = BorderLayout()
                add(previewLabel, BorderLayout.CENTER)
            })
        }

        // Buttons
        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            alignmentX = LEFT_ALIGNMENT

            add(createButton("Add Texture") { addTexture() })
            add(Box.createVerticalStrut(5))
            add(createButton("Remove Texture") { removeSelectedTexture() })
            add(Box.createVerticalStrut(5))
            add(createButton("Set as Default") { setAsDefault() })
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
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                background = if (isSelected) Color(100, 100, 255) else Color(50, 54, 62)
                foreground = Color.WHITE

                val entry = value as? TextureEntry
                text = if (entry?.isDefault == true) {
                    "${entry.imageEntry.name} (Default)"
                } else {
                    entry?.imageEntry?.name ?: ""
                }

                return this
            }
        }
    }

    private fun addTexture() {
        val fileChooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif")
        }

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            val image = ImageIcon(file.path).image
            val imageEntry = ImageEntry(file.name, file.path, image)

            // Add to resource manager
            val id = resourceManager.addImage(file.name, file.path, image)

            // Create texture entry
            val selectedType = objectTypeComboBox.selectedItem as ObjectType
            val textureEntry = TextureEntry(selectedType, imageEntry)

            // Add to type-specific list
            texturesByType.getOrPut(selectedType) { mutableListOf() }.add(textureEntry)

            updateTextureList()
        }
    }

    private fun removeSelectedTexture() {
        val selectedEntry = textureList.selectedValue as? TextureEntry ?: return
        texturesByType[selectedEntry.objectType]?.remove(selectedEntry)
        updateTextureList()
    }

    private fun setAsDefault() {
        val selectedEntry = textureList.selectedValue as? TextureEntry ?: return
        val objectType = selectedEntry.objectType

        // Remove default flag from all textures of this type
        texturesByType[objectType]?.forEach { entry ->
            texturesByType[objectType]?.remove(entry)
            texturesByType[objectType]?.add(entry.copy(isDefault = false))
        }

        // Add the selected texture with default flag
        texturesByType[objectType]?.remove(selectedEntry)
        texturesByType[objectType]?.add(selectedEntry.copy(isDefault = true))

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
            val scaledImage = image.getScaledInstance(100, 100, Image.SCALE_SMOOTH)
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
}