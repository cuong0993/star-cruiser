package de.stefanbissell.starcruiser

import de.stefanbissell.starcruiser.ships.Ship
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Mat22
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.World

class PhysicsEngine {

    private val world = World(Vec2())
    private val bodies: MutableMap<BodyId, Body> = mutableMapOf()

    fun step(time: GameTime) = world.step(time.delta.toFloat(), 6, 2)

    fun addShip(ship: Ship) {
        bodies[ship.id.toBodyId()] = ship.toBody()
    }

    fun addAsteroid(asteroid: Asteroid) {
        bodies[asteroid.id.toBodyId()] = asteroid.toBody()
    }

    fun updateShip(objectId: ObjectId, thrust: Double, rudder: Double) {
        bodies[objectId.toBodyId()]?.apply {
            if (thrust != 0.0) {
                applyForceToCenter(
                    Mat22.createRotationalTransform(angle).mul(Vec2(thrust.toFloat(), 0f))
                )
            }
            if (rudder != 0.0) {
                applyTorque(rudder.toFloat())
            }
        }
    }

    fun jumpShip(objectId: ObjectId, distance: Int) {
        bodies[objectId.toBodyId()]?.apply {
            setTransform(
                position.add(Mat22.createRotationalTransform(angle).mul(Vec2(distance.toFloat(), 0f))),
                angle
            )
        }
    }

    fun getBodyParameters(objectId: ObjectId) =
        bodies[objectId.toBodyId()]?.let {
            BodyParameters(
                position = it.position.toVector2(),
                speed = it.linearVelocity.toVector2(),
                rotation = it.angle.toDouble()
            )
        }

    private fun Ship.toBody() =
        createDynamicBody(
            position,
            rotation
        ).apply {
            createFixture(this@toBody)
        }

    private fun Body.createFixture(ship: Ship) {
        createFixture(
            polygonShape(
                Vec2(-14.1f, 3.3f),
                Vec2(-13.24f, 4.7f),
                Vec2(-7f, 4.7f),
                Vec2(-7f, -4.7f),
                Vec2(-13.24f, -4.7f),
                Vec2(-14.1f, -3.3f)
            ),
            ship.template.density.toFloat()
        )
        createFixture(
            polygonShape(
                Vec2(-12.4f, 3.4f),
                Vec2(9.2f, 3.4f),
                Vec2(9.2f, -3.4f),
                Vec2(-12.4f, -3.4f)
            ),
            ship.template.density.toFloat()
        )
        createFixture(
            polygonShape(
                Vec2(9f, 3.7f),
                Vec2(11.9f, 3f),
                Vec2(12.5f, 2.4f),
                Vec2(13f, 1.1f),
                Vec2(13f, -1.1f),
                Vec2(12.5f, -2.4f),
                Vec2(11.9f, -3f),
                Vec2(9f, -3.7f)
            ),
            ship.template.density.toFloat()
        )
    }

    private fun Asteroid.toBody() =
        createDynamicBody(
            position,
            rotation
        ).apply {
            createFixture(this@toBody)
        }

    private fun Body.createFixture(asteroid: Asteroid) {
        createFixture(
            CircleShape().apply {
                radius = asteroid.radius.toFloat()
            },
            0.02f
        )
    }

    private fun polygonShape(vararg points: Vec2) =
        PolygonShape().apply {
            set(points, points.size)
        }

    private fun createDynamicBody(
        position: Vector2,
        rotation: Double
    ) = world.createBody {
        type = BodyType.DYNAMIC
        this.position.set(position.toVec2())
        angle = rotation.toFloat()
        linearDamping = 0.4f
        angularDamping = 0.95f
    }

    private fun Vec2.toVector2(): Vector2 = Vector2(x.toDouble(), y.toDouble())

    private fun Vector2.toVec2(): Vec2 = Vec2(x.toFloat(), y.toFloat())

    private fun World.createBody(block: BodyDef.() -> Unit): Body = createBody(BodyDef().apply(block))

    private fun ObjectId.toBodyId() = BodyId(this.id)
}

data class BodyId(val id: String)

data class BodyParameters(
    val position: Vector2,
    val speed: Vector2,
    val rotation: Double
)