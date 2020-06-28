package de.bissell.starcruiser.ships

import de.bissell.starcruiser.GameTime
import de.bissell.starcruiser.ShieldMessage
import kotlin.math.max
import kotlin.math.min

class ShieldHandler(
    private val shieldTemplate: ShieldTemplate
) {

    private var up: Boolean = true
    private var damageSinceLastUpdate: Double = 0.0
    private var activated: Boolean = false

    private var currentStrength: Double = shieldTemplate.strength

    fun update(time: GameTime) {
        currentStrength = min(
            shieldTemplate.strength,
            currentStrength + rechargeAmount(time)
        )
    }

    fun endUpdate() {
        if (shieldFailing()) {
            up = false
        }
        activated = up && damageSinceLastUpdate > 0.0
        damageSinceLastUpdate = 0.0
    }

    fun takeDamageAndReportHullDamage(amount: Double): Double {
        return if (up) {
            takeDamageToShieldAndThenHull(amount)
        } else {
            amount
        }
    }

    fun setUp(value: Boolean) {
        if (value) {
            if (activationAllowed()) {
                up = true
            }
        } else {
            up = false
        }
    }

    private fun activationAllowed() = currentStrength >= shieldTemplate.activationStrength

    fun toMessage() =
        ShieldMessage(
            radius = shieldTemplate.radius,
            up = up,
            activated = activated,
            strength = currentStrength,
            max = shieldTemplate.strength
        )

    private fun shieldFailing() = currentStrength <= shieldTemplate.failureStrength

    private fun rechargeAmount(time: GameTime) = shieldTemplate.rechargeSpeed * time.delta

    private fun takeDamageToShieldAndThenHull(amount: Double): Double {
        val hullDamage = max(0.0, amount - currentStrength)
        damageSinceLastUpdate += amount
        currentStrength = max(
            0.0,
            currentStrength - amount
        )
        return hullDamage
    }
}