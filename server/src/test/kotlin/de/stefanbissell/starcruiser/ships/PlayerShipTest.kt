package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.BeamStatus
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.LockStatus
import de.stefanbissell.starcruiser.PoweredSystemMessage
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.PoweredSystemType.Reactor
import de.stefanbissell.starcruiser.PoweredSystemType.Sensors
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.WaypointMessage
import de.stefanbissell.starcruiser.isNear
import de.stefanbissell.starcruiser.p
import de.stefanbissell.starcruiser.physics.BodyParameters
import de.stefanbissell.starcruiser.physics.PhysicsEngine
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

class PlayerShipTest {

    private val ship = PlayerShip(
        position = p(3, -4),
        template = carrierTemplate.let {
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
            position = Vector2(3, -4)
        )
        every {
            physicsEngine.findObstructions(any(), any(), any())
        } returns emptyList()
    }

    @Test
    fun `sets positive throttle`() {
        ship.changeThrottle(50)

        expectThat(ship.throttle).isEqualTo(50)
    }

    @Test
    fun `sets negative throttle`() {
        ship.changeThrottle(-50)

        expectThat(ship.throttle).isEqualTo(-50)
    }

    @Test
    fun `clamps throttle to lower bound`() {
        ship.changeThrottle(-150)

        expectThat(ship.throttle).isEqualTo(-100)
    }

    @Test
    fun `clamps throttle to upper bound`() {
        ship.changeThrottle(150)

        expectThat(ship.throttle).isEqualTo(100)
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

        expectThat(ship.rudder).isEqualTo(50)
    }

    @Test
    fun `sets negative rudder`() {
        ship.changeRudder(-50)

        expectThat(ship.rudder).isEqualTo(-50)
    }

    @Test
    fun `clamps rudder to lower bound`() {
        ship.changeRudder(-150)

        expectThat(ship.rudder).isEqualTo(-100)
    }

    @Test
    fun `clamps rudder to upper bound`() {
        ship.changeRudder(150)

        expectThat(ship.rudder).isEqualTo(100)
    }

    @Test
    fun `can add waypoint`() {
        ship.addWaypoint(p(5, -4))

        expectThat(ship.toShortRangeScopeMessage().waypoints).containsExactly(
            WaypointMessage(1, "WP1", p(5, -4), p(2, 0), 90.0)
        )
    }

    @Test
    fun `can delete waypoint`() {
        ship.addWaypoint(p(5, -4))
        ship.mapSelectWaypoint(1)
        ship.deleteSelectedWaypoint()

        expectThat(ship.toShortRangeScopeMessage().waypoints).isEmpty()
    }

    @Test
    fun `keeps index after deletion`() {
        ship.addWaypoint(p(5, -4))
        ship.addWaypoint(p(10, -4))
        ship.mapSelectWaypoint(1)
        ship.deleteSelectedWaypoint()

        expectThat(ship.toShortRangeScopeMessage().waypoints).containsExactly(
            WaypointMessage(2, "WP2", p(10, -4), p(7, 0), 90.0)
        )
    }

    @Test
    fun `fills waypoint slot`() {
        ship.addWaypoint(p(5, -4))
        ship.addWaypoint(p(10, -4))
        ship.mapSelectWaypoint(1)
        ship.deleteSelectedWaypoint()
        ship.addWaypoint(p(15, -4))

        expectThat(ship.toShortRangeScopeMessage().waypoints).containsExactly(
            WaypointMessage(1, "WP1", p(15, -4), p(12, 0), 90.0),
            WaypointMessage(2, "WP2", p(10, -4), p(7, 0), 90.0)
        )
    }

    @Test
    fun `does not start scan with scan in progress`() {
        val target1 = PlayerShip()
        val target2 = PlayerShip()

        ship.mapSelectShip(target1.id)
        ship.startScan()
        ship.mapSelectShip(target2.id)
        ship.startScan()

        expectThat(ship.toNavigationMessage { target1 }.scanProgress).isNotNull()
            .get { targetId }.isEqualTo(target1.id)
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
        ship.setPower(PoweredSystemType.Impulse, 150)
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
    fun `updates physics engine applying maneuver power level`() {
        ship.setPower(PoweredSystemType.Maneuver, 80)
        ship.changeThrottle(50)
        ship.changeRudder(50)
        stepTimeTo(2)

        verify(exactly = 1) {
            physicsEngine.updateShip(
                ship.id,
                50 * ship.template.aheadThrustFactor,
                50 * ship.template.rudderFactor * 0.8
            )
        }
    }

    @Test
    fun `takes values from physics engine`() {
        every {
            physicsEngine.getBodyParameters(ship.id)
        } returns BodyParameters(
            position = p(1, 2),
            rotation = 5.0
        )

        ship.update(time, physicsEngine) { null }

        ship.toMessage().also {
            expectThat(it.position).isEqualTo(p(1, 2))
            expectThat(it.rotation).isEqualTo(5.0)
        }
    }

    @Test
    fun `updates target lock`() {
        val target = PlayerShip()

        ship.update(time, physicsEngine) { target }

        expectThat(ship.toShortRangeScopeMessage().lockProgress).isA<LockStatus.NoLock>()

        ship.lockTarget(target.id)
        stepTimeTo(1) { target }

        expectThat(ship.toShortRangeScopeMessage().lockProgress).isA<LockStatus.InProgress>()

        stepTimeTo(10) { target }

        expectThat(ship.toShortRangeScopeMessage().lockProgress).isA<LockStatus.Locked>()
    }

    @Test
    fun `updates beams`() {
        val target = PlayerShip(
            position = p(100, 0)
        )

        ship.lockTarget(target.id)
        stepTimeTo(10) { target }
        expectThat(ship.toShortRangeScopeMessage().lockProgress).isA<LockStatus.Locked>()
        expectThat(ship.toMessage().beams.first().status).isA<BeamStatus.Idle>()

        stepTimeTo(10.5) { target }
        expectThat(ship.toMessage().beams.first().status).isA<BeamStatus.Firing>()
        expectThat(target.toMessage().shield.strength).isEqualTo(target.template.shield.strength - 0.5)

        stepTimeTo(12) { target }
        expectThat(ship.toMessage().beams.first().status).isA<BeamStatus.Recharging>()

        stepTimeTo(14)
        expectThat(ship.toMessage().beams.first().status)
            .isA<BeamStatus.Recharging>()
            .get { progress }.isNear(ship.template.beams.first().rechargeSpeed * 2.0)
    }

    @Test
    fun `updates beams applying power level`() {
        ship.setPower(PoweredSystemType.Weapons, 200)
        ship.setCoolant(PoweredSystemType.Weapons, 1.0)
        val target = PlayerShip(
            position = p(100, 0)
        )

        ship.lockTarget(target.id)
        stepTimeTo(10) { target }
        stepTimeTo(10.5) { target }
        expectThat(ship.toMessage().beams.first().status).isA<BeamStatus.Firing>()
        stepTimeTo(12) { target }
        expectThat(ship.toMessage().beams.first().status)
            .isA<BeamStatus.Recharging>()
            .get { progress }.isNear(0.0)
        stepTimeTo(14)
        expectThat(ship.toMessage().beams.first().status)
            .isA<BeamStatus.Recharging>()
            .get { progress }.isNear(ship.template.beams.first().rechargeSpeed * 4.0)
    }

    @Test
    fun `does not fire if target outside arc`() {
        val target = PlayerShip(
            position = p(0, 100)
        )

        ship.lockTarget(target.id)
        stepTimeTo(10) { target }
        expectThat(ship.toShortRangeScopeMessage().lockProgress).isA<LockStatus.Locked>()
        expectThat(ship.toMessage().beams.first().status).isA<BeamStatus.Idle>()

        stepTimeTo(10.5) { target }
        expectThat(ship.toMessage().beams.first().status).isA<BeamStatus.Idle>()
    }

    @Test
    fun `does not fire if target outside range`() {
        val target = PlayerShip(
            position = p(ship.template.beams.first().range.last + 100, 0)
        )

        ship.lockTarget(target.id)
        stepTimeTo(10) { target }
        expectThat(ship.toShortRangeScopeMessage().lockProgress).isA<LockStatus.Locked>()
        expectThat(ship.toMessage().beams.first().status).isA<BeamStatus.Idle>()

        stepTimeTo(10.5) { target }
        expectThat(ship.toMessage().beams.first().status).isA<BeamStatus.Idle>()
    }

    @Test
    fun `updates shields`() {
        val damage = 5.0
        ship.takeDamage(Reactor, damage)
        expectThat(ship.toMessage().shield.strength)
            .isNear(shieldTemplate.strength - damage)

        stepTimeTo(4)
        expectThat(ship.toMessage().shield.strength)
            .isNear(shieldTemplate.strength - damage + shieldTemplate.rechargeSpeed * 4.0)
    }

    @Test
    fun `takes hull damage when shields depleted`() {
        val damage = shieldTemplate.strength + 5.0
        ship.takeDamage(Reactor, damage)
        expectThat(ship.toMessage().shield.strength)
            .isNear(0.0)
        expectThat(ship.hull)
            .isNear(ship.template.hull - 5.0)
        expectThat(stepTimeTo(0.1).destroyed)
            .isFalse()
    }

    @Test
    fun `takes powered system damage when shields depleted`() {
        val damage = shieldTemplate.strength + 5.0
        ship.takeDamage(Reactor, damage)
        expectThat(ship.toMessage().shield.strength)
            .isNear(0.0)
        expectThat(ship.toPowerMessage().settings[Reactor]?.damage)
            .isNotNull()
            .isNear(5.0 / ship.template.poweredSystemDamageCapacity)
    }

    @Test
    fun `ship can be destroyed`() {
        val damage = shieldTemplate.strength + ship.template.hull + 5.0
        ship.takeDamage(Reactor, damage)
        expectThat(ship.toMessage().shield.strength)
            .isNear(0.0)
        expectThat(ship.hull)
            .isNear(-5.0)
        expectThat(stepTimeTo(0.1).destroyed)
            .isTrue()
    }

    @Test
    fun `shield can be activated when above activation strength`() {
        val damage = shieldTemplate.strength - shieldTemplate.activationStrength * 2.0
        ship.takeDamage(Reactor, damage)
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
        ship.takeDamage(Reactor, damage)
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
        ship.setPower(PoweredSystemType.Maneuver, 150)

        expectThat(ship.toPowerMessage().settings)
            .hasEntry(
                PoweredSystemType.Maneuver,
                PoweredSystemMessage(
                    damage = 0.0,
                    level = 150,
                    heat = 0.0,
                    coolant = 0.0
                )
            )
    }

    @Test
    fun `can set coolant`() {
        ship.setCoolant(PoweredSystemType.Maneuver, 0.6)

        expectThat(ship.toPowerMessage().settings)
            .hasEntry(
                PoweredSystemType.Maneuver,
                PoweredSystemMessage(
                    damage = 0.0,
                    level = 100,
                    heat = 0.0,
                    coolant = 0.6
                )
            )
    }

    @Test
    fun `increases sensor range when given more power`() {
        ship.setPower(Sensors, 150)
        expectThat(ship.sensorRange)
            .isNear(ship.template.sensorRange * 1.5)
        expectThat(ship.toNavigationMessage { null }.sensorRange)
            .isNear(ship.template.sensorRange * 1.5)
    }

    @Test
    fun `decreases sensor range when given less power`() {
        ship.setPower(Sensors, 50)
        expectThat(ship.sensorRange)
            .isNear(ship.template.sensorRange * 0.5)
        expectThat(ship.toNavigationMessage { null }.sensorRange)
            .isNear(ship.template.sensorRange * 0.5)
    }

    @Test
    fun `minimal sensor range is short range scope range`() {
        ship.setPower(Sensors, 0)
        expectThat(ship.sensorRange)
            .isNear(ship.template.shortRangeScopeRange)
        expectThat(ship.toNavigationMessage { null }.sensorRange)
            .isNear(ship.template.shortRangeScopeRange)
    }

    @Test
    fun `can select ship on map`() {
        val target = PlayerShip(
            position = p(100, 0)
        )
        ship.mapSelectShip(target.id)

        expectThat(ship.toMapSelectionMessage { target })
            .isNotNull()
            .get { label }.isEqualTo(target.designation)
    }

    @Test
    fun `can select waypoint on map`() {
        ship.addWaypoint(p(0, 0))
        ship.mapSelectWaypoint(1)

        expectThat(ship.toMapSelectionMessage { null })
            .isNotNull()
            .get { label }.isEqualTo("WP1")
    }

    @Test
    fun `cannot select unknown waypoint on map`() {
        ship.addWaypoint(p(0, 0))
        ship.mapSelectWaypoint(2)

        expectThat(ship.toMapSelectionMessage { null })
            .isNull()
    }

    @Test
    fun `cleans up destroyed target`() {
        val target = PlayerShip(
            position = p(100, 0)
        )

        ship.lockTarget(target.id)
        ship.mapSelectShip(target.id)
        ship.startScan()

        expectThat(ship.toShortRangeScopeMessage().lockProgress)
            .isA<LockStatus.InProgress>()
        expectThat(ship.toMapSelectionMessage { target })
            .isNotNull()
            .get { label }.isEqualTo(target.designation)
        expectThat(ship.toNavigationMessage { target }.scanProgress)
            .isNotNull()
            .get { designation }.isEqualTo(target.designation)

        ship.targetDestroyed(target.id)

        expectThat(ship.toShortRangeScopeMessage().lockProgress)
            .isEqualTo(LockStatus.NoLock)
        expectThat(ship.toMapSelectionMessage { target })
            .isNull()
        expectThat(ship.toNavigationMessage { target }.scanProgress)
            .isNull()
    }

    private fun stepTimeTo(seconds: Number, shipProvider: ShipProvider = { null }): ShipUpdateResult {
        time.update(Instant.EPOCH.plusMillis((seconds.toDouble() * 1000).toLong()))
        ship.update(time, physicsEngine, shipProvider)
        return ship.endUpdate(physicsEngine)
    }

    private val shieldTemplate
        get() = ship.template.shield

    private val jumpDrive
        get() = ship.template.jumpDrive
}