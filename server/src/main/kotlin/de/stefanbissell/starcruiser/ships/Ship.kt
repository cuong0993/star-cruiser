package de.stefanbissell.starcruiser.ships

import de.stefanbissell.starcruiser.ContactMessage
import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.GameTime
import de.stefanbissell.starcruiser.LockStatus
import de.stefanbissell.starcruiser.MainScreenView
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.PhysicsEngine
import de.stefanbissell.starcruiser.PlayerShipMessage
import de.stefanbissell.starcruiser.PoweredSystemType
import de.stefanbissell.starcruiser.PoweredSystemType.Impulse
import de.stefanbissell.starcruiser.PoweredSystemType.Jump
import de.stefanbissell.starcruiser.PoweredSystemType.Maneuver
import de.stefanbissell.starcruiser.PoweredSystemType.Sensors
import de.stefanbissell.starcruiser.PoweredSystemType.Shields
import de.stefanbissell.starcruiser.PoweredSystemType.Weapons
import de.stefanbissell.starcruiser.ScanLevel
import de.stefanbissell.starcruiser.ScopeContactMessage
import de.stefanbissell.starcruiser.ShipMessage
import de.stefanbissell.starcruiser.ShortRangeScopeMessage
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.WaypointMessage
import de.stefanbissell.starcruiser.clamp
import de.stefanbissell.starcruiser.fiveDigits
import de.stefanbissell.starcruiser.randomShipName
import de.stefanbissell.starcruiser.toHeading
import de.stefanbissell.starcruiser.toRadians
import de.stefanbissell.starcruiser.twoDigits
import kotlin.math.abs

class Ship(
    val id: ObjectId = ObjectId.random(),
    val template: ShipTemplate = cruiserTemplate,
    private val designation: String = randomShipName(),
    var position: Vector2 = Vector2(),
    private var speed: Vector2 = Vector2(),
    var rotation: Double = 90.0.toRadians(),
    var throttle: Int = 0,
    var rudder: Int = 0
) {

    private var thrust = 0.0
    private val waypoints: MutableList<Waypoint> = mutableListOf()
    private val history = mutableListOf<Pair<Double, Vector2>>()
    private val scans = mutableMapOf<ObjectId, ScanLevel>()
    private val powerHandler = PowerHandler(template)
    private val beamHandlers = template.beams.map { BeamHandler(it, this) }
    private val shieldHandler = ShieldHandler(template.shield)
    private var mapSelection: MapSelection = MapSelection.None
    private var scanHandler: ScanHandler? = null
    private var lockHandler: LockHandler? = null
    var hull = template.hull
    private val jumpHandler = JumpHandler(
        jumpDrive = template.jumpDrive
    )
    private var mainScreenView = MainScreenView.Front

    fun update(time: GameTime, physicsEngine: PhysicsEngine, shipProvider: (ObjectId) -> Ship?) {
        powerHandler.update(time)
        updateBeams(time, shipProvider, physicsEngine)
        shieldHandler.update(time, Shields.boostLevel)
        jumpHandler.update(time, Jump.boostLevel)
        updateScan(time)
        updateLock(time)
        updateThrust(time)
        val effectiveThrust = if (thrust < 0) {
            thrust * template.reverseThrustFactor * Impulse.boostLevel
        } else {
            thrust * template.aheadThrustFactor * Impulse.boostLevel
        }
        val effectiveRudder = rudder * template.rudderFactor * Maneuver.boostLevel
        physicsEngine.updateShip(id, effectiveThrust, effectiveRudder)

        physicsEngine.getBodyParameters(id)?.let {
            position = it.position
            speed = it.speed
            rotation = it.rotation
        }

        updateHistory(time)
    }

    private fun updateBeams(
        time: GameTime,
        shipProvider: (ObjectId) -> Ship?,
        physicsEngine: PhysicsEngine
    ) {
        beamHandlers.forEach {
            it.update(
                time = time,
                boostLevel = Weapons.boostLevel,
                shipProvider = shipProvider,
                lockHandler = lockHandler,
                physicsEngine = physicsEngine
            )
        }
    }

    fun endUpdate(physicsEngine: PhysicsEngine): ShipUpdateResult {
        shieldHandler.endUpdate()
        val destroyed = hull <= 0.0
        if (!destroyed && jumpHandler.jumpComplete) {
            physicsEngine.jumpShip(id, jumpHandler.jumpDistance)
            jumpHandler.endJump()
        }
        return ShipUpdateResult(
            id = id,
            destroyed = destroyed
        )
    }

    fun targetDestroyed(shipId: ObjectId) {
        if (mapSelection.isShipSelected(shipId)) {
            mapSelection = MapSelection.None
        }
        if (scanHandler?.targetId == shipId) {
            scanHandler = null
        }
        if (lockHandler?.targetId == shipId) {
            lockHandler = null
        }
    }

    private fun updateScan(time: GameTime) {
        scanHandler?.also {
            it.update(time)
            if (it.isComplete) {
                val scan = scans[it.targetId] ?: ScanLevel.None
                scans[it.targetId] = scan.next
                scanHandler = null
            }
        }
    }

    private fun updateLock(time: GameTime) {
        lockHandler?.also {
            if (!it.isComplete) {
                it.update(time)
            }
        }
    }

    private fun updateThrust(time: GameTime) {
        val responsiveness = template.throttleResponsiveness * time.delta
        thrust = if (abs(thrust - throttle) <= responsiveness) {
            throttle.toDouble()
        } else {
            val diff = when {
                throttle > thrust -> responsiveness
                throttle < thrust -> -responsiveness
                else -> 0.0
            }
            (thrust + diff).clamp(-100.0, 100.0)
        }
    }

    private fun updateHistory(time: GameTime) {
        if (history.isEmpty()) {
            history.add(Pair(time.current, position))
        } else {
            if (abs(history.last().first - time.current) > 1.0) {
                history.add(Pair(time.current, position))
            }
            if (history.size > 10) {
                history.removeAt(0)
            }
        }
    }

    fun changeThrottle(value: Int) {
        if (!jumpHandler.jumping) {
            throttle = value.clamp(-100, 100)
        }
    }

    fun changeJumpDistance(value: Double) {
        if (!jumpHandler.jumping) {
            jumpHandler.changeJumpDistance(value)
        }
    }

    fun startJump() {
        if (jumpHandler.ready) {
            jumpHandler.startJump()
            throttle = 0
            rudder = 0
        }
    }

    fun changeRudder(value: Int) {
        if (!jumpHandler.jumping) {
            rudder = value.clamp(-100, 100)
        }
    }

    fun mapClearSelection() {
        mapSelection = MapSelection.None
    }

    fun mapSelectWaypoint(index: Int) {
        mapSelection = MapSelection.Waypoint(index)
    }

    fun mapSelectShip(targetId: ObjectId) {
        mapSelection = MapSelection.Ship(targetId)
    }

    fun addWaypoint(position: Vector2) {
        (1..waypoints.size * 2 + 1).firstOrNull {
            waypoints.none { waypoint -> waypoint.index == it }
        }?.also {
            waypoints += Waypoint(it, position)
        }
        waypoints.sortBy { it.index }
    }

    fun deleteWaypoint(index: Int) {
        waypoints.removeIf { it.index == index }
        if (mapSelection.isWaypointSelected(index)) {
            mapSelection = MapSelection.None
        }
    }

    fun startScan(targetId: ObjectId) {
        if (scanHandler == null && canIncreaseScanLevel(targetId)) {
            scanHandler = ScanHandler(
                targetId = targetId,
                scanningSpeed = template.scanSpeed,
                boostLevel = { Sensors.boostLevel }
            )
        }
    }

    fun lockTarget(targetId: ObjectId) {
        if (lockHandler?.targetId != targetId) {
            lockHandler = LockHandler(
                targetId = targetId,
                lockingSpeed = template.lockingSpeed,
                boostLevel = { Sensors.boostLevel }
            )
        }
    }

    fun setShieldsUp(value: Boolean) {
        shieldHandler.setUp(value)
    }

    fun toggleShieldsUp() {
        shieldHandler.toggleUp()
    }

    fun startRepair(systemType: PoweredSystemType) {
        powerHandler.startRepair(systemType)
    }

    fun setPower(systemType: PoweredSystemType, level: Int) {
        powerHandler.setLevel(systemType, level)
    }

    fun setCoolant(systemType: PoweredSystemType, coolant: Double) {
        powerHandler.setCoolant(systemType, coolant)
    }

    fun setMainScreenView(view: MainScreenView) {
        mainScreenView = view
    }

    fun takeDamage(targetSystemType: PoweredSystemType, amount: Double) {
        val hullDamage = shieldHandler.takeDamageAndReportHullDamage(amount)
        if (hullDamage > 0.0) {
            hull -= hullDamage
            powerHandler.takeDamage(targetSystemType, amount)
        }
    }

    fun toPlayerShipMessage() =
        PlayerShipMessage(
            id = id,
            name = designation,
            shipClass = template.className
        )

    fun toMessage() =
        ShipMessage(
            id = id,
            model = template.model,
            designation = designation,
            shipClass = template.className,
            speed = speed.twoDigits(),
            position = position.twoDigits(),
            rotation = rotation.fiveDigits(),
            heading = rotation.toHeading().twoDigits(),
            velocity = speed.length().twoDigits(),
            throttle = throttle,
            thrust = thrust.twoDigits(),
            rudder = rudder,
            history = history.map { it.second.twoDigits() },
            shortRangeScopeRange = template.shortRangeScopeRange,
            waypoints = waypoints.map { it.toWaypointMessage(this) },
            mapSelection = mapSelection.let { if (it is MapSelection.Ship) it.targetId else null },
            scanProgress = scanHandler?.toMessage(),
            lockProgress = lockHandler?.toMessage() ?: LockStatus.NoLock,
            beams = beamHandlers.map { it.toMessage(lockHandler) },
            shield = shieldHandler.toMessage(),
            hull = hull.twoDigits(),
            hullMax = template.hull,
            jumpDrive = jumpHandler.toMessage(),
            powerMessage = powerHandler.toMessage(),
            mainScreenView = mainScreenView,
            frontCamera = template.frontCamera.toMessage()
        )

    fun toShortRangeScopeMessage() =
        ShortRangeScopeMessage(
            shortRangeScopeRange = template.shortRangeScopeRange,
            rotation = rotation.fiveDigits(),
            history = history.map { (it.second - position).twoDigits() },
            waypoints = waypoints.map { it.toWaypointMessage(this) },
            lockProgress = lockHandler?.toMessage() ?: LockStatus.NoLock,
            beams = beamHandlers.map { it.toMessage(lockHandler) }
        )

    fun toScopeContactMessage(relativeTo: Ship) =
        ScopeContactMessage(
            id = id,
            type = getContactType(relativeTo),
            designation = designation,
            relativePosition = (position - relativeTo.position),
            rotation = rotation,
            locked = relativeTo.isLocking(id)
        )

    fun toContactMessage(relativeTo: Ship) =
        ContactMessage(
            id = id,
            model = template.model,
            type = getContactType(relativeTo),
            scanLevel = relativeTo.getScanLevel(id),
            designation = designation,
            position = position,
            relativePosition = (position - relativeTo.position),
            rotation = rotation,
            bearing = (position - relativeTo.position).angle().toHeading(),
            beams = beamHandlers.map { it.toMessage(lockHandler) },
            hullRatio = (hull / template.hull).fiveDigits(),
            shield = shieldHandler.toMessage(),
            jumpAnimation = jumpHandler.toMessage().animation
        )

    fun toPowerMessage() = powerHandler.toMessage()

    fun toJumpDriveMessage() = jumpHandler.toMessage()

    fun toShieldMessage() = shieldHandler.toMessage()

    private fun canIncreaseScanLevel(targetId: ObjectId) = getScanLevel(targetId).canBeIncreased

    private fun getScanLevel(targetId: ObjectId) = scans[targetId] ?: ScanLevel.None

    private fun getContactType(relativeTo: Ship) =
        if (relativeTo.getScanLevel(id) == ScanLevel.Basic) {
            ContactType.Friendly
        } else {
            ContactType.Unknown
        }

    private fun isLocking(targetId: ObjectId) =
        if (lockHandler != null) {
            lockHandler?.targetId == targetId
        } else {
            false
        }

    private val PoweredSystemType.boostLevel
        get() = powerHandler.getBoostLevel(this)
}

private class Waypoint(
    val index: Int,
    val position: Vector2
) {

    fun toWaypointMessage(relativeTo: Ship) =
        WaypointMessage(
            index = index,
            name = "WP$index",
            position = position.twoDigits(),
            relativePosition = (position - relativeTo.position).twoDigits(),
            bearing = (position - relativeTo.position).angle().toHeading().twoDigits()
        )
}

private sealed class MapSelection {

    fun isWaypointSelected(indexToCheck: Int) =
        this is Waypoint && index == indexToCheck

    fun isShipSelected(shipId: ObjectId) =
        this is Ship && targetId == shipId

    object None : MapSelection()

    data class Waypoint(
        val index: Int
    ) : MapSelection()

    data class Ship(
        val targetId: ObjectId
    ) : MapSelection()
}

data class ShipUpdateResult(
    val id: ObjectId,
    val destroyed: Boolean
)
