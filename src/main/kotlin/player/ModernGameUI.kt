package player

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.RadialGradientPaint
import java.awt.LinearGradientPaint
import java.awt.MultipleGradientPaint
import java.awt.geom.RoundRectangle2D
import java.awt.geom.Arc2D
import java.awt.image.BufferedImage
import java.awt.RenderingHints
import java.awt.geom.Point2D
import java.awt.Polygon
import kotlin.math.sin
import kotlin.math.PI

/**
 * A UI component that renders a modern FPS HUD inspired by
 * contemporary shooters with a minimalist, sleek design.
 */
class ModernGameUI {
    // UI color constants - modern color palette
    private val primaryColor = Color(0, 174, 239)        // Bright cyan blue
    private val secondaryColor = Color(255, 89, 94)      // Coral red for health
    private val accentColor = Color(255, 202, 58)        // Gold for important info
    private val backgroundColor = Color(0, 0, 0, 160)    // Semi-transparent black
    private val textColor = Color(255, 255, 255, 220)    // Off-white text
    private val warningColor = Color(255, 100, 100)      // Red for low health/ammo warning

    // UI dimensions and layout constants
    private val cornerRadius = 15f
    private val elementPadding = 20
    private val healthBarHeight = 8
    private val ammoBarHeight = 6
    private val iconSize = 32

    // Animated effects timing
    private var time = 0.0
    private val pulseSpeed = 3.0

    // Pre-rendered icon cache
    private var weaponIcons = mutableMapOf<Int, BufferedImage>()
    private var healthIcon: BufferedImage? = null
    private var armorIcon: BufferedImage? = null
    private var ammoIcon: BufferedImage? = null

    // Current state
    private var currentHealth = 100
    private var maxHealth = 100
    private var currentArmor = 75
    private var maxArmor = 100
    private var currentAmmo = 48
    private var maxAmmo = 60
    private var currentWeapon = 2
    private var kills = 14
    private var maxKills = 45
    private var secrets = 3
    private var maxSecrets = 12

    init {
        // Initialize icons
        createIcons()
    }

    /**
     * Updates the game time used for animations
     * @param deltaTime Time since last frame in seconds
     */
    fun update(deltaTime: Double) {
        time += deltaTime
        // We could update more dynamic elements here
    }

    /**
     * Sets the player's current health for display
     */
    fun setHealth(current: Int, max: Int) {
        currentHealth = current
        maxHealth = max
    }

    /**
     * Sets the player's current armor for display
     */
    fun setArmor(current: Int, max: Int) {
        currentArmor = current
        maxArmor = max
    }

    /**
     * Sets the player's current ammo for display
     */
    fun setAmmo(current: Int, max: Int) {
        currentAmmo = current
        maxAmmo = max
    }

    /**
     * Sets the current weapon
     */
    fun setCurrentWeapon(weapon: Int) {
        currentWeapon = weapon
    }

    /**
     * Sets player stats
     */
    fun setStats(kills: Int, maxKills: Int, secrets: Int, maxSecrets: Int) {
        this.kills = kills
        this.maxKills = maxKills
        this.secrets = secrets
        this.maxSecrets = maxSecrets
    }

    /**
     * Creates pre-rendered icons for the UI
     */
    private fun createIcons() {
        // Create health icon
        healthIcon = BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB)
        val g = healthIcon!!.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Draw a modern medical cross
        g.color = secondaryColor
        g.fillRoundRect(iconSize/3, iconSize/6, iconSize/3, iconSize*2/3, 4, 4)
        g.fillRoundRect(iconSize/6, iconSize/3, iconSize*2/3, iconSize/3, 4, 4)
        g.dispose()

        // Create armor icon
        armorIcon = BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB)
        val g2 = armorIcon!!.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Draw a shield shape
        g2.color = primaryColor
        val shield = Polygon()
        shield.addPoint(iconSize/2, 2)
        shield.addPoint(iconSize-4, iconSize/3)
        shield.addPoint(iconSize-4, iconSize*2/3)
        shield.addPoint(iconSize/2, iconSize-2)
        shield.addPoint(4, iconSize*2/3)
        shield.addPoint(4, iconSize/3)
        g2.fillPolygon(shield)

        // Inner detail
        g2.color = Color(primaryColor.red, primaryColor.green, primaryColor.blue, 160)
        val innerShield = Polygon()
        innerShield.addPoint(iconSize/2, 8)
        innerShield.addPoint(iconSize-10, iconSize/3)
        innerShield.addPoint(iconSize-10, iconSize*2/3)
        innerShield.addPoint(iconSize/2, iconSize-8)
        innerShield.addPoint(10, iconSize*2/3)
        innerShield.addPoint(10, iconSize/3)
        g2.fillPolygon(innerShield)
        g2.dispose()

        // Create ammo icon
        ammoIcon = BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB)
        val g3 = ammoIcon!!.createGraphics()
        g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Draw a bullet shape
        g3.color = accentColor
        g3.fillRoundRect(iconSize/3, 4, iconSize/3, iconSize/2, 6, 6)
        g3.fillRect(iconSize/3, iconSize/3, iconSize/3, iconSize/3)

        // Bullet base
        g3.color = Color(180, 180, 180)
        g3.fillRoundRect(iconSize/3, iconSize/2, iconSize/3, iconSize/3, 2, 2)
        g3.dispose()

        // Generate weapon icons
        for (i in 1..5) {
            weaponIcons[i] = createWeaponIcon(i)
        }
    }

    /**
     * Creates a unique icon for each weapon
     */
    private fun createWeaponIcon(weaponNum: Int): BufferedImage {
        val img = BufferedImage(iconSize*2, iconSize, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        when (weaponNum) {
            1 -> { // Pistol shape
                g.color = Color(180, 180, 180)
                g.fillRoundRect(iconSize/2, iconSize/4, iconSize, iconSize/3, 4, 4)
                g.fillRoundRect(iconSize/2, iconSize/4, iconSize/4, iconSize/2, 4, 4)
                g.fillRoundRect(iconSize/2, iconSize*2/3, iconSize*3/4, iconSize/6, 4, 4)
            }
            2 -> { // Shotgun shape
                g.color = Color(180, 180, 180)
                g.fillRoundRect(iconSize/4, iconSize/3, iconSize*3/2, iconSize/4, 4, 4)
                g.fillRoundRect(iconSize/4, iconSize*2/3, iconSize/3, iconSize/6, 4, 4)
            }
            3 -> { // Machine gun shape
                g.color = Color(180, 180, 180)
                g.fillRoundRect(iconSize/4, iconSize/3, iconSize*3/2, iconSize/5, 4, 4)
                g.fillRoundRect(iconSize/4, iconSize/2, iconSize, iconSize/4, 4, 4)

                // Bullets
                g.color = accentColor
                g.fillRect(iconSize/4, iconSize*3/4, iconSize/6, iconSize/8)
                g.fillRect(iconSize/4 + iconSize/5, iconSize*3/4, iconSize/6, iconSize/8)
            }
            4 -> { // Rocket launcher shape
                g.color = Color(180, 180, 180)
                g.fillRoundRect(iconSize/4, iconSize/3, iconSize*3/2, iconSize/3, 4, 4)

                // Rocket
                g.color = Color(200, 80, 80)
                g.fillRoundRect(iconSize*3/2, iconSize/3, iconSize/3, iconSize/3, 8, 8)
            }
            5 -> { // Plasma/BFG shape
                g.color = Color(100, 100, 180)
                g.fillRoundRect(iconSize/4, iconSize/4, iconSize, iconSize/2, 8, 8)
                g.fillRoundRect(iconSize, iconSize/3, iconSize/2, iconSize/3, 6, 6)

                // Energy core
                val centerX = iconSize/2 + iconSize/4
                val centerY = iconSize/2
                val radius = iconSize/5

                val gp = RadialGradientPaint(
                    centerX.toFloat(), centerY.toFloat(), radius.toFloat(),
                    floatArrayOf(0f, 1f),
                    arrayOf(Color(0, 220, 255, 200), Color(0, 100, 200, 50))
                )
                g.paint = gp
                g.fillOval(centerX - radius, centerY - radius, radius*2, radius*2)
            }
        }
        g.dispose()
        return img
    }

    /**
     * Renders the UI to the screen
     */
    fun render(g2: Graphics2D, width: Int, height: Int) {
        // Save original settings
        val originalComposite = g2.composite
        val originalStroke = g2.stroke
        val originalFont = g2.font

        // Enable anti-aliasing for smoother graphics
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Calculate positions for UI elements
        val leftPanelWidth = 300
        val rightPanelWidth = 300
        val bottomPanelHeight = 80
        val panelMargin = 20

        // Draw bottom panel for weapon selection
        drawModernPanel(g2,
            width/2 - 150, height - bottomPanelHeight - panelMargin,
            300, bottomPanelHeight
        )
        renderWeaponSelector(g2,
            width/2 - 140, height - bottomPanelHeight - panelMargin + 10,
            280, bottomPanelHeight - 20
        )

        // Draw left panel for health and armor
        drawModernPanel(g2,
            panelMargin, height - bottomPanelHeight - panelMargin - 120,
            leftPanelWidth, 120
        )
        renderHealthAndArmor(g2,
            panelMargin + 10, height - bottomPanelHeight - panelMargin - 120 + 10,
            leftPanelWidth - 20, 100
        )

        // Draw right panel for ammo
        drawModernPanel(g2,
            width - rightPanelWidth - panelMargin, height - bottomPanelHeight - panelMargin - 120,
            rightPanelWidth, 120
        )
        renderAmmo(g2,
            width - rightPanelWidth - panelMargin + 10, height - bottomPanelHeight - panelMargin - 120 + 10,
            rightPanelWidth - 20, 100
        )

        // Draw stats panel in top right
        drawModernPanel(g2,
            width - 180 - panelMargin, panelMargin,
            180, 100
        )
        renderStats(g2,
            width - 180 - panelMargin + 10, panelMargin + 10,
            160, 80
        )

        // Restore original settings
        g2.composite = originalComposite
        g2.stroke = originalStroke
        g2.font = originalFont
    }

    /**
     * Draws a modern panel with background
     */
    private fun drawModernPanel(g2: Graphics2D, x: Int, y: Int, width: Int, height: Int) {
        // Create semi-transparent background with rounded corners
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f)
        val panel = RoundRectangle2D.Float(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), cornerRadius, cornerRadius)

        // Draw shadow
        g2.color = Color(0, 0, 0, 100)
        g2.fill(RoundRectangle2D.Float(x.toFloat() + 3, y.toFloat() + 3, width.toFloat(), height.toFloat(), cornerRadius, cornerRadius))

        // Draw panel background with gradient
        val bgGradient = LinearGradientPaint(
            x.toFloat(), y.toFloat(),
            x.toFloat(), (y + height).toFloat(),
            floatArrayOf(0f, 1f),
            arrayOf(
                Color(40, 40, 40, 200),
                Color(20, 20, 20, 230)
            ),
            MultipleGradientPaint.CycleMethod.NO_CYCLE
        )
        g2.paint = bgGradient
        g2.fill(panel)

        // Draw panel border with glow effect for depth
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f)
        g2.stroke = BasicStroke(2f)
        val borderGradient = LinearGradientPaint(
            x.toFloat(), y.toFloat(),
            (x + width).toFloat(), (y + height).toFloat(),
            floatArrayOf(0f, 0.5f, 1f),
            arrayOf(
                Color(primaryColor.red, primaryColor.green, primaryColor.blue, 180),
                Color(200, 200, 200, 100),
                Color(primaryColor.red, primaryColor.green, primaryColor.blue, 180)
            ),
            MultipleGradientPaint.CycleMethod.NO_CYCLE
        )
        g2.paint = borderGradient
        g2.draw(panel)

        // Reset composite to fully opaque
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f)
    }

    /**
     * Renders health and armor information
     */
    private fun renderHealthAndArmor(g2: Graphics2D, x: Int, y: Int, width: Int, height: Int) {
        // Set up fonts
        val labelFont = Font("Rajdhani", Font.BOLD, 14)
        val valueFont = Font("Rajdhani", Font.BOLD, 24)

        // Calculate dynamic values
        val healthPercentage = currentHealth.toFloat() / maxHealth.toFloat()
        val armorPercentage = currentArmor.toFloat() / maxArmor.toFloat()

        // Health section
        g2.drawImage(healthIcon, x, y + 5, null)

        // Health label and value
        g2.font = labelFont
        g2.color = textColor
        g2.drawString("HEALTH", x + iconSize + 10, y + 20)

        // Health value with dynamic coloring based on percentage
        g2.font = valueFont
        val healthColor = when {
            healthPercentage < 0.3 -> {
                // Pulse effect for low health
                val alpha = (sin(time * pulseSpeed * PI) * 0.5 + 0.5) * 0.5 + 0.5
                Color(
                    warningColor.red,
                    warningColor.green,
                    warningColor.blue,
                    (255 * alpha).toInt()
                )
            }
            healthPercentage < 0.5 -> Color.ORANGE
            else -> secondaryColor
        }
        g2.color = healthColor
        g2.drawString("$currentHealth", x + iconSize + 10, y + 45)

        // Health bar background
        val healthBarY = y + 55
        g2.color = Color(0, 0, 0, 120)
        g2.fillRoundRect(x, healthBarY, width, healthBarHeight, 4, 4)

        // Health bar fill with gradient
        val healthGradient = LinearGradientPaint(
            x.toFloat(), healthBarY.toFloat(),
            (x + width).toFloat(), healthBarY.toFloat(),
            floatArrayOf(0f, 1f),
            arrayOf(
                Color(secondaryColor.red, secondaryColor.green / 2, secondaryColor.blue / 2),
                secondaryColor
            ),
            MultipleGradientPaint.CycleMethod.NO_CYCLE
        )
        g2.paint = healthGradient
        g2.fillRoundRect(x, healthBarY, (width * healthPercentage).toInt(), healthBarHeight, 4, 4)

        // Add segments to the health bar
        g2.color = Color(0, 0, 0, 80)
        val segmentWidth = width / 10
        for (i in 1 until 10) {
            val segX = x + (segmentWidth * i)
            g2.drawLine(segX, healthBarY, segX, healthBarY + healthBarHeight)
        }

        // Armor section
        g2.drawImage(armorIcon, x, y + healthBarY + healthBarHeight + 15, null)

        // Armor label and value
        g2.font = labelFont
        g2.color = textColor
        g2.drawString("ARMOR", x + iconSize + 10, y + healthBarY + healthBarHeight + 30)

        g2.font = valueFont
        g2.color = primaryColor
        g2.drawString("$currentArmor", x + iconSize + 10, y + healthBarY + healthBarHeight + 55)

        // Armor bar background
        val armorBarY = y + healthBarY + healthBarHeight + 65
        g2.color = Color(0, 0, 0, 120)
        g2.fillRoundRect(x, armorBarY, width, healthBarHeight, 4, 4)

        // Armor bar fill with gradient
        val armorGradient = LinearGradientPaint(
            x.toFloat(), armorBarY.toFloat(),
            (x + width).toFloat(), armorBarY.toFloat(),
            floatArrayOf(0f, 1f),
            arrayOf(
                Color(primaryColor.red / 2, primaryColor.green / 2, primaryColor.blue),
                primaryColor
            ),
            MultipleGradientPaint.CycleMethod.NO_CYCLE
        )
        g2.paint = armorGradient
        g2.fillRoundRect(x, armorBarY, (width * armorPercentage).toInt(), healthBarHeight, 4, 4)

        // Add segments to the armor bar
        g2.color = Color(0, 0, 0, 80)
        for (i in 1 until 10) {
            val segX = x + (segmentWidth * i)
            g2.drawLine(segX, armorBarY, segX, armorBarY + healthBarHeight)
        }
    }

    /**
     * Renders ammo information
     */
    private fun renderAmmo(g2: Graphics2D, x: Int, y: Int, width: Int, height: Int) {
        // Set up fonts
        val labelFont = Font("Rajdhani", Font.BOLD, 14)
        val valueFont = Font("Rajdhani", Font.BOLD, 32)

        // Calculate dynamic values
        val ammoPercentage = currentAmmo.toFloat() / maxAmmo.toFloat()

        // Ammo section
        g2.drawImage(ammoIcon, x, y + 5, null)

        // Ammo label
        g2.font = labelFont
        g2.color = textColor
        g2.drawString("AMMUNITION", x + iconSize + 10, y + 20)

        // Ammo value with dynamic coloring based on percentage
        g2.font = valueFont
        val ammoColor = when {
            ammoPercentage < 0.2 -> {
                // Pulse effect for low ammo
                val alpha = (sin(time * pulseSpeed * PI) * 0.5 + 0.5) * 0.5 + 0.5
                Color(
                    warningColor.red,
                    warningColor.green,
                    warningColor.blue,
                    (255 * alpha).toInt()
                )
            }
            ammoPercentage < 0.4 -> Color.ORANGE
            else -> accentColor
        }
        g2.color = ammoColor
        g2.drawString("$currentAmmo", x + iconSize + 10, y + 55)

        // Small max ammo indicator
        g2.font = labelFont
        g2.color = Color(200, 200, 200, 150)
        val maxAmmoString = "/ $maxAmmo"
        g2.drawString(maxAmmoString, x + iconSize + 80, y + 55)

        // Ammo bar background
        val ammoBarY = y + 70
        g2.color = Color(0, 0, 0, 120)
        g2.fillRoundRect(x, ammoBarY, width, ammoBarHeight, 4, 4)

        // Ammo bar fill with gradient
        val ammoGradient = LinearGradientPaint(
            x.toFloat(), ammoBarY.toFloat(),
            (x + width).toFloat(), ammoBarY.toFloat(),
            floatArrayOf(0f, 1f),
            arrayOf(
                Color(accentColor.red, accentColor.green, accentColor.blue / 2),
                accentColor
            ),
            MultipleGradientPaint.CycleMethod.NO_CYCLE
        )
        g2.paint = ammoGradient
        g2.fillRoundRect(x, ammoBarY, (width * ammoPercentage).toInt(), ammoBarHeight, 4, 4)

        // Add bullet indicators to the ammo bar
        if (maxAmmo <= 100) { // Only show indicators if ammo count is reasonable
            g2.color = Color(0, 0, 0, 120)
            val bulletSpacing = width.toFloat() / maxAmmo
            for (i in 1 until maxAmmo) {
                val bulletX = x + (bulletSpacing * i).toInt()
                if (i % 5 == 0) { // Make every 5th line more visible
                    g2.drawLine(bulletX, ammoBarY - 1, bulletX, ammoBarY + ammoBarHeight + 1)
                } else {
                    g2.drawLine(bulletX, ammoBarY, bulletX, ammoBarY + ammoBarHeight)
                }
            }
        }
    }

    /**
     * Renders weapon selector
     */
    private fun renderWeaponSelector(g2: Graphics2D, x: Int, y: Int, width: Int, height: Int) {
        // Calculate weapon slot positions
        val slotWidth = width / 5
        val slotHeight = height

        // Draw each weapon slot
        for (i in 1..5) {
            val slotX = x + (i-1) * slotWidth
            val isSelected = i == currentWeapon

            // Draw selector highlight if this is the current weapon
            if (isSelected) {
                // Dynamic highlighted slot
                g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f)
                val highlight = RoundRectangle2D.Float(
                    slotX.toFloat(), y.toFloat(),
                    slotWidth.toFloat(), slotHeight.toFloat(),
                    8f, 8f
                )

                // Animated gradient for selected weapon
                val highlightGradient = LinearGradientPaint(
                    slotX.toFloat(), y.toFloat(),
                    (slotX + slotWidth).toFloat(), (y + slotHeight).toFloat(),
                    floatArrayOf(0f, 0.5f, 1f),
                    arrayOf(
                        Color(primaryColor.red, primaryColor.green, primaryColor.blue, 100),
                        Color(primaryColor.red, primaryColor.green, primaryColor.blue, 180),
                        Color(primaryColor.red, primaryColor.green, primaryColor.blue, 100)
                    ),
                    MultipleGradientPaint.CycleMethod.NO_CYCLE
                )
                g2.paint = highlightGradient
                g2.fill(highlight)

                // Reset composite
                g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f)
            }

            // Draw weapon icon
            val iconX = slotX + (slotWidth - iconSize*2) / 2
            val iconY = y + (slotHeight - iconSize) / 2 - 5
            g2.drawImage(weaponIcons[i], iconX, iconY, null)

            // Draw weapon number
            g2.font = Font("Rajdhani", Font.BOLD, 16)
            g2.color = if (isSelected) accentColor else textColor
            g2.drawString("$i", slotX + slotWidth/2 - 5, y + slotHeight - 8)
        }
    }

    /**
     * Renders game stats (kills, secrets, etc.)
     */
    private fun renderStats(g2: Graphics2D, x: Int, y: Int, width: Int, height: Int) {
        val labelFont = Font("Rajdhani", Font.BOLD, 12)
        val valueFont = Font("Rajdhani", Font.BOLD, 14)

        // Title
        g2.font = Font("Rajdhani", Font.BOLD, 14)
        g2.color = textColor
        g2.drawString("STATISTICS", x, y + 14)

        // Line separator
        g2.color = Color(primaryColor.red, primaryColor.green, primaryColor.blue, 150)
        g2.drawLine(x, y + 18, x + width, y + 18)

        // Stats grid - Kills and Secrets
        val colWidth = width / 2

        // Kills
        g2.font = labelFont
        g2.color = Color(200, 200, 200, 200)
        g2.drawString("KILLS", x + 5, y + 38)

        g2.font = valueFont
        g2.color = textColor
        val killsText = "$kills/$maxKills"
        g2.drawString(killsText, x + 5, y + 58)

        // Secrets
        g2.font = labelFont
        g2.color = Color(200, 200, 200, 200)
        g2.drawString("SECRETS", x + colWidth, y + 38)

        g2.font = valueFont
        g2.color = textColor
        val secretsText = "$secrets/$maxSecrets"
        g2.drawString(secretsText, x + colWidth, y + 58)

        // Quick time info/mission time
        val timeStr = formatTime(time.toLong())
        g2.font = valueFont
        g2.color = accentColor
        g2.drawString(timeStr, x + width - 65, y + 78)

        g2.font = labelFont
        g2.color = Color(200, 200, 200, 200)
        g2.drawString("MISSION TIME", x + 5, y + 78)
    }

    /**
     * Formats time as MM:SS
     */
    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
}