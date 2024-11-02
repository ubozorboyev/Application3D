package hu.elte.application

import io.github.sceneview.ar.Scene
import io.github.sceneview.collision.Vector3
import kotlin.math.sqrt


fun Vector3.distance(vector: Vector3): Float {
    val dx = x - vector.x
    val dy = y - vector.y
    val dz = z - vector.z
    return sqrt(dx * dx + dy * dy + dz * dz)
}