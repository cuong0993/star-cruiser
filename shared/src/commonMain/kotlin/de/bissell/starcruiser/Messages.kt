package de.bissell.starcruiser

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PrimitiveDescriptor
import kotlinx.serialization.PrimitiveKind
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

val jsonConfiguration = JsonConfiguration.Stable.copy(
    encodeDefaults = true,
    isLenient = true,
    prettyPrint = true
)

@Serializable
data class ShipId(val id: String) {

    @Serializer(forClass = ShipId::class)
    companion object : KSerializer<ShipId> {
        override val descriptor: SerialDescriptor = PrimitiveDescriptor("ShipId", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: ShipId) {
            encoder.encodeString(value.id)
        }

        override fun deserialize(decoder: Decoder): ShipId {
            return ShipId(decoder.decodeString())
        }
    }
}

@Serializable
data class GameStateMessage(
    val counter: Long,
    val snapshot: SnapshotMessage
) {
    fun toJson(): String = Json(jsonConfiguration).stringify(serializer(), this)

    companion object {
        fun parse(input: String): GameStateMessage = Json(jsonConfiguration).parse(serializer(), input)
    }
}

@Serializable
sealed class SnapshotMessage {

    interface ShipSnapshot {
        val ship: ShipMessage
    }

    @Serializable
    data class ShipSelection(
        val playerShips: List<PlayerShipMessage>
    ) : SnapshotMessage()

    @Serializable
    data class Helm(
        override val ship: ShipMessage,
        val contacts: List<ScopeContactMessage>
    ) : SnapshotMessage(), ShipSnapshot

    @Serializable
    data class Navigation(
        override val ship: ShipMessage,
        val contacts: List<ContactMessage>
    ) : SnapshotMessage(), ShipSnapshot

    @Serializable
    data class MainScreen(
        override val ship: ShipMessage,
        val contacts: List<ContactMessage>
    ) : SnapshotMessage(), ShipSnapshot
}

@Serializable
data class PlayerShipMessage(
    val id: ShipId,
    val name: String,
    val shipClass: String?
)

@Serializable
data class ShipMessage(
    val id: ShipId,
    val designation: String,
    val shipClass: String?,
    val position: Vector2,
    val speed: Vector2,
    val rotation: Double,
    val heading: Double,
    val velocity: Double,
    val throttle: Int,
    val thrust: Double,
    val rudder: Int,
    val history: List<Pair<Double, Vector2>>,
    val shortRangeScopeRange: Double,
    val waypoints: List<WaypointMessage>,
    val scanProgress: ScanProgress?
)

@Serializable
data class ContactMessage(
    val id: ShipId,
    val type: ContactType,
    val scanLevel: ScanLevel,
    val designation: String,
    val position: Vector2,
    val relativePosition: Vector2,
    val speed: Vector2,
    val rotation: Double,
    val heading: Double,
    val velocity: Double,
    val history: List<Pair<Double, Vector2>>
)

@Serializable
data class ScopeContactMessage(
    val id: ShipId,
    val type: ContactType,
    val designation: String,
    val relativePosition: Vector2,
    val rotation: Double
)

enum class ContactType {
    Unknown,
    Friendly
}

enum class ScanLevel {
    None,
    Faction;

    fun next() =
        when (this) {
            None -> Faction
            Faction -> Faction
        }
}

@Serializable
data class ScanProgress(
    val targetId: ShipId,
    val progress: Double
)

@Serializable
data class WaypointMessage(
    val index: Int,
    val name: String,
    val position: Vector2,
    val relativePosition: Vector2
)
