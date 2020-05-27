package de.bissell.starcruiser

data class ShipTemplate(
    val className: String = "Infector",
    val aheadThrustFactor: Double = 0.2,
    val reverseThrustFactor: Double = 0.1,
    val rudderFactor: Double = 8.0
)