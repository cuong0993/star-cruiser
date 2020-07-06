package de.bissell.starcruiser

import de.bissell.starcruiser.ClientState.InShip
import de.bissell.starcruiser.ClientState.ShipDestroyed
import de.bissell.starcruiser.ClientState.ShipSelection
import de.bissell.starcruiser.Station.Helm
import de.bissell.starcruiser.Station.MainScreen
import de.bissell.starcruiser.Station.Navigation
import de.bissell.starcruiser.Station.Weapons
import de.bissell.starcruiser.client.ClientId
import de.bissell.starcruiser.ships.Ship
import java.time.Instant
import java.time.Instant.now
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.random.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.actor

sealed class GameStateChange

object Update : GameStateChange()
object TogglePause : GameStateChange()
object SpawnShip : GameStateChange()
class JoinShip(val clientId: ClientId, val objectId: ObjectId, val station: Station) : GameStateChange()
class ChangeStation(val clientId: ClientId, val station: Station) : GameStateChange()
class ExitShip(val clientId: ClientId) : GameStateChange()
class NewGameClient(val clientId: ClientId) : GameStateChange()
class GameClientDisconnected(val clientId: ClientId) : GameStateChange()
class ChangeThrottle(val clientId: ClientId, val value: Int) : GameStateChange()
class ChangeJumpDistance(val clientId: ClientId, val value: Double) : GameStateChange()
class StartJump(val clientId: ClientId) : GameStateChange()
class ChangeRudder(val clientId: ClientId, val value: Int) : GameStateChange()
class GetGameStateSnapshot(val clientId: ClientId, val response: CompletableDeferred<SnapshotMessage>) : GameStateChange()
class AddWaypoint(val clientId: ClientId, val position: Vector2) : GameStateChange()
class DeleteWaypoint(val clientId: ClientId, val index: Int) : GameStateChange()
class ScanShip(val clientId: ClientId, val targetId: ObjectId) : GameStateChange()
class LockTarget(val clientId: ClientId, val targetId: ObjectId) : GameStateChange()
class SetShieldsUp(val clientId: ClientId, val activated: Boolean) : GameStateChange()

class GameState {

    private var time = GameTime()
    private val ships = mutableMapOf<ObjectId, Ship>()
    private val asteroids = mutableListOf<Asteroid>()
    private val clients = mutableMapOf<ClientId, Client>()

    private val physicsEngine = PhysicsEngine()

    init {
        repeat(50) {
            spawnAsteroid()
        }
    }

    fun toMessage(clientId: ClientId): SnapshotMessage {
        val client = getClient(clientId)
        return when (val state = client.state) {
            is ShipSelection -> SnapshotMessage.ShipSelection(
                playerShips = ships.values.map(Ship::toPlayerShipMessage)
            )
            is ShipDestroyed -> SnapshotMessage.ShipDestroyed
            is InShip -> {
                val ship = state.ship
                when (state.station) {
                    Helm -> SnapshotMessage.Helm(
                        ship = ship.toMessage(),
                        contacts = getScopeContacts(ship),
                        asteroids = getScopeAsteroids(ship)
                    )
                    Weapons -> SnapshotMessage.Weapons(
                        ship = ship.toMessage(),
                        contacts = getScopeContacts(ship),
                        asteroids = getScopeAsteroids(ship)
                    )
                    Navigation -> SnapshotMessage.Navigation(
                        ship = ship.toMessage(),
                        contacts = getContacts(ship),
                        asteroids = getAsteroids(ship)
                    )
                    MainScreen -> SnapshotMessage.MainScreen(
                        ship = ship.toMessage(),
                        contacts = getContacts(ship),
                        asteroids = getAsteroids(ship)
                    )
                }
            }
        }
    }

    fun clientConnected(clientId: ClientId) {
        getClient(clientId)
    }

    fun clientDisconnected(clientId: ClientId) {
        clients.remove(clientId)
    }

    fun joinShip(clientId: ClientId, objectId: ObjectId, station: Station) {
        ships[objectId]?.also {
            getClient(clientId).joinShip(it, station)
        }
    }

    fun changeStation(clientId: ClientId, station: Station) {
        getClient(clientId).changeStation(station)
    }

    fun exitShip(clientId: ClientId) {
        getClient(clientId).exitShip()
    }

    fun spawnShip() {
        Ship(
            position = Vector2.random(300),
            rotation = Random.nextDouble(PI * 2.0)
        ).also {
            it.addWaypoint(Vector2.random(1000, 500))
            it.addWaypoint(Vector2.random(1000, 500))
            ships[it.id] = it
            physicsEngine.addShip(it)
        }
    }

    private fun spawnAsteroid() {
        Asteroid(
            position = Vector2.random(800, 200),
            rotation = Random.nextDouble(PI * 2.0),
            radius = Random.nextDouble(8.0, 32.0)
        ).also {
            asteroids += it
            physicsEngine.addAsteroid(it)
        }
    }

    fun togglePaused() {
        time.paused = !time.paused
    }

    fun update() {
        if (time.paused) return

        time.update(now())

        physicsEngine.step(time)
        updateShips()
        updateAsteroids()
    }

    private fun updateShips() {
        ships.forEach {
            it.value.apply {
                update(time, physicsEngine) { id -> ships[id] }
            }
        }
        ships.map {
            it.value.endUpdate(physicsEngine)
        }.filter {
            it.destroyed
        }.forEach {
            destroyShip(it.id)
        }
    }

    private fun updateAsteroids() {
        asteroids.forEach {
            it.update(physicsEngine)
        }
    }

    fun changeThrottle(clientId: ClientId, value: Int) {
        getClientShip(clientId)?.changeThrottle(value)
    }

    fun changeJumpDistance(clientId: ClientId, value: Double) {
        getClientShip(clientId)?.changeJumpDistance(value)
    }

    fun startJump(clientId: ClientId) {
        getClientShip(clientId)?.startJump()
    }

    fun changeRudder(clientId: ClientId, value: Int) {
        getClientShip(clientId)?.changeRudder(value)
    }

    fun addWaypoint(clientId: ClientId, position: Vector2) {
        getClientShip(clientId)?.addWaypoint(position)
    }

    fun deleteWaypoint(clientId: ClientId, index: Int) {
        getClientShip(clientId)?.deleteWaypoint(index)
    }

    fun scanShip(clientId: ClientId, targetId: ObjectId) {
        ships[targetId]?.also {
            getClientShip(clientId)?.startScan(targetId)
        }
    }

    fun lockTarget(clientId: ClientId, targetId: ObjectId) {
        ships[targetId]?.also {
            getClientShip(clientId)?.lockTarget(targetId)
        }
    }

    fun setShieldsUp(clientId: ClientId, value: Boolean) {
        getClientShip(clientId)?.setShieldsUp(value)
    }

    private fun getClient(clientId: ClientId) =
        clients.computeIfAbsent(clientId) { Client(clientId) }

    private fun getClientShip(clientId: ClientId): Ship? =
        getClient(clientId).state.let {
            if (it is InShip) it.ship else null
        }

    private fun getContacts(clientShip: Ship): List<ContactMessage> {
        return ships
            .filter { it.key != clientShip.id }
            .map { it.value }
            .map { it.toContactMessage(clientShip) }
    }

    private fun getAsteroids(clientShip: Ship): List<AsteroidMessage> {
        return asteroids
            .map { it.toMessage(clientShip) }
    }

    private fun getScopeContacts(clientShip: Ship): List<ScopeContactMessage> {
        return ships
            .filter { it.key != clientShip.id }
            .map { it.value }
            .map { it.toScopeContactMessage(clientShip) }
            .filter {
                it.relativePosition.length() < clientShip.template.shortRangeScopeRange * 1.1
            }
    }

    private fun getScopeAsteroids(clientShip: Ship): List<ScopeAsteroidMessage> {
        return asteroids
            .map { it.toScopeMessage(clientShip) }
            .filter {
                it.relativePosition.length() < clientShip.template.shortRangeScopeRange * 1.1
            }
    }

    private fun destroyShip(shipId: ObjectId) {
        clients.values.filter { client ->
            client.state.let { it is InShip && it.ship.id == shipId }
        }.forEach {
            it.shipDestroyed()
        }
        ships.values.forEach {
            it.targetDestroyed(shipId)
        }
        ships.remove(shipId)
    }

    companion object {
        fun CoroutineScope.gameStateActor() = actor<GameStateChange> {
            val gameState = GameState()
            for (change in channel) {
                when (change) {
                    is Update -> gameState.update()
                    is TogglePause -> gameState.togglePaused()
                    is GetGameStateSnapshot -> change.response.complete(gameState.toMessage(change.clientId))
                    is NewGameClient -> gameState.clientConnected(change.clientId)
                    is GameClientDisconnected -> gameState.clientDisconnected(change.clientId)
                    is JoinShip -> gameState.joinShip(change.clientId, change.objectId, change.station)
                    is ChangeStation -> gameState.changeStation(change.clientId, change.station)
                    is ExitShip -> gameState.exitShip(change.clientId)
                    is SpawnShip -> gameState.spawnShip()
                    is ChangeThrottle -> gameState.changeThrottle(change.clientId, change.value)
                    is ChangeJumpDistance -> gameState.changeJumpDistance(change.clientId, change.value)
                    is StartJump -> gameState.startJump(change.clientId)
                    is ChangeRudder -> gameState.changeRudder(change.clientId, change.value)
                    is AddWaypoint -> gameState.addWaypoint(change.clientId, change.position)
                    is DeleteWaypoint -> gameState.deleteWaypoint(change.clientId, change.index)
                    is ScanShip -> gameState.scanShip(change.clientId, change.targetId)
                    is LockTarget -> gameState.lockTarget(change.clientId, change.targetId)
                    is SetShieldsUp -> gameState.setShieldsUp(change.clientId, change.activated)
                }
            }
        }
    }
}

class GameTime {

    private var lastUpdate: Instant? = null

    var current: Double = 0.0
        private set

    var delta: Double = 0.001
        private set

    var paused: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                lastUpdate = null
            }
        }

    fun update(now: Instant) {
        delta = lastUpdate?.let {
            (it.until(now, ChronoUnit.MILLIS)) / 1000.0
        } ?: 0.001
        current += delta
        lastUpdate = now
    }
}

data class Client(
    val id: ClientId
) {

    var state: ClientState = ShipSelection
        private set

    fun joinShip(ship: Ship, station: Station) {
        state = InShip(
            ship = ship,
            station = station
        )
    }

    fun changeStation(station: Station) {
        val currentState = state
        if (currentState is InShip) {
            state = InShip(
                ship = currentState.ship,
                station = station
            )
        }
    }

    fun shipDestroyed() {
        state = ShipDestroyed
    }

    fun exitShip() {
        state = ShipSelection
    }
}

sealed class ClientState {

    object ShipSelection : ClientState()

    object ShipDestroyed : ClientState()

    data class InShip(
        val ship: Ship,
        val station: Station
    ) : ClientState()
}
