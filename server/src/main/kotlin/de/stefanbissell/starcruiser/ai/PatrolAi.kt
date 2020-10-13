package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.fullCircle
import de.stefanbissell.starcruiser.randomAngle
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.ShipContactList

class PatrolAi(
    private val behaviourAi: BehaviourAi,
    private val helmAi: HelmAi
) : ComponentAi(2.0) {

    var path: List<Vector2> = emptyList()
        set(value) {
            field = value
            pointInPath = 0
        }
    var pointInPath: Int = 0

    private val nextPoint: Vector2
        get() = path[pointInPath]

    override fun execute(
        ship: NonPlayerShip,
        time: GameTime,
        contactList: ShipContactList
    ) {
        if (path.isEmpty()) {
            path = createInitialPath(ship)
        }

        if (behaviourAi.behaviour is Behaviour.Patrol) {
            performPatrol(ship)
        }
    }

    private fun performPatrol(ship: NonPlayerShip) {
        if (path.isNotEmpty()) {
            with(ship) {
                checkPointReached()
                steerTowardsNextPoint()
            }
        }
    }

    private fun createInitialPath(ship: NonPlayerShip): List<Vector2> {
        val angle = randomAngle()
        return listOf(
            ship.position + Vector2(1000, 0).rotate(angle),
            ship.position + Vector2(1000, 0).rotate(angle + fullCircle / 3),
            ship.position
        )
    }

    private fun NonPlayerShip.checkPointReached() {
        if (nextPointRange < 50) {
            incrementNextPoint()
        }
    }

    private fun incrementNextPoint() {
        pointInPath = (pointInPath + 1) % path.size
    }

    private fun NonPlayerShip.steerTowardsNextPoint() {
        throttle = 50
        if (helmAi.targetRotation == null) {
            helmAi.targetRotation = nextPointAngle
        }
    }

    private val NonPlayerShip.nextPointRelative
        get() = nextPoint - position

    private val NonPlayerShip.nextPointRange
        get() = nextPointRelative.length()

    private val NonPlayerShip.nextPointAngle
        get() = nextPointRelative.angle()
}
