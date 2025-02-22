package ui

import java.awt.Color
import java.awt.Dimension
import java.awt.Image
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class ImageListModel : DefaultListModel<ImageEntry>()

data class ImageEntry(
    val name: String,
    val path: String,
    val image: Image
)

class ImageLoaderPanel : JPanel() {
    private val imageListModel = ImageListModel()
    private val imageList = JList(imageListModel).apply {
        background = Color(50, 54, 62)
        foreground = Color.WHITE
        selectionBackground = Color(100, 100, 255)
        selectionForeground = Color.WHITE
        visibleRowCount = 5
        fixedCellHeight = 25
    }

    private val addButton = JButton("Add Image").apply {
        background = Color(60, 63, 65)
        foreground = Color.WHITE
        isFocusPainted = false
        maximumSize = Dimension(Int.MAX_VALUE, 30)
    }

    private val removeButton = JButton("Remove Image").apply {
        background = Color(60, 63, 65)
        foreground = Color.WHITE
        isFocusPainted = false
        maximumSize = Dimension(Int.MAX_VALUE, 30)
    }

    private val scrollPane = JScrollPane(imageList).apply {
        preferredSize = Dimension(0, 120)
        border = BorderFactory.createLineBorder(Color(70, 73, 75))
        background = Color(40, 44, 52)
    }

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 44, 52)

        // Setup buttons panel
        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = Color(40, 44, 52)
            add(addButton)
            add(Box.createHorizontalStrut(5))
            add(removeButton)
        }

        // Configure list renderer
        imageList.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): java.awt.Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                background = if (isSelected) Color(100, 100, 255) else Color(50, 54, 62)
                foreground = Color.WHITE
                text = (value as? ImageEntry)?.name ?: ""
                return this
            }
        }

        // Add components
        add(buttonPanel)
        add(Box.createVerticalStrut(5))
        add(scrollPane)

        // Setup button actions
        addButton.addActionListener { addImage() }
        removeButton.addActionListener { removeSelectedImage() }
    }

    private fun addImage() {
        val fileChooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif")
        }

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            val image = ImageIcon(file.path).image
            val entry = ImageEntry(file.name, file.path, image)
            imageListModel.addElement(entry)
        }
    }

    private fun removeSelectedImage() {
        val selectedIndex = imageList.selectedIndex
        if (selectedIndex != -1) {
            imageListModel.remove(selectedIndex)
        }
    }

    fun getSelectedImage(): ImageEntry? {
        return imageList.selectedValue as? ImageEntry
    }
}