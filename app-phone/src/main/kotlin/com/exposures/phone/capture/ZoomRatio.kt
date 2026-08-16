package com.exposures.phone.capture

/** Clamps a lens's requested reference-photo zoom to what this phone's camera actually supports. */
object ZoomRatio {
    fun clamp(requested: Double, min: Float, max: Float): Float = requested.toFloat().coerceIn(min, max)
}
