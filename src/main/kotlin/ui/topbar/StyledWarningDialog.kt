package ui.topbar // Or wherever you want to put it

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

class StyledWarningDialog(
    parent: Window?,
    title: String,
    message: String
) : JDialog(parent, title, ModalityType.APPLICATION_MODAL) {

    enum class Result { OK, CANCEL }
    var userChoice: Result = Result.CANCEL
        private set

    // Use colors consistent with ControlsDialog
    private val gradientStartColor = Color(30, 33, 40)
    private val gradientEndColor = Color(45, 48, 55)
    private val buttonBackgroundColor = Color(60, 63, 65)
    private val buttonHoverColor = Color(80, 83, 85)
    private val buttonBorderColor = Color(80, 83, 85)
    private val textColor = Color.WHITE
    private val accentColor = Color(220, 95, 60)

    init {
        layout = BorderLayout()
        isUndecorated = false

        // Create main panel with gradient background
        val mainPanel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                val gradientPaint = GradientPaint(
                    0f, 0f, gradientStartColor,
                    0f, height.toFloat(), gradientEndColor
                )
                g2d.paint = gradientPaint
                g2d.fillRect(0, 0, width, height)
            }
        }.apply {
            layout = BorderLayout(10, 10)
            border = EmptyBorder(20, 30, 20, 30)

            // Message Label
            val messageLabel = JLabel(message).apply {
                foreground = textColor
                font = Font("SansSerif", Font.PLAIN, 13)
                horizontalAlignment = SwingConstants.CENTER
            }
            val messagePanel = JPanel(FlowLayout(FlowLayout.CENTER)).apply {
                isOpaque = false
                add(messageLabel)
                border = EmptyBorder(10, 0, 10, 0)
            }
            add(messagePanel, BorderLayout.CENTER)

            // Button Panel
            val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 10, 0)).apply {
                isOpaque = false
                val okButton = createStyledButton("OK")
                okButton.addActionListener { handleResult(Result.OK) }

                val cancelButton = createStyledButton("Cancel")
                cancelButton.addActionListener { handleResult(Result.CANCEL) }

                add(okButton)
                add(cancelButton)
            }
            add(buttonPanel, BorderLayout.SOUTH)
        }

        contentPane = mainPanel

        pack()
        minimumSize = Dimension(400, 180)
        setLocationRelativeTo(parent)
    }

    private fun handleResult(result: Result) {
        userChoice = result
        dispose()
    }

    private fun createStyledButton(text: String): JButton {
        return JButton(text).apply {
            foreground = textColor
            background = buttonBackgroundColor
            font = Font("Arial", Font.BOLD, 12)
            preferredSize = Dimension(100, 30)
            isFocusPainted = false
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(buttonBorderColor),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
            )

            // Hover effect
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    background = buttonHoverColor
                }
                override fun mouseExited(e: MouseEvent) {
                    background = buttonBackgroundColor
                }
            })
        }
    }

    // Optional: Convenience function to show the dialog and get the result
    companion object {
        fun showWarning(parent: Window?, title: String, message: String): Result {
            val dialog = StyledWarningDialog(parent, title, message)
            dialog.isVisible = true // This blocks until the dialog is closed
            return dialog.userChoice
        }
    }
}