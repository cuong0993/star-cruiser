package de.bissell.starcruiser

import kotlin.math.*

const val degreesToRadians = 0.017453292519943295
const val radiansToDegrees = 57.29577951308232

fun Int.clip(lower: Int, upper: Int) = min(upper, max(lower, this))

fun Double.toRadians() = this * degreesToRadians

fun Double.toDegrees(): Double = this * radiansToDegrees

fun Double.toHeading() =
    (90.0 - this.toDegrees()).let {
        if (it < 0.0) {
            it % 360.0 + 360.0
        } else {
            it % 360.0
        }
    }

fun Double.round(digits: Int) =
    (this * 10.0.pow(digits.absoluteValue)).roundToInt() / 10.0.pow(digits.absoluteValue)

fun Double.format(digits: Int) =
    this.round(digits).toString().let {
        if (it.contains(".")) it else "$it.${(1..digits).joinToString { "0" }}"
    }