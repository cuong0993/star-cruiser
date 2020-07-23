package de.stefanbissell.starcruiser.scenario

import de.stefanbissell.starcruiser.Vector2
import kotlin.math.abs
import kotlin.random.Random

data class Box(
    val bottomLeft: Vector2,
    val topRight: Vector2
) : Area {

    val width = abs(topRight.x - bottomLeft.x)
    val height = abs(topRight.y - bottomLeft.y)

    override val boundingBox: Box = this

    override fun isInside(point: Vector2): Boolean =
        point.x >= bottomLeft.x && point.y >= bottomLeft.y
            && point.x <= topRight.x && point.y <= topRight.y

    fun randomPointInside() =
        bottomLeft + Vector2(Random.nextDouble(width), Random.nextDouble(height))
}