package com.exposures.model

/** The frame number the next exposure logged against this roll should use. */
fun List<Exposure>.nextFrameNumber(): Int = (maxOfOrNull { it.frameNumber } ?: 0) + 1

/** Whether a roll has reached its target frame count given how many exposures have been logged against it. */
fun FilmRoll.isComplete(exposureCount: Int): Boolean = exposureCount >= targetFrameCount
