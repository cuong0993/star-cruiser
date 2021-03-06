package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.ships.ContactList
import de.stefanbissell.starcruiser.ships.DynamicObject
import de.stefanbissell.starcruiser.ships.Ship
import de.stefanbissell.starcruiser.ships.combat.DamageEvent
import strikt.api.Assertion
import strikt.assertions.isEqualTo

fun Assertion.Builder<Double>.isNear(expected: Number, tolerance: Double = 0.0000001) =
    isEqualTo(expected.toDouble(), tolerance)

fun Assertion.Builder<Vector2>.isNear(expected: Vector2, tolerance: Double = 0.0000001) =
    assert("is within $tolerance of $expected") {
        and {
            get { x }.isEqualTo(expected.x, tolerance)
            get { y }.isEqualTo(expected.y, tolerance)
        }
    }

fun emptyContactList(ship: Ship) = ContactList(ship, emptyList())

fun DynamicObject.takeDamage(targetSystemType: PoweredSystemType, amount: Double, modulation: Int) {
    applyDamage(DamageEvent.Beam(id, targetSystemType, amount, modulation))
}
