package com.exposures.phone.export

import com.exposures.database.repository.EquipmentRepository
import kotlinx.coroutines.flow.first

/** Builds CSV exports from the local repository — see [ExposureCsvWriter] for the actual format. */
class CsvExportCoordinator(private val repository: EquipmentRepository) {

    /** Exports a single roll's exposures, or null if the roll doesn't exist. */
    suspend fun exportRoll(rollId: String): String? {
        val roll = repository.getFilmRoll(rollId) ?: return null
        val exposures = repository.observeExposures(rollId).first()
        val lensNames = repository.observeLenses().first().associate { it.id to it.name }
        return ExposureCsvWriter.write(exposures, mapOf(rollId to roll.name), lensNames)
    }

    /** Exports every roll's exposures into one flat CSV, distinguished by the Roll column. */
    suspend fun exportAll(): String {
        val exposures = repository.observeAllExposures().first()
        val rollNames = repository.observeFilmRolls().first().associate { it.id to it.name }
        val lensNames = repository.observeLenses().first().associate { it.id to it.name }
        return ExposureCsvWriter.write(exposures, rollNames, lensNames)
    }
}
