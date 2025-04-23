package texturemanager

import ui.topbar.FileManager
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

class ProjectAssetsDialog(
    parent: JFrame?,
    private val fileManager: FileManager
) : JDialog(parent, "Project Assets", true) { // Modal dialog

    private lateinit var tree: JTree
    private lateinit var treeModel: DefaultTreeModel

    init {
        // Check if a project is actually loaded
        val projectDir = fileManager.getProjectDirectory()

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
            // Setup the dialog only if project exists
            setupUI(projectDir)
        }
    }

    private fun setupUI(projectDir: File) {
        size = Dimension(500, 600)
        setLocationRelativeTo(parent)
        layout = BorderLayout()

        // Create the root node (project name)
        val rootNode = DefaultMutableTreeNode(projectDir.name)
        treeModel = DefaultTreeModel(rootNode)

        // Populate the tree recursively
        populateTree(rootNode, projectDir)

        tree = JTree(treeModel).apply {
            showsRootHandles = true
            isEditable = false // Make tree read-only
            selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
            // Optional: Add a cell renderer for icons etc. later if needed
        }

        val scrollPane = JScrollPane(tree)
        add(scrollPane, BorderLayout.CENTER)

        // Add a close button
        val closeButton = JButton("Close")
        closeButton.addActionListener { dispose() }
        val buttonPanel = JPanel()
        buttonPanel.add(closeButton)
        add(buttonPanel, BorderLayout.SOUTH)
    }

    private fun populateTree(parentNode: DefaultMutableTreeNode, currentFile: File) {
        if (!currentFile.isDirectory) return // Should not happen if starting with dir, but safety check

        val files = currentFile.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) // Sort directories first, then alphabetically

        files?.forEach { file ->
            val node = DefaultMutableTreeNode(file.name)
            parentNode.add(node)
            if (file.isDirectory) {
                // Recursively populate subdirectories
                populateTree(node, file)
            }
            // else: it's a file, node is added, recursion stops here
        }
    }
}