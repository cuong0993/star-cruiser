package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.MapSelectionMessage
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.drawRect
import de.stefanbissell.starcruiser.formatThousands
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandlerParent
import de.stefanbissell.starcruiser.pad
import de.stefanbissell.starcruiser.toPercent
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.RIGHT

class SelectionDetails(
    private val canvas: HTMLCanvasElement,
    private val xExpr: CanvasDimensions.() -> Double = { width - vmin * 42 },
    private val yExpr: CanvasDimensions.() -> Double = { height - vmin * 2 },
    private val widthExpr: CanvasDimensions.() -> Double = { vmin * 40 },
    private val heightExpr: CanvasDimensions.() -> Double = { vmin * 62 },
    private val onScan: () -> Unit,
    private val onDelete: () -> Unit
) : PointerEventHandlerParent() {

    private val ctx: CanvasRenderingContext2D = canvas.context2D
    private val hullDisplay = HullDisplay(
        canvas = canvas,
        xExpr = { innerX },
        yExpr = { dim.topY + vmin * 22 },
        widthExpr = { dim.width - vmin * 8 },
        heightExpr = { vmin * 4 }
    )
    private val shieldsDisplay = ShieldsDisplay(
        canvas = canvas,
        xExpr = { innerX },
        yExpr = { dim.topY + vmin * 28 },
        widthExpr = { dim.width - vmin * 8 },
        heightExpr = { vmin * 4 }
    )
    private val detailsButton = CanvasButton(
        canvas = canvas,
        xExpr = { dim.rightX - vmin * 14 },
        yExpr = { dim.topY + vmin * 7 },
        widthExpr = { vmin * 12 },
        heightExpr = { vmin * 5 },
        onClick = { detailsButtonClicked() },
        activated = { showDetails },
        initialText = "Details"
    )
    private val actionButton = CanvasButton(
        canvas = canvas,
        xExpr = { dim.leftX + dim.width * 0.5 - vmin * 12 },
        yExpr = { dim.bottomY - vmin * 5 },
        widthExpr = { vmin * 24 },
        onClick = { actionButtonClicked() }
    )

    private var dim = calculateComponentDimensions()
    private var mapSelection: MapSelectionMessage? = null
    private val visible
        get() = mapSelection != null
    private val innerX
        get() = dim.leftX + dim.canvas.vmin * 4
    private var showDetails = false

    init {
        addChildren(detailsButton, actionButton)
    }

    override fun isInterestedIn(pointerEvent: PointerEvent): Boolean {
        return with(pointerEvent.point) {
            visible && x > dim.leftX && y > dim.topY &&
                x < dim.rightX && y < dim.bottomY
        }
    }

    fun draw(mapSelection: MapSelectionMessage?) {
        this.mapSelection = mapSelection?.also {
            ctx.draw(it)
        }
    }

    private fun detailsButtonClicked() {
        showDetails = !showDetails
    }

    private fun actionButtonClicked() {
        mapSelection?.apply {
            when {
                canScan -> onScan()
                canDelete -> onDelete()
            }
        }
    }

    private fun CanvasRenderingContext2D.draw(mapSelection: MapSelectionMessage) {
        dim = calculateComponentDimensions()

        drawBase()
        drawDesignation(mapSelection.label)

        val hasDetails = mapSelection.systemsDamage != null
        if (showDetails && hasDetails) {
            drawDetails(mapSelection)
        } else {
            drawBasic(mapSelection)
        }
        if (hasDetails) {
            detailsButton.draw()
        }
        when {
            mapSelection.canScan -> {
                actionButton.text = "Scan"
                actionButton.draw()
            }
            mapSelection.canDelete -> {
                actionButton.text = "Delete"
                actionButton.draw()
            }
        }
    }

    private fun CanvasRenderingContext2D.drawDetails(mapSelection: MapSelectionMessage) {
        mapSelection.systemsDamage?.entries?.forEachIndexed { index, system ->
            CanvasProgress(
                canvas = canvas,
                xExpr = { innerX },
                yExpr = { dim.topY + vmin * 14 + vmin * 5 * index },
                widthExpr = { dim.width - vmin * 8 },
                heightExpr = { vmin * 4 },
            ).apply {
                progress = system.value
                leftText = system.key.label
                rightText = "${system.value.toPercent()}%"
            }.draw()
        }
    }

    private fun CanvasRenderingContext2D.drawBasic(mapSelection: MapSelectionMessage) {
        drawBearing(mapSelection.bearing)
        drawRange(mapSelection.range)

        mapSelection.hullRatio?.also {
            hullDisplay.draw(it)
        }
        mapSelection.shield?.also {
            shieldsDisplay.draw(it)
        }
        mapSelection.shieldModulation?.also {
            drawShieldModulation(it)
        }
        mapSelection.beamModulation?.also {
            drawBeamModulation(it)
        }
    }

    private fun CanvasRenderingContext2D.drawBase() {
        save()

        fillStyle = UiStyle.buttonBackgroundColor
        beginPath()
        drawRect(dim)
        fill()

        lineWidth = dim.lineWidth
        strokeStyle = UiStyle.buttonForegroundColor
        beginPath()
        drawRect(dim)
        stroke()

        restore()
    }

    private fun CanvasRenderingContext2D.drawDesignation(designation: String) {
        save()

        font = UiStyle.boldFont(dim.canvas.vmin * 3)
        fillStyle = UiStyle.buttonForegroundColor
        fillText(designation, innerX, dim.topY + dim.canvas.vmin * 6)

        restore()
    }

    private fun CanvasRenderingContext2D.drawBearing(bearing: Int) {
        val text = bearing.pad(3)
        drawAttribute(dim.canvas.vmin * 12, "Bearing", text)
    }

    private fun CanvasRenderingContext2D.drawRange(range: Int) {
        val text = range.formatThousands()
        drawAttribute(dim.canvas.vmin * 16, "Range", text)
    }

    private fun CanvasRenderingContext2D.drawShieldModulation(modulation: Int) {
        val value = modulation * 2 + 78
        val text = "$value PHz"
        drawAttribute(dim.canvas.vmin * 32, "∿ Shields", text)
    }

    private fun CanvasRenderingContext2D.drawBeamModulation(modulation: Int) {
        val value = modulation * 2 + 78
        val text = "$value PHz"
        drawAttribute(dim.canvas.vmin * 36, "∿ Beams", text)
    }

    private fun CanvasRenderingContext2D.drawAttribute(
        yOffset: Double,
        name: String,
        value: String
    ) {
        val rightX = dim.rightX - dim.canvas.vmin * 4
        val y = dim.topY + yOffset

        save()

        font = UiStyle.font(dim.canvas.vmin * 3)
        fillStyle = UiStyle.buttonForegroundColor
        fillText(name, innerX, y)

        textAlign = CanvasTextAlign.RIGHT
        fillText(value, rightX, y)

        restore()
    }

    private fun calculateComponentDimensions() =
        ComponentDimensions.calculateRect(
            canvas = canvas,
            xExpr = xExpr,
            yExpr = yExpr,
            widthExpr = widthExpr,
            heightExpr = heightExpr
        )
}
