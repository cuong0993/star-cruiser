package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.Command.CommandChangeJumpDistance
import de.stefanbissell.starcruiser.Command.CommandChangeRudder
import de.stefanbissell.starcruiser.Command.CommandChangeThrottle
import de.stefanbissell.starcruiser.Command.CommandStartJump
import de.stefanbissell.starcruiser.components.CanvasButton
import de.stefanbissell.starcruiser.components.CanvasSlider
import de.stefanbissell.starcruiser.components.JumpDisplay
import de.stefanbissell.starcruiser.components.ShortRangeScope
import org.w3c.dom.CanvasRenderingContext2D
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class HelmUi : CanvasUi(Station.Helm, "helm-ui") {

    private val shortRangeScope = ShortRangeScope(canvas)
    private val throttleSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { it.vmin * 3 },
        yExpr = { it.height - it.vmin * 3 },
        widthExpr = { it.vmin * 10 },
        heightExpr = { it.vmin * 60 },
        onChange = {
            val throttle = min(10.0, max(-10.0, it * 20.0 - 10.0)).roundToInt() * 10
            clientSocket.send(CommandChangeThrottle(throttle))
        },
        lines = listOf(0.5),
        leftText = "Impulse"
    )
    private val jumpSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { it.vmin * 15 },
        yExpr = { it.height - it.vmin * 3 },
        widthExpr = { it.vmin * 10 },
        heightExpr = { it.vmin * 60 },
        onChange = {
            clientSocket.send(CommandChangeJumpDistance(it))
        },
        leftText = "Distance"
    )
    private val rudderSlider = CanvasSlider(
        canvas = canvas,
        xExpr = { it.width - it.vmin * 63 },
        yExpr = { it.height - it.vmin * 3 },
        widthExpr = { it.vmin * 60 },
        heightExpr = { it.vmin * 10 },
        onChange = {
            val rudder = min(10.0, max(-10.0, it * 20.0 - 10.0)).roundToInt() * 10
            clientSocket.send(CommandChangeRudder(rudder))
        },
        lines = listOf(0.5),
        leftText = "Rudder",
        reverseValue = true
    )
    private val jumpDisplay = JumpDisplay(
        canvas = canvas,
        xExpr = { it.vmin * 27 },
        yExpr = { it.height - if (it.width >= it.vmin * 125) it.vmin * 15 else it.vmin * 27 }
    )
    private val jumpButton = CanvasButton(
        canvas = canvas,
        xExpr = { it.vmin * 34 },
        yExpr = { it.height - if (it.width >= it.vmin * 125) it.vmin * 3 else it.vmin * 15 },
        widthExpr = { it.vmin * 20 },
        heightExpr = { it.vmin * 10 },
        onClick = { clientSocket.send(CommandStartJump) },
        text = { "Jump" }
    )

    init {
        pointerEventDispatcher.addHandlers(
            throttleSlider,
            jumpSlider,
            rudderSlider,
            jumpButton,
            shortRangeScope.rotateButton
        )
    }

    fun draw(snapshot: SnapshotMessage.Helm) {
        val ship = snapshot.ship

        ctx.draw(snapshot, ship)
    }

    private fun CanvasRenderingContext2D.draw(snapshot: SnapshotMessage.Helm, ship: ShipMessage) {
        transformReset()
        clear("#222")

        shortRangeScope.draw(snapshot)

        drawThrottle(ship)
        drawJump(ship)
        drawRudder(ship)
    }

    private fun drawThrottle(ship: ShipMessage) =
        throttleSlider.draw((ship.throttle + 100) / 200.0)

    private fun drawJump(ship: ShipMessage) {
        jumpSlider.draw(ship.jumpDrive.ratio)
        jumpDisplay.draw(ship.jumpDrive)
        jumpButton.draw()
    }

    private fun drawRudder(ship: ShipMessage) =
        rudderSlider.draw((ship.rudder + 100) / 200.0)
}