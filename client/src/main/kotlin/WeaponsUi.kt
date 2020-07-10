import components.CanvasButton
import components.HullDisplay
import components.ShieldsDisplay
import components.ShortRangeScope
import de.stefanbissell.starcruiser.Command
import de.stefanbissell.starcruiser.Command.CommandLockTarget
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.SnapshotMessage
import de.stefanbissell.starcruiser.Station
import org.w3c.dom.CanvasRenderingContext2D

class WeaponsUi : CanvasUi(Station.Weapons, "weapons-ui") {

    private val shortRangeScope = ShortRangeScope(canvas, true) { contactSelected(it) }
    private val lockTargetButton = CanvasButton(
        canvas = canvas,
        xExpr = { it.width * 0.5 - it.vmin * 45 },
        yExpr = { it.height * 0.5 - it.vmin * 38 },
        widthExpr = { it.vmin * 25 },
        heightExpr = { it.vmin * 10 },
        onClick = { toggleLockTarget() },
        activated = { selectingTarget },
        text = { "Lock on" }
    )
    private val hullDisplay = HullDisplay(
        canvas = canvas,
        xExpr = { it.vmin * 3 },
        yExpr = { it.height - it.vmin * 23 }
    )
    private val shieldsDisplay = ShieldsDisplay(
        canvas = canvas,
        xExpr = { it.vmin * 3 },
        yExpr = { it.height - it.vmin * 15 }
    )
    private val shieldsButton = CanvasButton(
        canvas = canvas,
        xExpr = { it.vmin * 13 },
        yExpr = { it.height - it.vmin * 3 },
        widthExpr = { it.vmin * 20 },
        heightExpr = { it.vmin * 10 },
        onClick = { toggleShields() },
        text = { if (shieldsUp) "Down" else "Up" }
    )

    private var shieldsUp = false
    private var selectingTarget = false

    init {
        pointerEventDispatcher.addHandlers(
            shortRangeScope.rotateButton,
            lockTargetButton,
            shieldsButton
        )
    }

    fun draw(snapshot: SnapshotMessage.Weapons) {
        shieldsUp = snapshot.ship.shield.up

        ctx.draw(snapshot)
    }

    private fun CanvasRenderingContext2D.draw(snapshot: SnapshotMessage.Weapons) {
        transformReset()
        clear("#222")

        shortRangeScope.draw(snapshot)
        lockTargetButton.draw()
        hullDisplay.draw(snapshot.ship)
        shieldsDisplay.draw(snapshot.ship.shield)
        shieldsButton.draw()
    }

    private fun contactSelected(targetId: ObjectId) {
        if (selectingTarget) {
            toggleLockTarget()
            clientSocket.send(CommandLockTarget(targetId))
        }
    }

    private fun toggleShields() {
        clientSocket.send(Command.CommandSetShieldsUp(!shieldsUp))
    }

    private fun toggleLockTarget() {
        selectingTarget = !selectingTarget
    }
}
