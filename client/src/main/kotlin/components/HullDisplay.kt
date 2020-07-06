package components

import CanvasDimensions
import de.bissell.starcruiser.ShipMessage
import kotlin.math.roundToInt
import org.w3c.dom.HTMLCanvasElement

class HullDisplay(
    canvas: HTMLCanvasElement,
    xExpr: (CanvasDimensions) -> Double,
    yExpr: (CanvasDimensions) -> Double,
    widthExpr: (CanvasDimensions) -> Double = { it.vmin * 40 },
    heightExpr: (CanvasDimensions) -> Double = { it.vmin * 6 }
) {

    private val canvasProgress = CanvasProgress(
        canvas = canvas,
        xExpr = xExpr,
        yExpr = yExpr,
        widthExpr = widthExpr,
        heightExpr = heightExpr,
        backgroundColor = "#111",
        foregroundColor = "#888"
    )

    init {
        canvasProgress.leftText = "Hull"
    }

    fun draw(shipMessage: ShipMessage) {
        val progress = shipMessage.hull / shipMessage.hullMax
        canvasProgress.progress = progress
        canvasProgress.rightText = "${(progress * 100).roundToInt()}%"

        canvasProgress.draw()
    }
}
