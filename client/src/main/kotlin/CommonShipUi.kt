import de.bissell.starcruiser.Command
import de.bissell.starcruiser.SnapshotMessage
import de.bissell.starcruiser.Station
import de.bissell.starcruiser.Station.*
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import kotlin.browser.document
import kotlin.dom.addClass
import kotlin.dom.removeClass

class CommonShipUi {

    private val root = document.getElementById("common-ship-ui")!! as HTMLElement
    private val exitButton = root.querySelector(".exit")!! as HTMLButtonElement
    private val fullScreenButton = root.querySelector(".fullscreen")!! as HTMLButtonElement
    private val stationButtons = mapOf(
        Helm to root.querySelector(".switchToHelm")!! as HTMLButtonElement,
        Weapons to root.querySelector(".switchToWeapons")!! as HTMLButtonElement,
        Navigation to root.querySelector(".switchToNavigation")!! as HTMLButtonElement,
        MainScreen to root.querySelector(".switchToMainScreen")!! as HTMLButtonElement
    )
    private val extraButtons = mutableListOf<ExtraButton>()

    private var currentStation: Station = Helm

    init {
        exitButton.onclick = { clientSocket.send(Command.CommandExitShip) }
        fullScreenButton.onclick = {
            val body = document.querySelector("body")!! as HTMLElement
            if (document.fullscreenElement == null) {
                body.requestFullscreen()
                fullScreenButton.innerText = "Windowed"
            } else {
                document.exitFullscreen()
                fullScreenButton.innerText = "Fullscreen"
            }
        }

        stationButtons.forEach {
            it.value.onclick = { _ ->
                clientSocket.send(Command.CommandChangeStation(it.key))
            }
        }
    }

    fun show() {
        root.style.visibility = "visible"
    }

    fun hide() {
        root.style.visibility = "hidden"
    }

    fun draw(snapshot: SnapshotMessage.ShipSnapshot) {
        val newStation = when (snapshot) {
            is SnapshotMessage.Weapons -> Weapons
            is SnapshotMessage.Navigation -> Navigation
            is SnapshotMessage.MainScreen -> MainScreen
            else -> Helm
        }

        if (newStation != currentStation) {
            stationButtons[currentStation]?.removeClass("current")
            stationButtons[newStation]?.addClass("current")

            extraButtons.forEach {
                if (it.isVisibleAtStation(newStation)) {
                    it.element.style.display = "block"
                } else {
                    it.element.style.display = "none"
                }
            }

            currentStation = newStation
        }
    }

    fun addExtraButtons(vararg buttons: ExtraButton) {
        buttons.forEach { extraButton ->
            extraButtons += extraButton

            extraButton.element.apply {
                if (!extraButton.isVisibleAtStation(Helm)) {
                    style.display = "none"
                }
                onclick = { extraButton.callback() }
            }
        }
    }
}

data class ExtraButton(
    val element: HTMLButtonElement,
    val callback: () -> Unit,
    val stations: List<Station>
) {

    constructor(
        selector: String,
        callback: () -> Unit,
        vararg station: Station
    ) : this(
        document.querySelector(selector)!! as HTMLButtonElement,
        callback,
        station.toList()
    )

    fun isVisibleAtStation(station: Station) = stations.contains(station)
}
