import java.awt.Color

data class Vec3(var x: Double, var y: Double, var z: Double)
data class Wall(val start: Vec3, val end: Vec3, val height: Double, val color: Color)
data class Floor(val x1: Double, val z1: Double, val x2: Double, val z2: Double, val y: Double, val color: Color)