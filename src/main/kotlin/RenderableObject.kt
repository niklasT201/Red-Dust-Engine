import java.awt.Color

// Combined class to store rendering info for both walls and floors
sealed class RenderableObject {
    abstract val distance: Double
    abstract val screenPoints: List<Pair<Int, Int>>
    abstract val color: Color
    abstract val texture: ImageEntry?
    abstract val textureCoords: List<Pair<Double, Double>>

    data class WallInfo(
        override val distance: Double,
        override val screenPoints: List<Pair<Int, Int>>,
        override val color: Color,
        override val texture: ImageEntry?,
        override val textureCoords: List<Pair<Double, Double>>,
        val wall: Wall
    ) : RenderableObject()

    data class FloorInfo(
        override val distance: Double,
        override val screenPoints: List<Pair<Int, Int>>,
        override val color: Color,
        override val texture: ImageEntry?,
        override val textureCoords: List<Pair<Double, Double>>,
        val floor: Floor,
        val viewingFromBelow: Boolean  // Flag for tracking view direction
    ) : RenderableObject()
}