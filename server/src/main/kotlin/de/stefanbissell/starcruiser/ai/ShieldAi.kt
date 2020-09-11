package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList

class ShieldAi : ComponentAi() {

    override fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: ShipContactList
    ) {
        with(ship.shieldHandler) {
            val hostile = closestHostile(ship, contactList)
            if (hostile != null && ship.rangeTo(hostile.position) <= 500.0) {
                if (!up && activationAllowed()) {
                    up = true
                }
            } else {
                if (up) {
                    up = false
                }
            }
        }
    }

    private fun closestHostile(ship: NonPlayerShip, contactList: ShipContactList) =
        contactList.shipList
            .filter {
                ship.inSensorRange(it)
            }.firstOrNull {
                it.getContactType(ship) == ContactType.Enemy
            }
}
