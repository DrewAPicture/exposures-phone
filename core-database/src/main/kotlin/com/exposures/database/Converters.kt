package com.exposures.database

import androidx.room.TypeConverter
import com.exposures.model.FilmFormat
import com.exposures.model.LightMeterType
import com.exposures.model.PhotoStatus
import com.exposures.model.RollStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.ShutterSpeedKind
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus

/** Room type converters for the enum and value types shared with core-model. */
class Converters {

    @TypeConverter
    fun fromShutterSpeed(value: ShutterSpeed): String = "${value.kind}:${value.numerator}:${value.denominator}"

    @TypeConverter
    fun toShutterSpeed(value: String): ShutterSpeed {
        val (kind, numerator, denominator) = value.split(":")
        return ShutterSpeed(ShutterSpeedKind.valueOf(kind), numerator.toInt(), denominator.toInt())
    }

    @TypeConverter
    fun fromShutterSpeedList(value: List<ShutterSpeed>): String = value.joinToString(";") { fromShutterSpeed(it) }

    @TypeConverter
    fun toShutterSpeedList(value: String): List<ShutterSpeed> =
        if (value.isBlank()) emptyList() else value.split(";").map(::toShutterSpeed)

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter
    fun fromRollStatus(value: RollStatus): String = value.name

    @TypeConverter
    fun toRollStatus(value: String): RollStatus = RollStatus.valueOf(value)

    @TypeConverter
    fun fromPhotoStatus(value: PhotoStatus): String = value.name

    @TypeConverter
    fun toPhotoStatus(value: String): PhotoStatus = PhotoStatus.valueOf(value)

    @TypeConverter
    fun fromFilmFormat(value: FilmFormat): String = value.name

    @TypeConverter
    fun toFilmFormat(value: String): FilmFormat = FilmFormat.valueOf(value)

    @TypeConverter
    fun fromStopIncrement(value: StopIncrement): String = value.name

    @TypeConverter
    fun toStopIncrement(value: String): StopIncrement = StopIncrement.valueOf(value)

    @TypeConverter
    fun fromLightMeterType(value: LightMeterType): String = value.name

    @TypeConverter
    fun toLightMeterType(value: String): LightMeterType = LightMeterType.valueOf(value)
}
