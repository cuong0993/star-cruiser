@file:UseSerializers(BigDecimalSerializer::class)

package de.bissell.starcruiser

import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.util.UUID

class GameClient(
    private val id: UUID = UUID.randomUUID(),
    private val gameStateActor: SendChannel<GameStateChange>,
    private val outgoing: SendChannel<Frame>,
    private val incoming: ReceiveChannel<Frame>
) {

    suspend fun start(coroutineScope: CoroutineScope) {
        gameStateActor.send(NewGameClient(id))

        val updateJob = coroutineScope.launch {
            while (isActive) {
                val response = CompletableDeferred<GameStateMessage>()
                gameStateActor.send(GetGameStateMessage(id, response))
                outgoing.sendText(response.await().toJson())
                delay(200)
            }
        }

        for (frame in incoming) {
            val input = String(frame.data)
            when (val command = Command.parse(input)) {
                is Command.CommandTogglePause -> gameStateActor.send(TogglePause)

                is Command.CommandChangeThrottle -> gameStateActor.send(ChangeThrottle(id, BigDecimal(command.diff)))
                is Command.CommandChangeRudder -> gameStateActor.send(ChangeRudder(id, BigDecimal(command.diff)))
            }
        }

        updateJob.cancelAndJoin()
        gameStateActor.send(GameClientDisconnected(id))
    }

    companion object {
        suspend fun CoroutineScope.startGameClient(
            gameStateActor: SendChannel<GameStateChange>,
            outgoing: SendChannel<Frame>,
            incoming: ReceiveChannel<Frame>
        ) =
            GameClient(
                gameStateActor = gameStateActor,
                outgoing = outgoing,
                incoming = incoming
            ).start(this)
    }
}

@Serializable
sealed class Command {

    @Serializable
    object CommandTogglePause : Command()
    @Serializable
    class CommandChangeThrottle(val diff: Long) : Command()
    @Serializable
    class CommandChangeRudder(val diff: Long) : Command()

    companion object {
        fun parse(input: String): Command = Json(jsonConfiguration).parse(serializer(), input)
    }
}

@Serializable
data class GameStateMessage(
    val paused: Boolean,
    val ship: ShipMessage,
    val contacts: List<ContactMessage>
) {
    fun toJson(): String = Json(jsonConfiguration).stringify(serializer(), this)
}

@Serializable
data class ShipMessage(
    val position: Vector2,
    val speed: Vector2,
    val rotation: BigDecimal,
    val heading: BigDecimal,
    val velocity: BigDecimal,
    val throttle: BigDecimal,
    val rudder: BigDecimal,
    val history: List<Pair<BigDecimal, Vector2>>
)

@Serializable
data class ContactMessage(
    val position: Vector2,
    val relativePosition: Vector2,
    val speed: Vector2,
    val rotation: BigDecimal,
    val heading: BigDecimal,
    val velocity: BigDecimal,
    val history: List<Pair<BigDecimal, Vector2>>
)
