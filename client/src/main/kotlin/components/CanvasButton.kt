package components

import CanvasDimensions
import drawPill
import input.PointerEvent
import input.PointerEventHandler
import org.w3c.dom.*
import px

class CanvasButton(
    private val canvas: HTMLCanvasElement,
    private val ctx: CanvasRenderingContext2D = canvas.getContext(contextId = "2d")!! as CanvasRenderingContext2D,
    private val xExpr: (CanvasDimensions) -> Double,
    private val yExpr: (CanvasDimensions) -> Double,
    private val widthExpr: (CanvasDimensions) -> Double,
    private val heightExpr: (CanvasDimensions) -> Double,
    private val onClick: () -> Unit = {},
    private val text: String? = null
) : PointerEventHandler {

    private var pressed = false

    fun draw() {
        val dim = currentDimensions(canvas)

        with(ctx) {
            save()

            drawPill(dim)
            drawText(dim)

            restore()
        }
    }

    override fun isInterestedIn(pointerEvent: PointerEvent): Boolean {
        val dim = currentDimensions(canvas)
        val point = pointerEvent.point

        return point.x > dim.bottomX && point.x < dim.bottomX + dim.width
                && point.y > dim.bottomY - dim.height && point.y < dim.bottomY
    }

    override fun handlePointerDown(pointerEvent: PointerEvent) {
        pressed = true
        onClick()
    }

    override fun handlePointerUp(pointerEvent: PointerEvent) {
        pressed = false
    }

    private fun CanvasRenderingContext2D.drawPill(dim: ComponentDimensions) {
        lineWidth = dim.lineWidth
        fillStyle = if (pressed) "#333" else "#111"
        beginPath()
        drawPill(dim.bottomX, dim.bottomY, dim.width, dim.height)
        fill()

        strokeStyle = "#888"
        beginPath()
        drawPill(dim.bottomX, dim.bottomY, dim.width, dim.height)
        stroke()
    }

    private fun CanvasRenderingContext2D.drawText(dim: ComponentDimensions) {
        if (text != null) {
            save()

            fillStyle = "#888"
            textAlign = CanvasTextAlign.CENTER
            textBaseline = CanvasTextBaseline.ALPHABETIC
            translate(dim.bottomX, dim.bottomY)
            val textSize = (dim.height * 0.5).toInt()
            font = "${textSize.px} sans-serif"
            translate(dim.width * 0.5, -dim.height * 0.35)
            fillText(text, 0.0, 0.0)
        }
    }

    private fun currentDimensions(canvas: HTMLCanvasElement) =
        ComponentDimensions.calculate(
            canvas, xExpr, yExpr, widthExpr, heightExpr
        )
}
