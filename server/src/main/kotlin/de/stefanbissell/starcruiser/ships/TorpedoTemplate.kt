package de.stefanbissell.starcruiser.ships

data class TorpedoTemplate(
    val radius: Double = 1.0,
    val mass: Double = 100.0,
    val thrust: Double = 2_000.0,
    val maxBurnTime: Double = 20.0
)
