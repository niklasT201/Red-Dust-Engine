package texturemanager

import ui.topbar.FileManager
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
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
    fileManager: FileManager
) : JDialog(parent, "Project Assets", true) {

    private lateinit var tree: JTree
    private lateinit var treeModel: DefaultTreeModel
    private lateinit var previewPanel: JPanel
    private lateinit var detailsPanel: JPanel
    private lateinit var filterComboBox: JComboBox<String>
    private lateinit var searchField: JTextField
    private val projectDir: File?
    private var zoomFactor = 1.0
    private var currentImage: BufferedImage? = null
    private lateinit var imagePanel: JPanel

    // Main theme colors - matching the About dialog style
    private val BACKGROUND_COLOR = Color(40, 44, 52)
    private val PANEL_COLOR = Color(45, 48, 55)
    private val ACCENT_COLOR = Color(220, 95, 60) // Using your warm orange/red accent color
    private val BUTTON_COLOR = Color(60, 63, 65)
    private val BUTTON_HOVER_COLOR = Color(80, 83, 85)
    private val TEXT_COLOR = Color.WHITE
    private val SECONDARY_TEXT_COLOR = Color(180, 180, 180)

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

        // Set dialog background to use gradient like in About dialog
        contentPane = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                val gradientPaint = GradientPaint(
                    0f, 0f, Color(30, 33, 40),
                    0f, height.toFloat(), Color(45, 48, 55)
                )
                g2d.paint = gradientPaint
                g2d.fillRect(0, 0, width, height)
            }
        }.apply {
            layout = BorderLayout(5, 5)
        }

        // Setup components
        setupFilterPanel()
        setupTreePanel()
        setupDetailsPanel()
        setupPreviewPanel()

        // Split panes for layout
        val leftPanel = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
            topComponent = JScrollPane(tree).apply {
                border = createStyledBorder("Project Structure")
                background = PANEL_COLOR
                viewport.background = PANEL_COLOR
            }
            bottomComponent = detailsPanel
            dividerLocation = 400
            border = EmptyBorder(5, 5, 5, 0)
            background = BACKGROUND_COLOR
            dividerSize = 5
        }

        val mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            leftComponent = leftPanel
            rightComponent = previewPanel
            dividerLocation = 400
            border = EmptyBorder(5, 5, 5, 5)
            background = BACKGROUND_COLOR
            dividerSize = 5
        }

        // Add components to dialog
        contentPane.add(mainSplitPane, BorderLayout.CENTER)

        // Add a close button with style matching About dialog
        val closeButton = createStyledButton("Close").apply {
            addActionListener { dispose() }
        }

        val buttonPanel = JPanel().apply {
            isOpaque = false
            layout = FlowLayout(FlowLayout.CENTER)
            add(closeButton)
        }

        contentPane.add(buttonPanel, BorderLayout.SOUTH)
    }

    private fun setupFilterPanel() {
        val filterPanel = JPanel(BorderLayout(5, 0)).apply {
            isOpaque = false
            border = EmptyBorder(5, 5, 5, 5)
        }

        // Create a stylized title for the filter panel
        val titleLabel = JLabel("PROJECT ASSETS", SwingConstants.LEFT).apply {
            foreground = ACCENT_COLOR
            font = Font("Impact", Font.BOLD, 18)
            border = EmptyBorder(0, 5, 5, 0)
        }

        // Add stylized separator similar to About dialog
        val separator = createGlowingSeparator()

        val headerPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(titleLabel, BorderLayout.NORTH)
            add(separator, BorderLayout.CENTER)
        }

        // Filter combo box with custom style
        filterComboBox = JComboBox(arrayOf("All Files", "Images", "Audio", "3D Models", "Scripts")).apply {
            background = BUTTON_COLOR
            foreground = TEXT_COLOR
            font = Font("Arial", Font.PLAIN, 12)
            addActionListener {
                refreshTree()
            }
        }

        // Search field with custom style
        searchField = JTextField().apply {
            background = BUTTON_COLOR
            foreground = TEXT_COLOR
            font = Font("Arial", Font.PLAIN, 12)
            columns = 15
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(25, 28, 35)),
                BorderFactory.createEmptyBorder(3, 5, 3, 5)
            )
            addActionListener {
                refreshTree()
            }
        }

        val searchButton = createStyledButton("Search").apply {
            preferredSize = Dimension(80, 26)
            addActionListener {
                refreshTree()
            }
        }

        val filterControlsPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            isOpaque = false
            add(JLabel("Filter:").apply {
                foreground = TEXT_COLOR
                font = Font("Arial", Font.PLAIN, 12)
            })
            add(filterComboBox)
        }

        val searchPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            isOpaque = false
            add(JLabel("Search:").apply {
                foreground = TEXT_COLOR
                font = Font("Arial", Font.PLAIN, 12)
            })
            add(searchField)
            add(searchButton)
        }

        val controlsPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(filterControlsPanel, BorderLayout.WEST)
            add(searchPanel, BorderLayout.EAST)
        }

        filterPanel.add(headerPanel, BorderLayout.NORTH)
        filterPanel.add(controlsPanel, BorderLayout.CENTER)

        contentPane.add(filterPanel, BorderLayout.NORTH)
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
            background = PANEL_COLOR
            foreground = TEXT_COLOR
            font = Font("Arial", Font.PLAIN, 12)
            selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION

            // Custom cell renderer with file icons
            setCellRenderer(object : DefaultTreeCellRenderer() {
                init {
                    textSelectionColor = TEXT_COLOR
                    textNonSelectionColor = TEXT_COLOR
                    backgroundSelectionColor = ACCENT_COLOR.darker()
                    backgroundNonSelectionColor = PANEL_COLOR
                    borderSelectionColor = ACCENT_COLOR
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
            background = PANEL_COLOR
            border = createStyledBorder("File Details")

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
            background = PANEL_COLOR
            border = createStyledBorder("Preview")

            // Default message when no file selected
            val placeholderPanel = JPanel(BorderLayout()).apply {
                isOpaque = false
                add(JLabel("Select a file to preview", SwingConstants.CENTER).apply {
                    foreground = SECONDARY_TEXT_COLOR
                    font = Font("Arial", Font.ITALIC, 14)
                }, BorderLayout.CENTER)
            }
            add(placeholderPanel, BorderLayout.CENTER)
        }
    }

    private fun updatePreview(file: File) {
        previewPanel.removeAll()
        zoomFactor = 1.0 // Reset zoom when loading a new image

        // Header for preview panel showing filename
        val headerPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(JLabel(file.name).apply {
                foreground = ACCENT_COLOR
                font = Font("Arial", Font.BOLD, 14)
                border = EmptyBorder(0, 5, 5, 0)
            }, BorderLayout.WEST)
        }

        if (!file.isDirectory) {
            val extension = file.extension.lowercase()

            when {
                imageExtensions.contains(extension) -> {
                    try {
                        val image = ImageIO.read(file)
                        if (image != null) {
                            currentImage = image

                            // Create a panel to display the image with zoom functionality
                            imagePanel = object : JPanel(BorderLayout()) {
                                override fun paintComponent(g: Graphics) {
                                    super.paintComponent(g)
                                    val g2d = g as Graphics2D
                                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

                                    currentImage?.let {
                                        val scaledWidth = (it.width * zoomFactor).toInt()
                                        val scaledHeight = (it.height * zoomFactor).toInt()

                                        // Center the image in the panel
                                        val x = (width - scaledWidth) / 2
                                        val y = (height - scaledHeight) / 2

                                        g2d.drawImage(it, x, y, scaledWidth, scaledHeight, this)
                                    }
                                }

                                override fun getPreferredSize(): Dimension {
                                    return currentImage?.let {
                                        Dimension(
                                            (it.width * zoomFactor).toInt().coerceAtLeast(500),
                                            (it.height * zoomFactor).toInt().coerceAtLeast(400)
                                        )
                                    } ?: Dimension(500, 400)
                                }
                            }.apply {
                                background = Color(30, 33, 40)
                            }

                            // Add mouse wheel listener for zooming
                            imagePanel.addMouseWheelListener { e ->
                                if (e.wheelRotation < 0) {
                                    // Zoom in (scroll up)
                                    zoomFactor *= 1.1
                                } else {
                                    // Zoom out (scroll down)
                                    zoomFactor /= 1.1
                                }
                                // Limit zoom factor to reasonable range
                                zoomFactor = zoomFactor.coerceIn(0.1, 10.0)

                                // Update the panel size and repaint
                                imagePanel.revalidate()
                                imagePanel.repaint()
                            }

                            // Create a scroll pane for the image panel
                            val scrollPane = JScrollPane(imagePanel).apply {
                                background = Color(30, 33, 40)
                                border = null
                                viewport.background = Color(30, 33, 40)
                                horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                            }

                            previewPanel.add(headerPanel, BorderLayout.NORTH)
                            previewPanel.add(scrollPane, BorderLayout.CENTER)

                            // Add zoom info and image dimensions to footer
                            val infoPanel = JPanel(BorderLayout()).apply {
                                isOpaque = false
                                add(JLabel("Dimensions: ${image.width}x${image.height} | Zoom: 100%").apply {
                                    name = "zoomInfoLabel"  // Set a name so we can find and update it
                                    foreground = SECONDARY_TEXT_COLOR
                                    horizontalAlignment = SwingConstants.CENTER
                                    border = EmptyBorder(5, 0, 5, 0)
                                }, BorderLayout.CENTER)

                                // Add zoom controls
                                val controlPanel = JPanel(FlowLayout(FlowLayout.CENTER)).apply {
                                    isOpaque = false

                                    val zoomInButton = createStyledButton("+").apply {
                                        preferredSize = Dimension(40, 26)
                                        addActionListener {
                                            zoomFactor *= 1.25
                                            zoomFactor = zoomFactor.coerceIn(0.1, 10.0)
                                            imagePanel.revalidate()
                                            imagePanel.repaint()
                                            updateZoomLabel()
                                        }
                                    }

                                    val zoomOutButton = createStyledButton("-").apply {
                                        preferredSize = Dimension(40, 26)
                                        addActionListener {
                                            zoomFactor /= 1.25
                                            zoomFactor = zoomFactor.coerceIn(0.1, 10.0)
                                            imagePanel.revalidate()
                                            imagePanel.repaint()
                                            updateZoomLabel()
                                        }
                                    }

                                    val resetZoomButton = createStyledButton("1:1").apply {
                                        preferredSize = Dimension(40, 26)
                                        addActionListener {
                                            zoomFactor = 1.0
                                            imagePanel.revalidate()
                                            imagePanel.repaint()
                                            updateZoomLabel()
                                        }
                                    }

                                    add(zoomOutButton)
                                    add(resetZoomButton)
                                    add(zoomInButton)
                                }

                                add(controlPanel, BorderLayout.EAST)
                            }

                            previewPanel.add(infoPanel, BorderLayout.SOUTH)

                            // Add mouse listeners for panning (optional)
                            addPanningSupport(imagePanel, scrollPane)
                        } else {
                            showPreviewPlaceholder(headerPanel, "Could not load image")
                        }
                    } catch (e: Exception) {
                        showPreviewPlaceholder(headerPanel, "Error loading image: ${e.message}")
                    }
                }
                // Keep the rest of your when cases unchanged
                audioExtensions.contains(extension) -> {
                    showPreviewPlaceholder(headerPanel, "Audio file: ${file.name}")
                }
                modelExtensions.contains(extension) -> {
                    showPreviewPlaceholder(headerPanel, "3D Model file: ${file.name}")
                }
                else -> {
                    // For text-based files, we could add a simple text preview
                    if (file.length() < 10_000) { // Only preview small files
                        try {
                            val text = file.readText()
                            val textArea = JTextArea(text).apply {
                                isEditable = false
                                background = Color(30, 33, 40)
                                foreground = TEXT_COLOR
                                font = Font(Font.MONOSPACED, Font.PLAIN, 12)
                                caretColor = ACCENT_COLOR
                            }
                            previewPanel.add(headerPanel, BorderLayout.NORTH)
                            previewPanel.add(JScrollPane(textArea).apply {
                                border = null
                                viewport.background = Color(30, 33, 40)
                            }, BorderLayout.CENTER)
                        } catch (e: Exception) {
                            showPreviewPlaceholder(headerPanel, "File not previewable")
                        }
                    } else {
                        showPreviewPlaceholder(headerPanel, "File too large to preview")
                    }
                }
            }
        } else {
            showPreviewPlaceholder(headerPanel, "Directory: ${file.name}")
        }

        previewPanel.revalidate()
        previewPanel.repaint()
    }

    // Add these helper functions
    private fun updateZoomLabel() {
        val zoomPercent = (zoomFactor * 100).toInt()
        previewPanel.components.forEach { component ->
            if (component is JPanel) {
                component.components.forEach { subComponent ->
                    if (subComponent is JLabel && subComponent.name == "zoomInfoLabel") {
                        currentImage?.let {
                            subComponent.text = "Dimensions: ${it.width}x${it.height} | Zoom: $zoomPercent%"
                        }
                    }
                }
            }
        }
    }

    private fun addPanningSupport(imagePanel: JPanel, scrollPane: JScrollPane) {
        var lastPoint: Point? = null

        imagePanel.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                imagePanel.cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                lastPoint = e.point
            }

            override fun mouseReleased(e: MouseEvent) {
                imagePanel.cursor = Cursor.getDefaultCursor()
                lastPoint = null
            }
        })

        imagePanel.addMouseMotionListener(object : MouseAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                if (lastPoint != null) {
                    val viewPort = scrollPane.viewport
                    val viewRect = viewPort.viewRect

                    // Calculate how much to move
                    val deltaX = lastPoint!!.x - e.x
                    val deltaY = lastPoint!!.y - e.y

                    // Create a new position
                    val newX = viewRect.x + deltaX
                    val newY = viewRect.y + deltaY

                    // Scroll to the new position
                    val scrollTo = Point(newX, newY)
                    viewPort.viewPosition = scrollTo
                }
                lastPoint = e.point
            }
        })
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
        showPreviewPlaceholder(null, "Select a file to preview")
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

    private fun showPreviewPlaceholder(headerPanel: JPanel?, message: String) {
        if (headerPanel != null) {
            previewPanel.add(headerPanel, BorderLayout.NORTH)
        }

        previewPanel.add(JPanel(BorderLayout()).apply {
            isOpaque = false
            add(JLabel(message, SwingConstants.CENTER).apply {
                foreground = SECONDARY_TEXT_COLOR
                font = Font("Arial", Font.ITALIC, 14)
            }, BorderLayout.CENTER)
        }, BorderLayout.CENTER)
    }

    private fun createDetailItem(text: String): JLabel {
        return JLabel(text).apply {
            foreground = TEXT_COLOR
            font = Font("Arial", Font.PLAIN, 12)
            alignmentX = Component.LEFT_ALIGNMENT
            border = EmptyBorder(5, 8, 5, 8)
        }
    }

    private fun createStyledButton(text: String): JButton {
        return JButton(text).apply {
            foreground = TEXT_COLOR
            background = BUTTON_COLOR
            font = Font("Arial", Font.BOLD, 12)
            preferredSize = Dimension(120, 30)
            isFocusPainted = false
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(80, 83, 85)),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
            )

            // Hover effect
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    background = BUTTON_HOVER_COLOR
                }

                override fun mouseExited(e: MouseEvent) {
                    background = BUTTON_COLOR
                }
            })
        }
    }

    private fun createStyledBorder(title: String): javax.swing.border.Border {
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color(70, 73, 75)),
            title,
            TitledBorder.LEFT,
            TitledBorder.TOP,
            Font("Arial", Font.BOLD, 12),
            ACCENT_COLOR
        )
    }

    private fun createGlowingSeparator(): JPanel {
        return object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                // Draw glowing line
                val width = this.width
                val y = this.height / 2

                val gradient = LinearGradientPaint(
                    0f, y.toFloat(), width.toFloat(), y.toFloat(),
                    floatArrayOf(0.0f, 0.5f, 1.0f),
                    arrayOf(PANEL_COLOR, ACCENT_COLOR, PANEL_COLOR)
                )

                g2d.stroke = BasicStroke(2f)
                g2d.paint = gradient
                g2d.drawLine(0, y, width, y)
            }

            init {
                preferredSize = Dimension(1, 10)
                isOpaque = false
            }
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