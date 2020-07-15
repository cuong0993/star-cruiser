package de.stefanbissell.starcruiser.components

import de.stefanbissell.starcruiser.AsteroidMessage
import de.stefanbissell.starcruiser.CanvasDimensions
import de.stefanbissell.starcruiser.ContactMessage
import de.stefanbissell.starcruiser.ContactType
import de.stefanbissell.starcruiser.ObjectId
import de.stefanbissell.starcruiser.Positional
import de.stefanbissell.starcruiser.ScanLevel
import de.stefanbissell.starcruiser.ScanProgress
import de.stefanbissell.starcruiser.ShipMessage
import de.stefanbissell.starcruiser.SnapshotMessage
import de.stefanbissell.starcruiser.Vector2
import de.stefanbissell.starcruiser.WaypointMessage
import de.stefanbissell.starcruiser.circle
import de.stefanbissell.starcruiser.clamp
import de.stefanbissell.starcruiser.clear
import de.stefanbissell.starcruiser.context2D
import de.stefanbissell.starcruiser.dimensions
import de.stefanbissell.starcruiser.drawAsteroidSymbol
import de.stefanbissell.starcruiser.drawLockMarker
import de.stefanbissell.starcruiser.drawShipSymbol
import de.stefanbissell.starcruiser.environmentContactStyle
import de.stefanbissell.starcruiser.friendlyContactStyle
import de.stefanbissell.starcruiser.input.PointerEvent
import de.stefanbissell.starcruiser.input.PointerEventHandler
import de.stefanbissell.starcruiser.scanProgressStyle
import de.stefanbissell.starcruiser.selectionMarkerStyle
import de.stefanbissell.starcruiser.shipStyle
import de.stefanbissell.starcruiser.transformReset
import de.stefanbissell.starcruiser.translate
import de.stefanbissell.starcruiser.translateToCenter
import de.stefanbissell.starcruiser.unknownContactStyle
import de.stefanbissell.starcruiser.wayPointStyle
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt

class NavigationMap(
    private val canvas: HTMLCanvasElement,
    private val mapClickListener: (MapClick) -> Unit = {}
) {

    private val ctx = canvas.context2D
    private var dim = CanvasDimensions(100, 100)
    private val mapGrid = MapGrid(canvas)
    private val scanProgressComponent = CanvasProgress(
        canvas = canvas,
        xExpr = { dim.width * 0.5 - dim.vmin * 20 },
        yExpr = { dim.vmin * 10 },
        widthExpr = { dim.vmin * 40 },
        heightExpr = { dim.vmin * 6 },
        backgroundColor = "#111",
        foregroundColor = "#ff6347"
    )
    var center = Vector2()
    var scaleSetting = 3
        private set
    private val scale: Double
        get() = 4.0 / 2.0.pow(scaleSetting.toDouble())

    private var selectedObjectId: ObjectId? = null
    var selectedContact: ContactMessage?
        get() = contacts.firstOrNull { it.id == selectedObjectId }
        set(value) { selectedObjectId = value?.id?.also { selectedWaypointIndex = null } }

    private var selectedWaypointIndex: Int? = null
    var selectedWaypoint: WaypointMessage?
        get() = waypoints.firstOrNull { it.index == selectedWaypointIndex }
        set(value) { selectedWaypointIndex = value?.index?.also { selectedObjectId = null } }

    private var contacts: List<ContactMessage> = emptyList()
    private var asteroids: List<AsteroidMessage> = emptyList()
    private var waypoints: List<WaypointMessage> = emptyList()

    val selection: Selection?
        get() = selectedContact?.let {
            Selection(
                label = it.designation,
                bearing = it.bearing,
                range = it.relativePosition.length(),
                canScan = it.scanLevel != ScanLevel.highest,
                canDelete = false
            )
        } ?: selectedWaypoint?.let {
            Selection(
                label = it.name,
                bearing = it.bearing,
                range = it.relativePosition.length(),
                canScan = false,
                canDelete = true
            )
        }

    fun zoomIn() {
        scaleSetting = (scaleSetting - 1).clamp(0, 6)
    }

    fun zoomOut() {
        scaleSetting = (scaleSetting + 1).clamp(0, 6)
    }

    fun changeZoom(value: Double) {
        scaleSetting = (6.0 - value * 6.0).roundToInt()
    }

    fun draw(snapshot: SnapshotMessage.Navigation) {
        val ship = snapshot.ship
        contacts = snapshot.contacts
        asteroids = snapshot.asteroids
        waypoints = ship.waypoints
        dim = canvas.dimensions()

        with(ctx) {
            transformReset()
            clear("#000")

            drawGrid()
            drawHistory(ship)
            drawAsteroids()
            drawWaypoints(ship)
            drawContacts()
            drawSelectedMarker()
            drawShip(ship)
            drawScanProgress(ship)
        }
    }

    private fun drawGrid() {
        mapGrid.draw(center, scale)
    }

    private fun CanvasRenderingContext2D.drawAsteroids() {
        asteroids.forEach {
            drawAsteroid(it)
        }
    }

    private fun CanvasRenderingContext2D.drawAsteroid(asteroid: AsteroidMessage) {
        save()
        translateToCenter()
        environmentContactStyle(dim)

        translate(asteroid.position.adjustForMap())
        drawAsteroidSymbol(asteroid.rotation, dim.vmin * 0.8 * asteroid.radius * 0.1 * scale)
        restore()
    }

    private fun CanvasRenderingContext2D.drawContacts() {
        contacts.forEach {
            drawContact(it)
        }
    }

    private fun CanvasRenderingContext2D.drawContact(contact: ContactMessage) {
        save()
        translateToCenter()
        when (contact.type) {
            ContactType.Friendly -> friendlyContactStyle(dim)
            else -> unknownContactStyle(dim)
        }

        translate(contact.position.adjustForMap())
        drawShipSymbol(contact.rotation, dim.vmin * 0.8)

        translate(0.0, -dim.vmin * 3)
        fillText(contact.designation, 0.0, 0.0)
        restore()
    }

    private fun CanvasRenderingContext2D.drawSelectedMarker() {
        val pos = selectedContact?.position ?: selectedWaypoint?.position

        pos?.also {
            save()
            selectionMarkerStyle(dim)
            translateToCenter()
            translate(pos.adjustForMap())
            drawLockMarker(dim.vmin * 3)
            restore()
        }
    }

    private fun CanvasRenderingContext2D.drawShip(ship: ShipMessage) {
        save()
        translateToCenter()
        translate(ship.position.adjustForMap())
        shipStyle(dim)
        drawShipSymbol(ship.rotation, dim.vmin * 0.8)
        restore()
    }

    private fun CanvasRenderingContext2D.drawHistory(ship: ShipMessage) {
        save()
        translateToCenter()
        fillStyle = "#222"

        for (point in ship.history) {
            save()
            translate(point.adjustForMap())
            beginPath()
            circle(0.0, 0.0, 2.0)
            fill()
            restore()
        }
        restore()
    }

    private fun CanvasRenderingContext2D.drawWaypoints(ship: ShipMessage) {
        save()
        translateToCenter()
        wayPointStyle(dim)

        for (waypoint in ship.waypoints) {
            save()

            translate(waypoint.position.adjustForMap())
            beginPath()
            circle(0.0, 0.0, dim.vmin * 0.8)
            stroke()

            translate(0.0, -dim.vmin * 2)
            fillText(waypoint.name, 0.0, 0.0)

            restore()
        }
        restore()
    }

    private fun CanvasRenderingContext2D.drawScanProgress(ship: ShipMessage) {
        val progress = ship.scanProgress ?: return
        val contact = contacts.firstOrNull { it.id == progress.targetId } ?: return

        val designation = contact.designation
        drawScanProgressBar(designation, progress)
        drawScanProgressMarker(contact, progress)
    }

    private fun drawScanProgressBar(designation: String, progress: ScanProgress) {
        scanProgressComponent.centerText = "Scanning $designation"
        scanProgressComponent.progress = progress.progress
        scanProgressComponent.draw()
    }

    private fun CanvasRenderingContext2D.drawScanProgressMarker(contact: ContactMessage, progress: ScanProgress) {
        save()
        scanProgressStyle(dim)

        strokeStyle = "#ff6347"
        translateToCenter()
        translate(contact.position.adjustForMap())
        beginPath()
        circle(
            0.0, 0.0, dim.vmin * 2.3,
            -PI * 0.5, PI * progress.progress * 2.0 - PI * 0.5
        )
        stroke()

        restore()
    }

    private fun getNearestWaypoints(vector: Vector2): List<WaypointMessage> = getNearest(waypoints, vector)

    private fun getNearestContacts(vector: Vector2): List<ContactMessage> = getNearest(contacts, vector)

    private fun <T : Positional> getNearest(elements: Iterable<T>, vector: Vector2): List<T> {
        val click = vector - canvasCenter()
        return elements
            .asSequence()
            .map { it to it.position.adjustForMap() }
            .map { it.first to it.second - click }
            .map { it.first to it.second.length() }
            .filter { it.second <= 20.0 }
            .sortedBy { it.second }
            .map { it.first }
            .toList()
    }

    private fun Vector2.toWorld() = ((this - canvasCenter()) / scale).let { Vector2(it.x, -it.y) } + center

    private fun convert(vector: Vector2) = (vector / scale).let { Vector2(-it.x, it.y) }

    private fun Vector2.adjustForMap() = ((this - center) * scale).let { Vector2(it.x, -it.y) }

    private fun canvasCenter() = Vector2(canvas.width * 0.5, canvas.height * 0.5)

    inner class MapPointerEventHandler : PointerEventHandler {

        private var firstEvent: Vector2? = null
        private var previousEvent: Vector2? = null
        private var distance = 0.0

        override fun handlePointerDown(pointerEvent: PointerEvent) {
            firstEvent = pointerEvent.point
            previousEvent = pointerEvent.point
        }

        override fun handlePointerMove(pointerEvent: PointerEvent) {
            handleMove(pointerEvent.point)
        }

        override fun handlePointerUp(pointerEvent: PointerEvent) {
            handleMove(pointerEvent.point)
            if (distance <= 20.0) {
                handleClick()
            }
            firstEvent = null
            previousEvent = null
            distance = 0.0
        }

        private fun handleMove(currentEvent: Vector2) {
            previousEvent?.let {
                val move = currentEvent - it
                center += convert(move)
                distance += move.length()
            }
            previousEvent = currentEvent
        }

        private fun handleClick() {
            firstEvent?.let {
                mapClickListener(
                    MapClick(
                        screen = it,
                        world = it.toWorld(),
                        waypoints = getNearestWaypoints(it),
                        contacts = getNearestContacts(it)
                    )
                )
            }
        }
    }
}

data class MapClick(
    val screen: Vector2,
    val world: Vector2,
    val waypoints: List<WaypointMessage>,
    val contacts: List<ContactMessage>
)

data class Selection(
    val label: String,
    val bearing: Double,
    val range: Double,
    val canScan: Boolean,
    val canDelete: Boolean
)