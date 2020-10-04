package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.ApplicationConfig.gameStateUpdateIntervalMillis
import de.stefanbissell.starcruiser.GameState.Companion.gameStateActor
import de.stefanbissell.starcruiser.client.GameClient.Companion.startGameClient
import de.stefanbissell.starcruiser.client.createStatisticsActor
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.ContentNegotiation
import io.ktor.http.cio.websocket.pingPeriod
import io.ktor.http.cio.websocket.timeout
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.netty.EngineMain
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.seconds
import kotlin.time.toJavaDuration

fun main(args: Array<String>): Unit = EngineMain.main(args)

object ApplicationConfig {

    const val gameStateUpdateIntervalMillis: Long = 20
    const val gameClientUpdateIntervalMillis: Long = 10
    const val gameClientMaxInflightMessages: Int = 3
}

@Suppress("unused") // Referenced in application.conf
fun Application.module() {

    val gameStateActor = gameStateActor()
    val statisticsActor = createStatisticsActor()

    launch {
        while (isActive) {
            gameStateActor.send(Update)
            delay(gameStateUpdateIntervalMillis)
        }
    }

    install(WebSockets) {
        pingPeriod = 15.seconds.toJavaDuration()
        timeout = 15.seconds.toJavaDuration()
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(ContentNegotiation) {
        configuredJson
    }

    routing {
        webUi()

        get("/restart") {
            gameStateActor.send(Restart)
            call.respondRedirect("/")
        }

        webSocket("/ws/client") {
            startGameClient(
                gameStateActor = gameStateActor,
                statisticsActor = statisticsActor,
                outgoing = outgoing,
                incoming = incoming,
                log = log
            )
        }
    }
}
