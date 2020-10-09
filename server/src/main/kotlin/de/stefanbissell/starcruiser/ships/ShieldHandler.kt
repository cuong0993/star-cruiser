package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ShieldMessage
import de.stefanbissell.starcruiser.clamp
import de.stefanbissell.starcruiser.twoDigits
import kotlin.math.max

class ShieldHandler(
    private val shieldTemplate: ShieldTemplate
) {

    var up: Boolean = true
        set(value) {
            if (value) {
                if (activationAllowed()) {
                    field = true
                }
            } else {
                field = false
            }
        }
    var timeSinceActivation: Double = 1_000_000.0
    private var damageSinceLastUpdate: Double = 0.0
    private var activated: Boolean = false

    private var currentStrength: Double = shieldTemplate.strength
        set(value) {
            field = value.clamp(0.0, shieldTemplate.strength)
        }

    fun update(time: GameTime, boostLevel: Double = 1.0) {
        currentStrength += rechargeAmount(time, boostLevel)
    }

    fun endUpdate(time: GameTime) {
        if (shieldFailing()) {
            up = false
        }
        updateActivation(time)
    }

    fun takeDamageAndReportHullDamage(amount: Double): Double {
        return if (up) {
            takeDamageToShieldAndThenHull(amount)
        } else {
            amount
        }
    }

    fun toggleUp() {
        up = !up
    }

    fun activationAllowed() = currentStrength >= shieldTemplate.activationStrength

    fun toMessage() =
        ShieldMessage(
            radius = shieldTemplate.radius,
            up = up,
            activated = activated,
            strength = currentStrength.twoDigits(),
            max = shieldTemplate.strength
        )

    private fun updateActivation(time: GameTime) {
        activated = up && damageSinceLastUpdate > 0.0
        if (!activated) {
            timeSinceActivation += time.delta
        }
        damageSinceLastUpdate = 0.0
    }

    private fun shieldFailing() = currentStrength <= shieldTemplate.failureStrength

    private fun rechargeAmount(time: GameTime, boostLevel: Double) =
        if (boostLevel > 0.1) {
            shieldTemplate.rechargeSpeed * time.delta * boostLevel
        } else {
            -shieldTemplate.decaySpeed * time.delta
        }

    private fun takeDamageToShieldAndThenHull(amount: Double): Double {
        val hullDamage = max(0.0, amount - currentStrength)
        timeSinceActivation = 0.0
        damageSinceLastUpdate += amount
        currentStrength -= amount
        return hullDamage
    }
}
