package texturemanager

import ui.topbar.FileManager
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder
import javax.swing.event.TreeSelectionEvent
import javax.swing.filechooser.FileSystemView
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel
import java.text.SimpleDateFormat
import java.util.*

class ProjectAssetsDialog(
    parent: JFrame?,
    private val fileManager: FileManager
) : JDialog(parent, "Project Assets", true) {

    private lateinit var tree: JTree
    private lateinit var treeModel: DefaultTreeModel
    private lateinit var previewPanel: JPanel
    private lateinit var detailsPanel: JPanel
    private lateinit var filterComboBox: JComboBox<String>
    private lateinit var searchField: JTextField
    private val projectDir: File?

    // Asset file extensions
    private val imageExtensions = listOf("png", "jpg", "jpeg", "gif", "bmp")
    private val audioExtensions = listOf("wav", "mp3", "ogg")
    private val modelExtensions = listOf("obj", "fbx", "blend")
    private val scriptExtensions = listOf("kt", "java", "json", "xml")

    init {
        projectDir = fileManager.getProjectDirectory()

        if (projectDir == null || !projectDir.exists() || !projectDir.isDirectory) {
            // Handle case where no project is loaded or directory is invalid
            JOptionPane.showMessageDialog(
                parent,
                "No active project found or project directory is invalid.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
            // Dispose the dialog immediately if created inappropriately
            SwingUtilities.invokeLater { dispose() }
        } else {
            setupUI()
        }
    }

    private fun setupUI() {
        size = Dimension(900, 700)
        setLocationRelativeTo(parent)
        layout = BorderLayout(5, 5)
        background = Color(40, 44, 52)

        // Setup components
        setupFilterPanel()
        setupTreePanel()
        setupDetailsPanel()
        setupPreviewPanel()

        // Split panes for layout
        val leftPanel = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
            topComponent = JScrollPane(tree).apply {
                border = BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color(70, 73, 75)),
                    "Project Structure",
                    TitledBorder.LEFT,
                    TitledBorder.TOP,
                    null,
                    Color.WHITE
                )
            }
            bottomComponent = detailsPanel
            dividerLocation = 400
            border = EmptyBorder(5, 5, 5, 0)
        }

        val mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            leftComponent = leftPanel
            rightComponent = previewPanel
            dividerLocation = 400
            border = EmptyBorder(5, 5, 5, 5)
        }

        // Add components to dialog
        add(mainSplitPane, BorderLayout.CENTER)

        // Add a close button
        val closeButton = createButton("Close").apply {
            addActionListener { dispose() }
        }
        val buttonPanel = JPanel().apply {
            background = Color(40, 44, 52)
            add(closeButton)
        }
        add(buttonPanel, BorderLayout.SOUTH)
    }

    private fun setupFilterPanel() {
        val filterPanel = JPanel(BorderLayout(5, 0)).apply {
            background = Color(40, 44, 52)
            border = EmptyBorder(5, 5, 5, 5)
        }

        // Filter combo box
        filterComboBox = JComboBox(arrayOf("All Files", "Images", "Audio", "3D Models", "Scripts")).apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            addActionListener {
                refreshTree()
            }
        }

        // Search field
        searchField = JTextField().apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            columns = 15
            addActionListener {
                refreshTree()
            }
        }

        val searchButton = createButton("Search").apply {
            addActionListener {
                refreshTree()
            }
        }

        filterPanel.add(JLabel("Filter:").apply { foreground = Color.WHITE }, BorderLayout.WEST)
        filterPanel.add(filterComboBox, BorderLayout.CENTER)

        val searchPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            background = Color(40, 44, 52)
            add(JLabel("Search:").apply { foreground = Color.WHITE })
            add(searchField)
            add(searchButton)
        }

        filterPanel.add(searchPanel, BorderLayout.EAST)
        add(filterPanel, BorderLayout.NORTH)
    }

    private fun setupTreePanel() {
        // Create the root node
        val rootNode = DefaultMutableTreeNode(projectDir?.name ?: "Project")
        treeModel = DefaultTreeModel(rootNode)

        // Populate the tree
        populateTree(rootNode, projectDir!!)

        // Create the tree with custom renderer
        tree = JTree(treeModel).apply {
            showsRootHandles = true
            isEditable = false
            background = Color(50, 54, 62)
            foreground = Color.WHITE
            selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION

            // Custom cell renderer with file icons
            setCellRenderer(object : DefaultTreeCellRenderer() {
                init {
                    textSelectionColor = Color.WHITE
                    textNonSelectionColor = Color.WHITE
                    backgroundSelectionColor = Color(100, 100, 255)
                    backgroundNonSelectionColor = Color(50, 54, 62)
                    borderSelectionColor = Color(70, 70, 200)
                }

                override fun getTreeCellRendererComponent(
                    tree: JTree?,
                    value: Any?,
                    selected: Boolean,
                    expanded: Boolean,
                    leaf: Boolean,
                    row: Int,
                    hasFocus: Boolean
                ): Component {
                    super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)

                    val node = value as DefaultMutableTreeNode
                    val userObject = node.userObject

                    if (userObject is FileNode) {
                        val file = userObject.file
                        text = userObject.displayName

                        // Set icon based on file type
                        if (file.isDirectory) {
                            icon = if (expanded)
                                UIManager.getIcon("FileView.directoryOpenIcon")
                            else
                                UIManager.getIcon("FileView.directoryClosedIcon")
                        } else {
                            val fileType = file.extension.lowercase()
                            icon = when {
                                imageExtensions.contains(fileType) -> UIManager.getIcon("FileView.imageFileIcon")
                                audioExtensions.contains(fileType) -> UIManager.getIcon("FileView.fileIcon")
                                modelExtensions.contains(fileType) -> UIManager.getIcon("FileView.fileIcon")
                                scriptExtensions.contains(fileType) -> UIManager.getIcon("FileView.textFileIcon")
                                else -> FileSystemView.getFileSystemView().getSystemIcon(file)
                            }
                        }
                    }
                    return this
                }
            })

            // Selection listener
            addTreeSelectionListener { e: TreeSelectionEvent ->
                val node = lastSelectedPathComponent as? DefaultMutableTreeNode
                val userObject = node?.userObject

                if (userObject is FileNode) {
                    updatePreview(userObject.file)
                    updateDetails(userObject.file)
                } else {
                    clearPreview()
                    clearDetails()
                }
            }

            // Double-click to open file
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        val node = lastSelectedPathComponent as? DefaultMutableTreeNode
                        val userObject = node?.userObject

                        if (userObject is FileNode && !userObject.file.isDirectory) {
                            try {
                                Desktop.getDesktop().open(userObject.file)
                            } catch (ex: Exception) {
                                JOptionPane.showMessageDialog(
                                    this@ProjectAssetsDialog,
                                    "Could not open file: ${ex.message}",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE
                                )
                            }
                        }
                    }
                }
            })
        }
    }

    private fun setupDetailsPanel() {
        detailsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 44, 52)
            border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color(70, 73, 75)),
                "File Details",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                Color.WHITE
            )

            // Details will be populated when a file is selected
            add(createDetailItem("Name:"))
            add(createDetailItem("Type:"))
            add(createDetailItem("Size:"))
            add(createDetailItem("Path:"))
            add(createDetailItem("Modified:"))
        }
    }

    private fun setupPreviewPanel() {
        previewPanel = JPanel(BorderLayout()).apply {
            background = Color(40, 44, 52)
            border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color(70, 73, 75)),
                "Preview",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                Color.WHITE
            )

            // Default message when no file selected
            add(JLabel("Select a file to preview", SwingConstants.CENTER).apply {
                foreground = Color.WHITE
            }, BorderLayout.CENTER)
        }
    }

    private fun updatePreview(file: File) {
        previewPanel.removeAll()

        if (!file.isDirectory) {
            val extension = file.extension.lowercase()

            when {
                imageExtensions.contains(extension) -> {
                    try {
                        val image = ImageIO.read(file)
                        if (image != null) {
                            val previewImage = createScaledImage(image, 400, 400)
                            val imageLabel = JLabel(ImageIcon(previewImage))
                            imageLabel.horizontalAlignment = SwingConstants.CENTER

                            previewPanel.add(JScrollPane(imageLabel).apply {
                                background = Color(30, 33, 40)
                                border = null
                            }, BorderLayout.CENTER)

                            previewPanel.add(JLabel("Image dimensions: ${image.width}x${image.height}").apply {
                                foreground = Color.WHITE
                                horizontalAlignment = SwingConstants.CENTER
                            }, BorderLayout.SOUTH)
                        } else {
                            showPreviewPlaceholder("Could not load image")
                        }
                    } catch (e: Exception) {
                        showPreviewPlaceholder("Error loading image: ${e.message}")
                    }
                }
                audioExtensions.contains(extension) -> {
                    showPreviewPlaceholder("Audio file: ${file.name}")
                }
                modelExtensions.contains(extension) -> {
                    showPreviewPlaceholder("3D Model file: ${file.name}")
                }
                else -> {
                    // For text-based files, we could add a simple text preview
                    if (file.length() < 10_000) { // Only preview small files
                        try {
                            val text = file.readText()
                            val textArea = JTextArea(text).apply {
                                isEditable = false
                                background = Color(30, 33, 40)
                                foreground = Color.WHITE
                                font = Font(Font.MONOSPACED, Font.PLAIN, 12)
                            }
                            previewPanel.add(JScrollPane(textArea), BorderLayout.CENTER)
                        } catch (e: Exception) {
                            showPreviewPlaceholder("File not previewable")
                        }
                    } else {
                        showPreviewPlaceholder("File too large to preview")
                    }
                }
            }
        } else {
            showPreviewPlaceholder("Directory: ${file.name}")
        }

        previewPanel.revalidate()
        previewPanel.repaint()
    }

    private fun updateDetails(file: File) {
        // Find components in details panel
        val components = detailsPanel.components

        if (components.size >= 5) {
            (components[0] as JLabel).text = "Name: ${file.name}"

            val fileType = if (file.isDirectory) "Directory" else {
                val extension = file.extension.lowercase()
                when {
                    imageExtensions.contains(extension) -> "Image (.$extension)"
                    audioExtensions.contains(extension) -> "Audio (.$extension)"
                    modelExtensions.contains(extension) -> "3D Model (.$extension)"
                    scriptExtensions.contains(extension) -> "Script (.$extension)"
                    else -> "File (.$extension)"
                }
            }
            (components[1] as JLabel).text = "Type: $fileType"

            val size = if (file.isDirectory) {
                val fileCount = file.listFiles()?.size ?: 0
                "$fileCount items"
            } else {
                formatFileSize(file.length())
            }
            (components[2] as JLabel).text = "Size: $size"

            // Get relative path from project root
            val relativePath = projectDir?.let {
                file.absolutePath.removePrefix(it.absolutePath).replace("\\", "/")
            } ?: file.absolutePath
            (components[3] as JLabel).text = "Path: $relativePath"

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            (components[4] as JLabel).text = "Modified: ${dateFormat.format(Date(file.lastModified()))}"
        }
    }

    private fun clearPreview() {
        previewPanel.removeAll()
        showPreviewPlaceholder("Select a file to preview")
        previewPanel.revalidate()
        previewPanel.repaint()
    }

    private fun clearDetails() {
        val components = detailsPanel.components

        if (components.size >= 5) {
            (components[0] as JLabel).text = "Name:"
            (components[1] as JLabel).text = "Type:"
            (components[2] as JLabel).text = "Size:"
            (components[3] as JLabel).text = "Path:"
            (components[4] as JLabel).text = "Modified:"
        }
    }

    private fun showPreviewPlaceholder(message: String) {
        previewPanel.add(JLabel(message, SwingConstants.CENTER).apply {
            foreground = Color.WHITE
        }, BorderLayout.CENTER)
    }

    private fun createScaledImage(image: Image, maxWidth: Int, maxHeight: Int): Image {
        val width = image.getWidth(null)
        val height = image.getHeight(null)

        if (width <= maxWidth && height <= maxHeight) {
            return image // No need to scale
        }

        val ratio = width.toDouble() / height.toDouble()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxWidth
            newHeight = (maxWidth / ratio).toInt()
        } else {
            newHeight = maxHeight
            newWidth = (maxHeight * ratio).toInt()
        }

        return image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)
    }

    private fun createDetailItem(text: String): JLabel {
        return JLabel(text).apply {
            foreground = Color.WHITE
            font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
            alignmentX = Component.LEFT_ALIGNMENT
            border = EmptyBorder(3, 5, 3, 5)
        }
    }

    private fun createButton(text: String): JButton {
        return JButton(text).apply {
            background = Color(60, 63, 65)
            foreground = Color.WHITE
            isFocusPainted = false
        }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }

    private fun populateTree(parentNode: DefaultMutableTreeNode, directory: File) {
        val filteredFiles = getFilteredFiles(directory)

        filteredFiles.forEach { file ->
            val fileNode = FileNode(file)
            val node = DefaultMutableTreeNode(fileNode)
            parentNode.add(node)
            if (file.isDirectory) {
                // Recursively populate subdirectories
                populateTree(node, file)
            }
        }
    }

    private fun getFilteredFiles(directory: File): List<File> {
        val files = directory.listFiles() ?: return emptyList()

        // Apply file type filter
        val filteredByType = when (filterComboBox.selectedItem) {
            "Images" -> files.filter {
                it.isDirectory || imageExtensions.contains(it.extension.lowercase())
            }
            "Audio" -> files.filter {
                it.isDirectory || audioExtensions.contains(it.extension.lowercase())
            }
            "3D Models" -> files.filter {
                it.isDirectory || modelExtensions.contains(it.extension.lowercase())
            }
            "Scripts" -> files.filter {
                it.isDirectory || scriptExtensions.contains(it.extension.lowercase())
            }
            else -> files.toList()
        }

        // Apply search filter if text is present
        val searchText = searchField.text.trim().lowercase()
        val filteredBySearch = if (searchText.isNotEmpty()) {
            filteredByType.filter { it.name.lowercase().contains(searchText) }
        } else {
            filteredByType
        }

        // Sort directories first, then by name
        return filteredBySearch.sortedWith(
            compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }
        )
    }

    private fun refreshTree() {
        val rootNode = DefaultMutableTreeNode(projectDir?.name ?: "Project")
        treeModel = DefaultTreeModel(rootNode)
        populateTree(rootNode, projectDir!!)
        tree.model = treeModel

        // Expand root node
        val path = tree.getPathForRow(0)
        tree.expandPath(path)
    }

    // Helper class to store file reference with the node
    private class FileNode(val file: File) {
        val displayName: String = file.name

        override fun toString(): String = displayName
    }
}