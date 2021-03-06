package de.stefanbissell.starcruiser.ai

import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.ScanLevel
import de.stefanbissell.starcruiser.TestFactions
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.isNear
import de.stefanbissell.starcruiser.p
import de.stefanbissell.starcruiser.ships.ContactList
import de.stefanbissell.starcruiser.ships.NonPlayerShip
import de.stefanbissell.starcruiser.ships.Ship
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import kotlin.math.PI

class HomingAiTest {

    private val ship = NonPlayerShip(faction = TestFactions.enemy)
    private val time = GameTime.atEpoch()
    private val behaviourAi = BehaviourAi(Behaviour.Attack)
    private val helmAi = HelmAi()
    private val homingAi = HomingAi(behaviourAi, helmAi)

    private val shipList = mutableListOf<Ship>()

    @Test
    fun `stops when to close to target`() {
        ship.throttle = 50
        addShip(p(90, 0))

        executeAi()

        expectThat(ship.throttle).isEqualTo(0)
    }

    @Test
    fun `throttles up when distant from target`() {
        ship.throttle = 0
        addShip(p(110, 0))

        executeAi()

        expectThat(ship.throttle).isEqualTo(70)
    }

    @Test
    fun `does not home on friendly ship`() {
        addShip(hostile = false)

        executeAi()

        expectThat(homingAi.target).isNull()
        expectThat(helmAi.targetRotation).isNull()
    }

    @Test
    fun `does not home on ship outside sensor range`() {
        addShip(p(ship.sensorRange * 2, 0))

        executeAi()

        expectThat(homingAi.target).isNull()
        expectThat(helmAi.targetRotation).isNull()
    }

    @Test
    fun `does nothing if behaviour not attack`() {
        addShip(p(1_000, 1_000))
        behaviourAi.behaviour = Behaviour.CombatPatrol

        executeAi()

        expectThat(homingAi.target).isNull()
        expectThat(helmAi.targetRotation).isNull()
    }

    @Test
    fun `homes on hostile ship`() {
        val target = addShip(p(1_000, 1_000))

        executeAi()

        expectThat(homingAi.target).isEqualTo(target.id)
        expectThat(helmAi.targetRotation)
            .isNotNull().isNear(PI * 0.25)
    }

    @Test
    fun `homes on closest hostile ship`() {
        addShip(p(1_000, 1_000))
        val nearTarget = addShip(p(-500, -500))

        executeAi()

        expectThat(homingAi.target).isEqualTo(nearTarget.id)
        expectThat(helmAi.targetRotation)
            .isNotNull().isNear(-PI * 0.75)
    }

    @Test
    fun `does not change helm if already turning`() {
        val target = addShip()
        helmAi.targetRotation = 0.0

        executeAi()

        expectThat(homingAi.target).isEqualTo(target.id)
        expectThat(helmAi.targetRotation)
            .isNotNull().isNear(0.0)
    }

    @Test
    fun `looses target when no longer in contact list`() {
        homingAi.target = ObjectId.random()

        executeAi()

        expectThat(homingAi.target).isNull()
    }

    @Test
    fun `looses target when out of sensor range`() {
        val target = addShip(p(ship.sensorRange * 2, 0))
        homingAi.target = target.id

        executeAi()

        expectThat(homingAi.target).isNull()
    }

    @Test
    fun `reverts to cruising speed when no target`() {
        ship.throttle = 0

        executeAi()

        expectThat(ship.throttle).isEqualTo(50)
    }

    @Test
    fun `chooses new target when current is out of sensor range`() {
        val outOfRangeTarget = addShip(p(ship.sensorRange * 2, 0))
        val inRangeTarget = addShip(p(ship.sensorRange * 0.5, 0))
        homingAi.target = outOfRangeTarget.id

        executeAi()

        expectThat(homingAi.target).isEqualTo(inRangeTarget.id)
    }

    private fun executeAi() {
        homingAi.execute(AiState(ship, time, ContactList(ship, shipList)))
    }

    private fun addShip(
        position: Vector2 = p(1_000, 1_000),
        hostile: Boolean = true
    ): Ship {
        val target = NonPlayerShip(
            position = position,
            faction = if (hostile) TestFactions.player else TestFactions.enemy
        )
        ship.scans[target.id] = ScanLevel.Detailed
        shipList.add(target)
        return target
    }
}
