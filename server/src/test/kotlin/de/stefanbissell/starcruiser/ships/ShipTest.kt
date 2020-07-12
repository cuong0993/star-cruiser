package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.BeamStatus
import de.stefanbissell.starcruiser.BodyParameters
import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.LockStatus
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.PhysicsEngine
import de.stefanbissell.starcruiser.PoweredSystem
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.WaypointMessage
import de.stefanbissell.starcruiser.isNear
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.hasEntry
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import java.time.Instant

class ShipTest {

    private val ship = Ship(
        position = Vector2(3, -4),
        template = ShipTemplate().let {
            it.copy(beams = it.beams.subList(0, 1))
        }
    )
    private val time = GameTime().apply {
        update(Instant.EPOCH)
    }
    private val physicsEngine = mockk<PhysicsEngine>(relaxed = true)

    @BeforeEach
    fun setUp() {
        every {
            physicsEngine.getBodyParameters(ship.id)
        } returns BodyParameters(
            position = Vector2(3, -4),
            speed = Vector2(),
            rotation = 0.0
        )
    }

    @Test
    fun `updates positive thrust`() {
        ship.changeThrottle(100)
        stepTimeTo(2)

        expectThat(ship.toMessage().thrust).isNear(ship.template.throttleResponsiveness * 2)
    }

    @Test
    fun `updates negative thrust`() {
        ship.changeThrottle(-100)
        stepTimeTo(2)

        expectThat(ship.toMessage().thrust).isNear(ship.template.throttleResponsiveness * -2)
    }

    @Test
    fun `sets positive throttle`() {
        ship.changeThrottle(50)

        expectThat(ship.toMessage().throttle).isEqualTo(50)
    }

    @Test
    fun `sets negative throttle`() {
        ship.changeThrottle(-50)

        expectThat(ship.toMessage().throttle).isEqualTo(-50)
    }

    @Test
    fun `clamps throttle to lower bound`() {
        ship.changeThrottle(-150)

        expectThat(ship.toMessage().throttle).isEqualTo(-100)
    }

    @Test
    fun `clamps throttle to upper bound`() {
        ship.changeThrottle(150)

        expectThat(ship.toMessage().throttle).isEqualTo(100)
    }

    @Test
    fun `sets jump distance`() {
        ship.changeJumpDistance(0.2)

        expectThat(ship.toMessage().jumpDrive.distance).isEqualTo(3_000)
    }

    @Test
    fun `sets jump distance adjusting to increments`() {
        ship.changeJumpDistance(0.24)

        expectThat(ship.toMessage().jumpDrive.distance).isEqualTo(3_500)
    }

    @Test
    fun `clamps jump distance to lower bound`() {
        ship.changeJumpDistance(-0.5)

        expectThat(ship.toMessage().jumpDrive.distance).isEqualTo(jumpDrive.minDistance)
    }

    @Test
    fun `clamps jump distance to upper bound`() {
        ship.changeJumpDistance(1.5)

        expectThat(ship.toMessage().jumpDrive.distance).isEqualTo(jumpDrive.maxDistance)
    }

    @Test
    fun `sets positive rudder`() {
        ship.changeRudder(50)

        expectThat(ship.toMessage().rudder).isEqualTo(50)
    }

    @Test
    fun `sets negative rudder`() {
        ship.changeRudder(-50)

        expectThat(ship.toMessage().rudder).isEqualTo(-50)
    }

    @Test
    fun `clamps rudder to lower bound`() {
        ship.changeRudder(-150)

        expectThat(ship.toMessage().rudder).isEqualTo(-100)
    }

    @Test
    fun `clamps rudder to upper bound`() {
        ship.changeRudder(150)

        expectThat(ship.toMessage().rudder).isEqualTo(100)
    }

    @Test
    fun `can add waypoint`() {
        ship.addWaypoint(Vector2(5, -4))

        expectThat(ship.toMessage().waypoints).containsExactly(
            WaypointMessage(1, "WP1", Vector2(5, -4), Vector2(2, 0), 90.0)
        )
    }

    @Test
    fun `can delete waypoint`() {
        ship.addWaypoint(Vector2(5, -4))
        ship.deleteWaypoint(1)

        expectThat(ship.toMessage().waypoints).isEmpty()
    }

    @Test
    fun `keeps index after deletion`() {
        ship.addWaypoint(Vector2(5, -4))
        ship.addWaypoint(Vector2(10, -4))
        ship.deleteWaypoint(1)

        expectThat(ship.toMessage().waypoints).containsExactly(
            WaypointMessage(2, "WP2", Vector2(10, -4), Vector2(7, 0), 90.0)
        )
    }

    @Test
    fun `fills waypoint slot`() {
        ship.addWaypoint(Vector2(5, -4))
        ship.addWaypoint(Vector2(10, -4))
        ship.deleteWaypoint(1)
        ship.addWaypoint(Vector2(15, -4))

        expectThat(ship.toMessage().waypoints).containsExactly(
            WaypointMessage(1, "WP1", Vector2(15, -4), Vector2(12, 0), 90.0),
            WaypointMessage(2, "WP2", Vector2(10, -4), Vector2(7, 0), 90.0)
        )
    }

    @Test
    fun `does not start scan with scan in progress`() {
        val target1 = Ship()
        val target2 = Ship()

        ship.startScan(target1.id)
        ship.startScan(target2.id)

        expectThat(ship.toMessage().scanProgress).isNotNull()
            .get { targetId }.isEqualTo(target1.id)
    }

    @Test
    fun `scans target`() {
        val target = Ship()

        ship.startScan(target.id)
        stepTimeTo(4)

        expectThat(target.toContactMessage(ship).type).isEqualTo(ContactType.Unknown)

        stepTimeTo(6)

        expectThat(target.toContactMessage(ship).type).isEqualTo(ContactType.Friendly)
    }

    @Test
    fun `does not scan target with maximum scan level`() {
        val target = Ship()

        ship.startScan(target.id)
        stepTimeTo(6)

        expectThat(target.toContactMessage(ship).type).isEqualTo(ContactType.Friendly)

        ship.startScan(target.id)

        expectThat(ship.toMessage().scanProgress).isNull()
    }

    @Test
    fun `updates physics engine`() {
        ship.changeThrottle(50)
        ship.changeRudder(50)
        stepTimeTo(2)

        verify(exactly = 1) {
            physicsEngine.updateShip(
                ship.id,
                50 * ship.template.aheadThrustFactor,
                50 * ship.template.rudderFactor
            )
        }
    }

    @Test
    fun `updates physics engine applying impulse power level`() {
        ship.setPower(PoweredSystem.Impulse, 150)
        ship.changeThrottle(50)
        ship.changeRudder(50)
        stepTimeTo(2)

        verify(exactly = 1) {
            physicsEngine.updateShip(
                ship.id,
                50 * ship.template.aheadThrustFactor * 1.5,
                50 * ship.template.rudderFactor
            )
        }
    }

    @Test
    fun `takes values from physics engine`() {
        every {
            physicsEngine.getBodyParameters(ship.id)
        } returns BodyParameters(
            position = Vector2(1, 2),
            speed = Vector2(3, 4),
            rotation = 5.0
        )

        ship.update(time, physicsEngine) { null }

        ship.toMessage().also {
            expectThat(it.position).isEqualTo(Vector2(1, 2))
            expectThat(it.speed).isEqualTo(Vector2(3, 4))
            expectThat(it.rotation).isEqualTo(5.0)
        }
    }

    @Test
    fun `updates target lock`() {
        val target = Ship()

        ship.update(time, physicsEngine) { target }

        expectThat(ship.toMessage().lockProgress).isA<LockStatus.NoLock>()

        ship.lockTarget(target.id)
        stepTimeTo(1)

        expectThat(ship.toMessage().lockProgress).isA<LockStatus.InProgress>()

        stepTimeTo(10)

        expectThat(ship.toMessage().lockProgress).isA<LockStatus.Locked>()
    }

    @Test
    fun `updates beams`() {
        val target = Ship(
            position = Vector2(100, 0)
        )

        ship.lockTarget(target.id)
        stepTimeTo(10)
        expectThat(ship.toMessage().lockProgress).isA<LockStatus.Locked>()
        expectThat(ship.toMessage().beams.first().status).isA<BeamStatus.Idle>()

        stepTimeTo(10.5) { target }
        expectThat(ship.toMessage().beams.first().status).isA<BeamStatus.Firing>()
        expectThat(target.toMessage().shield.strength).isEqualTo(target.template.shield.strength - 0.5)

        stepTimeTo(12) { target }
        expectThat(ship.toMessage().beams.first().status).isA<BeamStatus.Recharging>()
    }

    @Test
    fun `does not fire if target outside arc`() {
        val target = Ship(
            position = Vector2(0, 100)
        )

        ship.lockTarget(target.id)
        stepTimeTo(10)
        expectThat(ship.toMessage().lockProgress).isA<LockStatus.Locked>()
        expectThat(ship.toMessage().beams.first().status).isA<BeamStatus.Idle>()

        stepTimeTo(10.5) { target }
        expectThat(ship.toMessage().beams.first().status).isA<BeamStatus.Idle>()
    }

    @Test
    fun `does not fire if target outside range`() {
        val target = Ship(
            position = Vector2(ship.template.beams.first().range.last + 100, 0)
        )

        ship.lockTarget(target.id)
        stepTimeTo(10)
        expectThat(ship.toMessage().lockProgress).isA<LockStatus.Locked>()
        expectThat(ship.toMessage().beams.first().status).isA<BeamStatus.Idle>()

        stepTimeTo(10.5) { target }
        expectThat(ship.toMessage().beams.first().status).isA<BeamStatus.Idle>()
    }

    @Test
    fun `updates shields`() {
        val damage = 5.0
        ship.takeDamage(damage)
        expectThat(ship.toMessage().shield.strength)
            .isNear(shieldTemplate.strength - damage)

        stepTimeTo(4)
        expectThat(ship.toMessage().shield.strength)
            .isNear(shieldTemplate.strength - damage + shieldTemplate.rechargeSpeed * 4.0)
    }

    @Test
    fun `takes hull damage when shields depleted`() {
        val damage = shieldTemplate.strength + 5.0
        ship.takeDamage(damage)
        expectThat(ship.toMessage().shield.strength)
            .isNear(0.0)
        expectThat(ship.toMessage().hull)
            .isNear(ship.template.hull - 5.0)
        expectThat(stepTimeTo(0.1).destroyed)
            .isFalse()
    }

    @Test
    fun `ship can be destroyed`() {
        val damage = shieldTemplate.strength + ship.template.hull + 5.0
        ship.takeDamage(damage)
        expectThat(ship.toMessage().shield.strength)
            .isNear(0.0)
        expectThat(ship.toMessage().hull)
            .isNear(-5.0)
        expectThat(stepTimeTo(0.1).destroyed)
            .isTrue()
    }

    @Test
    fun `shield can be activated when above activation strength`() {
        val damage = shieldTemplate.strength - shieldTemplate.activationStrength * 2.0
        ship.takeDamage(damage)
        ship.setShieldsUp(false)

        expectThat(ship.toMessage().shield.up)
            .isFalse()

        ship.setShieldsUp(true)
        expectThat(ship.toMessage().shield.up)
            .isTrue()
    }

    @Test
    fun `shield fails when below failure strength and cannot be activated again`() {
        val damage = shieldTemplate.strength - shieldTemplate.failureStrength * 0.5
        ship.takeDamage(damage)
        stepTimeTo(0.1)

        expectThat(ship.toMessage().shield.up)
            .isFalse()

        ship.setShieldsUp(true)
        expectThat(ship.toMessage().shield.up)
            .isFalse()

        stepTimeTo(1000.0)
        ship.setShieldsUp(true)

        expectThat(ship.toMessage().shield.up)
            .isTrue()
    }

    @Test
    fun `can set power`() {
        ship.setPower(PoweredSystem.Maneuver, 150)

        expectThat(ship.toMessage().powerMessage.settings)
            .hasEntry(PoweredSystem.Maneuver, 150)
    }

    private fun stepTimeTo(seconds: Number, shipProvider: (ObjectId) -> Ship? = { null }): ShipUpdateResult {
        time.update(Instant.EPOCH.plusMillis((seconds.toDouble() * 1000).toLong()))
        ship.update(time, physicsEngine, shipProvider)
        return ship.endUpdate(physicsEngine)
    }

    private val shieldTemplate
        get() = ship.template.shield

    private val jumpDrive
        get() = ship.template.jumpDrive
}
